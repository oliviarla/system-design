package com.x.feedapp.user.repository

import com.x.feedapp.user.domain.FollowingByUser
import com.x.feedapp.user.domain.FollowingKey
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface FollowingByUserRepository: ReactiveCrudRepository<FollowingByUser, FollowingKey> {
    fun findAllByKeyUsername(username: String): Flux<FollowingByUser>
}