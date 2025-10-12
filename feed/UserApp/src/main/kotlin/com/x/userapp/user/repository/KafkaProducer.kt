package com.x.userapp.user.repository

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Service
class KafkaProducer(private val kafkaTemplate: KafkaTemplate<String, String>) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaProducer::class.java)
    }

    fun sendMessage(topic: String, message: String): Mono<Void> {
        return kafkaTemplate.send(topic, message)
            .whenComplete { result, ex ->
                when {
                    ex != null -> logger.error("Failed to send message to $topic: ${ex.message}")
                    else -> logger.info("Message sent successfully to $topic: $result")
                }
            }
            .toMono()
            .then()
    }
}