package com.x.batchapp.repository

import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface FollowerRepository : ReactiveCrudRepository<Any, String> {

    @Query("SELECT COUNT(*) FROM follower_by_user WHERE username = ?0")
    fun countByUsername(username: String): Mono<Long>
}