package com.x.feedapp.feed.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class FollowService {
    fun getFollowings(username: String): Flux<String> {
        TODO("Not yet implemented")
    }
}