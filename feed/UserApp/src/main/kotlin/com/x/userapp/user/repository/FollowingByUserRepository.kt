package com.x.userapp.user.repository

import com.x.userapp.user.domain.FollowingByUser
import com.x.userapp.user.domain.FollowingKey
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface FollowingByUserRepository: ReactiveCrudRepository<FollowingByUser, FollowingKey> {
    fun findAllByKeyUsername(username: String): Flux<FollowingByUser>
}