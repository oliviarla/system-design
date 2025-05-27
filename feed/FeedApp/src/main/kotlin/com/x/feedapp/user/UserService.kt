package com.x.feedapp.user

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class UserService {
    fun getFollowingIds(userId: Long): Flux<Long> {
        // TODO: Mock implementation, replace with actual logic
        return Flux.just(123, 234, 345);
    }
}
