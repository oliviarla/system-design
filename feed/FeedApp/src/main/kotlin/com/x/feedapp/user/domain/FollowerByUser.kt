package com.x.feedapp.user.domain

import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant

@Table("follower_by_user")
data class FollowerByUser(
    @PrimaryKey
    val key: FollowerKey,
    @field:Column("followed_at")
    @field:LastModifiedDate
    var followedAt: Instant? = null
)