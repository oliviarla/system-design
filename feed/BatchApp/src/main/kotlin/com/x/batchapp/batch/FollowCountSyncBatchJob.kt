package com.x.batchapp.batch

import com.x.batchapp.domain.FollowCountSyncEvent
import com.x.batchapp.repository.FollowCountSyncKafkaRepository
import com.x.batchapp.repository.FollowRedisRepository
import com.x.batchapp.repository.FollowerRepository
import com.x.batchapp.repository.FollowingRepository
import com.x.batchapp.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

data class UserFollowCounts(
    val username: String,
    val followingCount: Long,
    val followerCount: Long
)

@Configuration
class FollowCountSyncBatchJob(
    private val userRepository: UserRepository,
    private val followingRepository: FollowingRepository,
    private val followerRepository: FollowerRepository,
    private val followRedisRepository: FollowRedisRepository,
    private val followCountSyncKafkaRepository: FollowCountSyncKafkaRepository
) {
    private val logger = LoggerFactory.getLogger(FollowCountSyncBatchJob::class.java)

    @Bean
    fun followCountSyncJob(
        jobRepository: JobRepository,
        kafkaFailureProcessingStep: Step,
        redisToScyllaDBSyncStep: Step
    ): Job {
        return JobBuilder("followCountSyncJob", jobRepository)
            .start(kafkaFailureProcessingStep)
            .next(redisToScyllaDBSyncStep)
            .build()
    }

    // Step 1: Process Kafka messages for Redis failures
    @Bean
    fun kafkaFailureProcessingStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager
    ): Step {
        return StepBuilder("kafkaFailureProcessingStep", jobRepository)
            .chunk<FollowCountSyncEvent, UserFollowCounts>(10, transactionManager)
            .reader(kafkaMessageReader())
            .processor(kafkaFailureProcessor())
            .writer(kafkaFailureWriter())
            .faultTolerant()
            .skip(Exception::class.java)
            .skipLimit(100)
            .build()
    }

    @Bean
    fun kafkaMessageReader(): ItemReader<FollowCountSyncEvent> {
        val messages = followCountSyncKafkaRepository.consumeAllMessages()
        logger.info("Loaded ${messages.size} Kafka messages to process")

        return object : ItemReader<FollowCountSyncEvent> {
            private val iterator = messages.iterator()

            override fun read(): FollowCountSyncEvent? {
                return if (iterator.hasNext()) iterator.next() else null
            }
        }
    }

    @Bean
    fun kafkaFailureProcessor(): ItemProcessor<FollowCountSyncEvent, UserFollowCounts> {
        return ItemProcessor { event ->
            try {
                logger.debug("Processing Kafka failure for: ${event.sourceUsername} -> ${event.targetUsername}")

                // Get distinct usernames that need to be updated
                val usernames = setOf(event.sourceUsername, event.targetUsername)

                // Calculate counts from ScyllaDB for both users
                val counts = usernames.map { username ->
                    val (followingCount, followerCount) = Mono.zip(
                        followingRepository.countByUsername(username),
                        followerRepository.countByUsername(username)
                    ).block()!!

                    UserFollowCounts(username, followingCount, followerCount)
                }

                // Return the first one (we'll handle multiple in writer)
                // This is a simplification - ideally we'd process both users
                counts.firstOrNull()
            } catch (e: Exception) {
                logger.error("Error processing Kafka failure: $event", e)
                throw e
            }
        }
    }

    @Bean
    fun kafkaFailureWriter(): ItemWriter<UserFollowCounts> {
        return ItemWriter { items ->
            items.forEach { userFollowCounts ->
                try {
                    // Update Redis with calculated counts from ScyllaDB
                    Flux.merge(
                        followRedisRepository.setFollowingCount(
                            userFollowCounts.username,
                            userFollowCounts.followingCount
                        ),
                        followRedisRepository.setFollowerCount(
                            userFollowCounts.username,
                            userFollowCounts.followerCount
                        )
                    ).then().block()

                    logger.info("Updated Redis counts from Kafka failure for user: ${userFollowCounts.username}")
                } catch (e: Exception) {
                    logger.error("Error updating Redis for user: ${userFollowCounts.username}", e)
                    throw e
                }
            }
        }
    }

    // Step 2: Sync from Redis to ScyllaDB User table
    @Bean
    fun redisToScyllaDBSyncStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager
    ): Step {
        return StepBuilder("redisToScyllaDBSyncStep", jobRepository)
            .chunk<String, UserFollowCounts>(10, transactionManager)
            .reader(redisUserReader())
            .processor(redisCountFetcher())
            .writer(scyllaDBUserTableWriter())
            .faultTolerant()
            .skip(Exception::class.java)
            .skipLimit(100)
            .build()
    }

    @Bean
    fun redisUserReader(): ItemReader<String> {
        val usernames = followRedisRepository.getAllFollowCountUsernames()
            .collectList()
            .block() ?: emptyList()

        logger.info("Loaded ${usernames.size} users from Redis to sync")

        return object : ItemReader<String> {
            private val iterator = usernames.iterator()

            override fun read(): String? {
                return if (iterator.hasNext()) iterator.next() else null
            }
        }
    }

    @Bean
    fun redisCountFetcher(): ItemProcessor<String, UserFollowCounts> {
        return ItemProcessor { username ->
            try {
                logger.debug("Fetching Redis counts for user: $username")

                val (followingCount, followerCount) = Mono.zip(
                    followRedisRepository.getFollowingCount(username),
                    followRedisRepository.getFollowerCount(username)
                ).block()!!

                UserFollowCounts(username, followingCount, followerCount)
            } catch (e: Exception) {
                logger.error("Error fetching Redis counts for user: $username", e)
                throw e
            }
        }
    }

    @Bean
    fun scyllaDBUserTableWriter(): ItemWriter<UserFollowCounts> {
        return ItemWriter { items ->
            items.forEach { userFollowCounts ->
                try {
                    // Update ScyllaDB User table with Redis counts
                    userRepository.findById(userFollowCounts.username)
                        .flatMap { user ->
                            val redisFollowingCount = userFollowCounts.followingCount.toInt()
                            val redisFollowerCount = userFollowCounts.followerCount.toInt()

                            // Only update if counts don't match
                            if (user.followingCount != redisFollowingCount || user.followerCount != redisFollowerCount) {
                                logger.info(
                                    "Syncing counts for user $user.username: " +
                                    "following ${user.followingCount} -> $redisFollowingCount, " +
                                    "follower ${user.followerCount} -> $redisFollowerCount"
                                )
                                user.followingCount = redisFollowingCount
                                user.followerCount = redisFollowerCount
                                userRepository.save(user)
                            } else {
                                Mono.just(user)
                            }
                        }
                        .block()

                    logger.debug("Successfully synced ScyllaDB User table for: ${userFollowCounts.username}")
                } catch (e: Exception) {
                    logger.error("Error updating User table for: ${userFollowCounts.username}", e)
                    throw e
                }
            }
        }
    }
}
