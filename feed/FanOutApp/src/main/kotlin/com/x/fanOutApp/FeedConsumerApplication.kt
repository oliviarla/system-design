package com.x.fanOutApp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableKafka
class FeedConsumerApplication

fun main(args: Array<String>) {
	runApplication<FeedConsumerApplication>(*args)
}
