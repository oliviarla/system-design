package com.x.userapp.user.repository

import com.x.userapp.user.domain.FollowerByUser
import com.x.userapp.user.domain.FollowerKey
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface FollowerByUserRepository : ReactiveCrudRepository<FollowerByUser, FollowerKey> {
    fun findAllByKeyUsername(username: String): Flux<FollowerByUser>
    fun countByKeyUsername(username: String): Mono<Long>
}