package com.x.feedapp.user.repository

import com.x.feedapp.user.domain.FollowerByUser
import com.x.feedapp.user.domain.FollowerKey
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface FollowerByUserRepository : ReactiveCrudRepository<FollowerByUser, FollowerKey> {
    fun findAllByKeyUsername(username: String): Flux<FollowerByUser>
}