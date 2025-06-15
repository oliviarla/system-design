package com.x.feedapp.feed.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import java.time.Instant

@PrimaryKeyClass
data class FeedKey(
    @field:PrimaryKeyColumn(name = "feed_id", type = PrimaryKeyType.PARTITIONED)
    val feedId: String,
    @field:PrimaryKeyColumn(name = "username", ordinal = 0, type = PrimaryKeyType.CLUSTERED)
    val username: String,
    @field:PrimaryKeyColumn(name = "created_at", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    @field:CreatedDate
    val createdAt: Instant? = null,
)