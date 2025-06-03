package com.x.feedapp.feed.controller

import com.x.feedapp.feed.controller.dto.CreateFeedRequest
import com.x.feedapp.feed.controller.dto.NewsFeedDto
import com.x.feedapp.feed.controller.dto.UpdateFeedRequest
import com.x.feedapp.feed.domain.Feed
import com.x.feedapp.feed.service.FeedService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/feeds")
class FeedController(private val feedService: FeedService) {

    // TODO: use Spring Security AuthenticationPrincipal
    fun createFeed(createFeedRequest: CreateFeedRequest, userId: Long): Mono<ResponseEntity<Unit>> {
        return feedService.createFeed(createFeedRequest, userId)
            .thenReturn(ResponseEntity.ok().build())
    }

    fun updateFeed(feedId: Long, updateFeedRequest: UpdateFeedRequest, userId: Long): Mono<NewsFeedDto> {
        return feedService.updateFeed(feedId, updateFeedRequest, userId)
            .map { feed -> fromFeed(feed) }
    }

    // TODO: delete if the content's owner requested
    fun deleteFeed(feedId: Long) = feedService.deleteFeed(feedId)

    private fun fromFeed(feed: Feed) = NewsFeedDto (
        id = feed.id.toString(),
        content = feed.content,
        authorId = feed.userId.toString(),
        createdAt = feed.createdAt.toString(),
    )
}
