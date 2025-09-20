package com.x.feedConsumer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FeedConsumerApplication

fun main(args: Array<String>) {
	runApplication<FeedConsumerApplication>(*args)
}
