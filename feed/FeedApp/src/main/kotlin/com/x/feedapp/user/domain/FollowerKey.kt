package com.x.feedapp.user.domain

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn

@PrimaryKeyClass
data class FollowerKey(
    @field:PrimaryKeyColumn(name = "username", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    val username: String,
    @field:PrimaryKeyColumn(name = "follower_username", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    val followerUsername: String
)