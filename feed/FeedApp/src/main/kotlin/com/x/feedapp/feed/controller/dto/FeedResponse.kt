package com.x.feedapp.feed.controller.dto

import com.x.feedapp.feed.domain.Feed

data class FeedResponse(
    val id: String,
    val content: String,
    val authorId: String,
    val createdAt: String,
)

fun toFeedResponse(feed: Feed): FeedResponse {
    return FeedResponse(
        id = feed.feedId,
        content = feed.content,
        authorId = feed.username,
        createdAt = feed.createdAt.toString(),
    )
}