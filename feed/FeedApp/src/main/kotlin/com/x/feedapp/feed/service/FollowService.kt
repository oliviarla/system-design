package com.x.feedapp.feed.service

import com.x.feedapp.config.UserAppWebClientFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class FollowService(
    private val userAppWebClientFactory: UserAppWebClientFactory
) {
    fun getFollowings(username: String): Mono<List<String>> {
        return userAppWebClientFactory.getClient()
            .get()
            .uri("/api/v1/users/followings/{username}", username)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<String>>() {})
    }
}