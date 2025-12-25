package com.x.batchapp.domain

import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table

@Table("user")
data class User(
    @field:PrimaryKey
    val username: String,
    @field:Column("following_count")
    var followingCount: Int = 0,
    @field:Column("follower_count")
    var followerCount: Int = 0
)