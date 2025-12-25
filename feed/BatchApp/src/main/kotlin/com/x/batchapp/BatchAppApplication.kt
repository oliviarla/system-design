package com.x.batchapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BatchAppApplication

fun main(args: Array<String>) {
    runApplication<BatchAppApplication>(*args)
}