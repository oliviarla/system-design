package com.x.feedapp.feed.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table
data class Feed(
    @Id val id: Long? = null,
    val content: String,
    val userId: Long,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)
