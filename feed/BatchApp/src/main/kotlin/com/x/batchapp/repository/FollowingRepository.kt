package com.x.batchapp.repository

import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface FollowingRepository : ReactiveCrudRepository<Any, String> {

    @Query("SELECT COUNT(*) FROM following_by_user WHERE username = ?0")
    fun countByUsername(username: String): Mono<Long>
}