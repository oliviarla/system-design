package com.x.batchapp.repository

import com.x.batchapp.domain.FollowCountSyncEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class FollowCountSyncKafkaRepository(
    private val consumerFactory: ConsumerFactory<String, FollowCountSyncEvent>,
    @Value("\${kafka.topic.follow-count-sync:follow-count-sync}") private val topic: String
) {
    fun consumeAllMessages(): List<FollowCountSyncEvent> {
        val consumer = consumerFactory.createConsumer("batch-follow-count-sync", "batch")
        consumer.subscribe(listOf(topic))

        val messages = mutableListOf<FollowCountSyncEvent>()
        try {
            var emptyPollCount = 0
            while (emptyPollCount < 3) {
                val records = consumer.poll(Duration.ofSeconds(1))
                if (records.isEmpty) {
                    emptyPollCount++
                } else {
                    emptyPollCount = 0
                    records.forEach { record ->
                        messages.add(record.value())
                    }
                }
            }
            consumer.commitSync()
        } finally {
            consumer.close()
        }

        return messages
    }
}
