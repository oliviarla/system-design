package com.x.feedapp.feed.repository

import com.x.feedapp.feed.domain.FeedByUser
import com.x.feedapp.feed.domain.FeedKey
import org.springframework.data.cassandra.core.query.CassandraPageRequest
import org.springframework.data.cassandra.repository.Query
import org.springframework.data.domain.Slice
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface FeedByUserRepository: ReactiveCrudRepository<FeedByUser, FeedKey> {
    fun findAllByKeyUsername(username: String, pageRequest: CassandraPageRequest): Mono<Slice<FeedByUser>>

    @Query("SELECT * FROM feeds_by_user WHERE username = ?0 AND feed_id < ?1 ORDER BY feed_id DESC LIMIT ?2")
    fun findByKeyUsernameAndKeyFeedIdLessThan(
        username: String,
        lastFeedId: String,
        limit: Int
    ): Flux<FeedByUser>

    fun findByKeyUsernameAndKeyFeedIdBetween(
        username: String,
        fromId: String,
        toId: String
    ): Flux<FeedByUser>
}