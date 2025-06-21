package com.x.feedapp.feed.repository

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class KafkaProducer(private val kafkaTemplate: KafkaTemplate<String, String>) {
    private val logger: Logger = LoggerFactory.getLogger(KafkaProducer::class.java)
    val topicName: String = "feed-creation"

    fun sendFeedCreationMessage(message: String): CompletableFuture<SendResult<String, String>> {
        return kafkaTemplate.send(topicName, message)
            .whenComplete {
                result, ex ->
                if (ex != null) {
                    logger.error("Failed to send message: ${ex.message}")
                } else {
                    logger.info("Message sent successfully: $result")
                }
            }
    }
}
