package com.x.userapp.user.repository

import com.x.userapp.user.domain.User
import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface UserRepository: ReactiveCrudRepository<User, Long> {
    fun existsByUsername(username: String): Mono<Boolean>

    fun findByUsername(username: String): Mono<User>

    fun findFollowingsByUsername(username: String): Flux<User>

    fun findFollowersByUsername(username: String): Flux<User>

    @Query("UPDATE user SET following_count = following_count + 1 WHERE username = ?0")
    fun incrementFollowingCount(username: String): Mono<Boolean>

    @Query("UPDATE user SET following_count = following_count - 1 WHERE username = ?0")
    fun decrementFollowingCount(username: String): Mono<Boolean>

    @Query("UPDATE user SET follower_count = follower_count + 1 WHERE username = ?0")
    fun incrementFollowerCount(username: String): Mono<Boolean>

    @Query("UPDATE user SET follower_count = follower_count - 1 WHERE username = ?0")
    fun decrementFollowerCount(username: String): Mono<Boolean>
}
