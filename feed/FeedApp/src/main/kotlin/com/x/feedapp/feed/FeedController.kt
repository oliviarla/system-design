package com.x.feedapp.feed

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/feeds")
class FeedController(private val feedService: FeedService) {

    // TODO: use Spring Security AuthenticationPrincipal
    fun createFeed(createFeedRequest: CreateFeedRequest, userId: Long): Mono<ResponseEntity<Unit>> {
        return feedService.createFeed(createFeedRequest, userId)
            .thenReturn(ResponseEntity.ok().build())
    }

    fun updateFeed(feedId: Long, updateFeedRequest: UpdateFeedRequest, userId: Long): Mono<Feed> {
        return feedService.updateFeed(feedId, updateFeedRequest, userId)
    }

    // TODO: delete the content's owner
    fun deleteFeed(feedId: Long) = feedService.deleteFeed(feedId)


    fun getNewsFeed(userId: Long ): Flux<Feed> {
        return feedService.getNewsFeed(userId)
    }
}
