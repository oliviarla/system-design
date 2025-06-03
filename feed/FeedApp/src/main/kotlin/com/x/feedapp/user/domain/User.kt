package com.x.feedapp.user.domain

import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("user")
class User(
    @PrimaryKey val id: Long? = null,
    val username: String,
    val password: String,
    val createdAt: LocalDateTime) {
}
