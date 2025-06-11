package com.x.feedapp.feed.controller

import com.x.feedapp.feed.controller.dto.FeedDto
import com.x.feedapp.feed.service.NewsFeedService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import reactor.core.publisher.Mono

class NewsFeedController(private val newsFeedService: NewsFeedService) {

    @QueryMapping
    fun getNewsFeed(@Argument userId: Long, @Argument page: Int): Mono<List<FeedDto>> {
        return Mono.empty()
//        return newsFeedService.getNewsFeed(userId, page).collectList()
    }

}
