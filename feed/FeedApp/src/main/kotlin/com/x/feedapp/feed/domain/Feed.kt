package com.x.feedapp.feed.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Transient
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.domain.Persistable
import java.time.Instant

@Table
data class Feed(
    @Id
    @field:Column("id")
    val feedId: String,
    val content: String,
    val username: String,
    @field:Column("created_at")
    @field:CreatedDate
    val createdAt: Instant? = null,
    @field:Column("updated_at")
    @field:LastModifiedDate
    val updatedAt: Instant? = null,
    @field:Transient
    var isNewFeed: Boolean = false
) : Persistable<String> {

    override fun getId(): String = feedId

    override fun isNew(): Boolean = isNewFeed
}
