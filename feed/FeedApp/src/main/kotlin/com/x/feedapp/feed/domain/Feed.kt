package com.x.feedapp.feed.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Transient
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.domain.Persistable
import java.time.Instant

@Table("feeds_by_id")
data class Feed(
    @Id
    @field:Column("feed_id")
    val feedId: String,
    val username: String,
    val content: String,
    @field:Column("created_at")
    @field:CreatedDate
    val createdAt: Instant? = null,
    @field:Column("updated_at")
    @field:LastModifiedDate
    val updatedAt: Instant? = null,
) : Persistable<String> {
    @Transient
    private var isNewFeed: Boolean = false

    override fun getId(): String = feedId

    override fun isNew(): Boolean = isNewFeed

    fun markAsNew() {
        isNewFeed = true
    }
}
