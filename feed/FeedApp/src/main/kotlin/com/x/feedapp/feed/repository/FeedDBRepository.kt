package com.x.feedapp.feed.repository

import com.x.feedapp.feed.domain.Feed
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface FeedDBRepository : ReactiveCrudRepository<Feed, String> {
    fun findByFeedId(id: String): Mono<Feed>
    fun existsByFeedId(id: String): Mono<Boolean>
    fun deleteByFeedId(id: String): Mono<Void>
}
