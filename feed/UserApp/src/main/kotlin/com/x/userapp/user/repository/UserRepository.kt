package com.x.userapp.user.repository

import com.x.userapp.user.domain.User
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface UserRepository: ReactiveCrudRepository<User, Long> {
    fun existsByUsername(username: String): Mono<Boolean>

    fun findByUsername(username: String): Mono<User>

    fun findFollowingsByUsername(username: String): Flux<User>

    fun findFollowersByUsername(username: String): Flux<User>
}
