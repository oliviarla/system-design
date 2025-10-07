package com.x.userapp.user.domain

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn

@PrimaryKeyClass
data class FollowingKey(
    @field:PrimaryKeyColumn(name = "username", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    val username: String,
    @field:PrimaryKeyColumn(name = "following_username", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    val followingUsername: String
)