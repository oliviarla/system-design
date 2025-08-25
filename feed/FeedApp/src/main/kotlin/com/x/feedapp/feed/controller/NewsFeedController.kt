package com.x.feedapp.feed.controller

import com.x.feedapp.feed.controller.dto.FeedResponse
import com.x.feedapp.feed.service.NewsFeedService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import reactor.core.publisher.Mono

class NewsFeedController(private val newsFeedService: NewsFeedService) {

    @QueryMapping
    fun getNewsFeed(@Argument userId: Long, @Argument lastFeedId: Int, @Argument size: Int): Mono<List<FeedResponse>> {
        return Mono.empty()
//        return newsFeedService.getNewsFeed(userId, page).collectList()
    }

}
