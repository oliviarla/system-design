package com.x.feedapp.user.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant

@Table("user")
class User(
    @field:PrimaryKey val username: String,
    val password: String,
    @field:Column("created_at") @field:CreatedDate var createdAt: Instant? = null) {
}
