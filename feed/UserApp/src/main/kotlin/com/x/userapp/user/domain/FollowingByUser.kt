package com.x.userapp.user.domain

import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant

@Table("following_by_user")
data class FollowingByUser (
    @PrimaryKey
    val key: FollowingKey,
    @field:Column("followed_at")
    @field:LastModifiedDate
    var followedAt: Instant? = null
)