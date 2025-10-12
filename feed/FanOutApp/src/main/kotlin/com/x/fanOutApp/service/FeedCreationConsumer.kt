package com.x.fanOutApp.service

import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.Retry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class FeedCreationConsumer(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>,
    private val webClient: WebClient,
    private val retry: Retry,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @field:Value("\${spring.services.user-service-url}") private val userServiceUrl: String
) {
    private val logger: Logger = LoggerFactory.getLogger(FeedCreationConsumer::class.java)
    private val newsFeedCacheKey: (String) -> String = { username -> "news_feed:$username" }

    @KafkaListener(topics = ["feed-creation"], groupId = "feed-consumer-group")
    fun consumeFeedCreationMessage(message: String) {
        logger.info("Received feed creation message: $message")
        val parsed = parseFeedCreationMessage(message)
        val username = parsed.first
        val feedId = parsed.second
        try {
            fanOutNewsFeedCache(username, feedId)
        } catch (e: Exception) {
            logger.error("Error processing feed creation message: ${e.message}", e)
        }
    }

    private fun parseFeedCreationMessage(message: String): Pair<String, String> {
        // Parse message format: "username FeedKey(feedId=123, username=user, createdAt=...)"
        val parts = message.split(" ", limit = 2)
        val pair: Pair<String, String> = Pair("", "")
        if (parts.size != 2) {
            logger.error("Invalid message format: $message. Expected 'username FeedKey(...)'")
            return pair
        }

        val username = parts[0]
        val feedKeyPart = parts[1]

        // Extract feedId from FeedKey string using regex
        val feedIdRegex = Regex("feedId=(\\d+)")
        val matchResult = feedIdRegex.find(feedKeyPart)

        if (matchResult == null) {
            logger.error("Could not extract feedId from message: $message")
            return pair
        }

        val feedId = matchResult.groupValues[1]
        val score = feedId.toDoubleOrNull()
        if (score == null) {
            logger.error("Invalid feedId format: $feedId. Expected numeric value")
            return pair
        }
        return Pair(username, feedId)
    }

    private fun fanOutNewsFeedCache(username: String, feedId: String): Mono<Boolean> {
        return getFollowerCount(username)
            .flatMap { followerCount ->
                if (followerCount > MAX_FOLLOWERS_FOR_FANOUT) {
                    Mono.just(true)
                } else {
                    // Process followers in batches to avoid overwhelming Cassandra
                    getFanOutFollowers(username)
                        .buffer(BATCH_SIZE)
                        .delayElements(Duration.ofMillis(10)) // Add small delay between batches
                        .flatMap { followersChunk ->
                            addFeedToNewsFeed(followersChunk, username, feedId)
                        }
                        .reduce(true) { acc, _ -> acc }
                }
            }
    }

    private fun getFollowerCount(username: String): Mono<Long> {
        return webClient.get()
            .uri("$userServiceUrl/api/follower/count/$username")
            .retrieve()
            .bodyToMono(Long::class.java)
            .doOnError { error ->
                logger.error("Failed to get follower count for $username from user service", error)
            }
            .onErrorReturn(0L)
    }

    fun getFanOutFollowers(username: String): Flux<String> {
        return webClient.get()
            .uri("$userServiceUrl/api/follower/$username")
            .retrieve()
            .bodyToFlux(String::class.java)
            .doOnError { error ->
                logger.error("Failed to get followers for $username from user service", error)
            }
            .onErrorResume {
                Flux.empty()
            }
    }

    private fun addFeedToNewsFeed(followers: List<String>, username: String, feedId: String): Mono<Boolean> {
        val script = RedisScript<Long>.of(
            """
                if redis.call("EXISTS", KEYS[1]) == 1 then
                    return redis.call("ZADD", KEYS[1], ARGV[1], ARGV[2])
                else
                return 0
                end
            """.trimIndent(),
            Long::class.java
        )
        return Flux.fromIterable(followers)
            .flatMap { follower ->
                reactiveRedisTemplate.execute(
                    script,
                    listOf(newsFeedCacheKey(follower)),
                    feedId + username, feedId.toDouble()
                )
                    .next()
                    .map { l -> l > 0 }
                    .transformDeferred(RetryOperator.of(retry))
                    .onErrorResume { error ->
                        logger.error("Failed to add feed to news feed after retries for follower: $follower", error)
                        sendToDeadLetterQueue(username, feedId, follower, error)
                        Mono.just(false)
                    }
            }
            .collectList()
            .map { results -> results.all { it } }
    }

    private fun sendToDeadLetterQueue(username: String, feedId: String, follower: String, error: Throwable): Mono<Void> {
        val dlqMessage = "username=$username,feedId=$feedId,follower=$follower,error=${error.message}"
        return Mono.fromFuture(
            kafkaTemplate.send("feed-creation-dlq", dlqMessage)
        )
            .doOnSuccess {
                logger.info("Sent failed message to DLQ: $dlqMessage")
            }
            .doOnError { dlqError ->
                logger.error("Failed to send message to DLQ: $dlqMessage", dlqError)
            }
            .then()
            .onErrorResume { Mono.empty() }
    }

    companion object {
        private const val MAX_FOLLOWERS_FOR_FANOUT = 10000 // Threshold for celebrity users
        private const val BATCH_SIZE = 100 // Process 100 followers at a time
    }
}