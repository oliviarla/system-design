package com.x.feedapp.feed.domain

import org.springframework.data.annotation.Transient
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.domain.Persistable

@Table("feeds_by_user")
data class FeedByUser(
    @PrimaryKey
    val key: FeedKey
): Persistable<FeedKey> {
    @Transient
    private var isNewFeed: Boolean = false

    override fun getId(): FeedKey {
        return key
    }

    override fun isNew(): Boolean {
        return isNewFeed
    }

    fun markAsNew() {
        isNewFeed = true
    }
}

fun toFeedByUser(feed: Feed): FeedByUser {
    return FeedByUser(
        key = FeedKey(
            feedId = feed.feedId,
            username = feed.username,
        )
    )
}