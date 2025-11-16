package com.x.feedapp.feed.controller.dto

data class PagedFeedResponse(
    val feeds: List<FeedResponse>,
    val size: Int,
    val lastFeedId: String,
    val hasNext: Boolean
)
