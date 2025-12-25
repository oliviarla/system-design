package com.x.batchapp.repository

import com.x.batchapp.domain.User
import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface UserRepository : ReactiveCrudRepository<User, String> {

    @Query("SELECT username FROM user")
    fun findAllUsernames(): Flux<String>
}