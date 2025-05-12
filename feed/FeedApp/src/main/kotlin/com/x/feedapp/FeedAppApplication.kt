package com.x.feedapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FeedAppApplication

fun main(args: Array<String>) {
	runApplication<FeedAppApplication>(*args)
}
