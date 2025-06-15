package com.x.feedapp.feed.repository

import com.x.feedapp.feed.domain.FeedByUser
import com.x.feedapp.feed.domain.FeedKey
import org.springframework.data.cassandra.core.query.CassandraPageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface FeedByUserRepository: ReactiveCrudRepository<FeedByUser, FeedKey> {
    fun findAllByKeyUsername(username: String, pageRequest: CassandraPageRequest): Mono<Slice<FeedByUser>>
}