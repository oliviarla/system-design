package com.x.feedapp.feed.repository

import com.x.feedapp.feed.domain.Feed
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface FeedDBRepository : ReactiveCrudRepository<Feed, String> {
    override fun findById(id: String): Mono<Feed>
    override fun existsById(id: String): Mono<Boolean>
}
