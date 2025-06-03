package com.x.feedapp.user.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserService(userRepository: UserRepository) {
    fun getFollowingIds(userId: Long): Flux<Long> {
        // TODO: Mock implementation, replace with actual logic
        return Flux.just(123, 234, 345);
    }

    fun join(username: String, password: String): Mono<Boolean> {
        if (username.isBlank() || password.isBlank()) {
            return Mono.just(false)
        }

        if ()
    }

    private fun validateUsername(username: String): Boolean {
        userre
    }
}
