package com.x.batchapp.batch

import com.x.batchapp.repository.NewsFeedRedisRepository
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

data class NewsfeedCleanupResult(
    val username: String,
    val removedItems: Long
)

@Configuration
class NewsFeedCleanupBatchJob(
    private val newsFeedRedisRepository: NewsFeedRedisRepository
) {
    private val logger = LoggerFactory.getLogger(NewsFeedCleanupBatchJob::class.java)

    @Bean
    fun newsFeedCleanupJob(
        jobRepository: JobRepository,
        newsFeedCleanupStep: Step
    ): Job {
        return JobBuilder("newsFeedCleanupJob", jobRepository)
            .start(newsFeedCleanupStep)
            .build()
    }

    @Bean
    fun newsFeedCleanupStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager
    ): Step {
        return StepBuilder("newsFeedCleanupStep", jobRepository)
            .chunk<String, NewsfeedCleanupResult>(20, transactionManager)
            .reader(newsfeedKeyReader())
            .processor(newsfeedTrimProcessor())
            .writer(newsfeedCleanupWriter())
            .faultTolerant()
            .skip(Exception::class.java)
            .skipLimit(100)
            .build()
    }

    @Bean
    fun newsfeedKeyReader(): ItemReader<String> {
        val newsfeedKeys = newsFeedRedisRepository.getAllNewsfeedKeys()
            .collectList()
            .block() ?: emptyList()

        logger.info("Loaded ${newsfeedKeys.size} newsfeed keys to process")

        return object : ItemReader<String> {
            private val iterator = newsfeedKeys.iterator()

            override fun read(): String? {
                return if (iterator.hasNext()) iterator.next() else null
            }
        }
    }

    @Bean
    fun newsfeedTrimProcessor(): ItemProcessor<String, NewsfeedCleanupResult> {
        return ItemProcessor { key ->
            try {
                val username = key.removePrefix("newsfeed:")
                logger.debug("Trimming newsfeed for user: $username")

                val removedItems = newsFeedRedisRepository.trimNewsfeed(username).block() ?: 0L

                if (removedItems > 0) {
                    logger.debug("Trimmed $removedItems items from newsfeed for user: $username")
                }

                NewsfeedCleanupResult(username, removedItems)
            } catch (e: Exception) {
                logger.error("Error trimming newsfeed for key: $key", e)
                throw e
            }
        }
    }

    @Bean
    fun newsfeedCleanupWriter(): ItemWriter<NewsfeedCleanupResult> {
        return ItemWriter { items ->
            val trimmedCount = items.count { it.removedItems > 0 }
            val totalRemoved = items.sumOf { it.removedItems }

            logger.info("Chunk completed: Processed ${items.count()} newsfeeds, Trimmed $trimmedCount newsfeeds, Removed $totalRemoved total items")
        }
    }
}
