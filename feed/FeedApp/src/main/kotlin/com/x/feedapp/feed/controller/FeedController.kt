package com.x.feedapp.feed.controller

import com.x.feedapp.feed.controller.dto.CreateFeedRequest
import com.x.feedapp.feed.controller.dto.FeedResponse
import com.x.feedapp.feed.controller.dto.PagedFeedResponse
import com.x.feedapp.feed.controller.dto.UpdateFeedRequest
import com.x.feedapp.feed.controller.dto.toFeedResponse
import com.x.feedapp.feed.domain.Feed
import com.x.feedapp.feed.service.FeedService
import com.x.feedapp.feed.service.NewsFeedService
import org.springframework.data.domain.Slice
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/feeds")
class FeedController(
    private val feedService: FeedService,
    private val newsFeedService: NewsFeedService
) {

    @PostMapping
    fun createFeed(@RequestBody createFeedRequest: CreateFeedRequest): Mono<ResponseEntity<Unit>> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val username = context.authentication.principal as String
                feedService.createFeed(createFeedRequest, username)
                    .thenReturn(ResponseEntity.ok().build<Unit>())
            }
            .switchIfEmpty(Mono.just(ResponseEntity.status(401).build()))
    }

    @PostMapping("/update/{feedId}")
    fun updateFeed(
        @PathVariable feedId: String,
        @RequestBody updateFeedRequest: UpdateFeedRequest
    ): Mono<ResponseEntity<Unit>> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val username = context.authentication.principal as String
                feedService.updateFeed(feedId, updateFeedRequest, username)
                    .thenReturn(ResponseEntity.ok().build<Unit>())
            }
            .switchIfEmpty(Mono.just(ResponseEntity.status(401).build()))
    }

    @DeleteMapping("/{feedId}")
    fun deleteFeed(@PathVariable feedId: String): Mono<ResponseEntity<Unit>> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val username = context.authentication.principal as String
                feedService.deleteFeed(feedId, username)
                    .thenReturn(ResponseEntity.ok().build<Unit>())
            }
            .switchIfEmpty(Mono.just(ResponseEntity.status(401).build()))
    }

    @GetMapping
    fun getFeed(@RequestParam(name = "id") feedId: String): Mono<ResponseEntity<FeedResponse>> {
        return feedService.getFeed(feedId)
            .map { feed -> ResponseEntity.ok(toFeedResponse(feed)) }
            .switchIfEmpty(
                Mono.just(ResponseEntity.notFound().build())
            )

    }

    @GetMapping("/my")
    fun myFeeds(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) lastFeedId: String?
    ): Mono<ResponseEntity<PagedFeedResponse>> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val username = context.authentication.principal as String
                feedService.getFeedsByUser(username, size, lastFeedId)
                    .map { slice -> ResponseEntity.ok(createPageResponse(slice)) }
            }
            .switchIfEmpty(Mono.just(ResponseEntity.status(401).build()))
    }

    @GetMapping("/my/news")
    fun getNewsFeed(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) lastFeedId: String?
    ): Mono<ResponseEntity<PagedFeedResponse>> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val username = context.authentication.principal as String
                newsFeedService.getNewsFeed(username, size, lastFeedId)
                    .map { slice -> ResponseEntity.ok(createPageResponse(slice)) }
            }
            .switchIfEmpty(Mono.just(ResponseEntity.status(401).build()))
    }

    @GetMapping("/{username}")
    fun getUserFeeds(
        @PathVariable username: String,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) lastFeedId: String?
    ): Mono<PagedFeedResponse> {
        return feedService.getFeedsByUser(username, size, lastFeedId)
            .map { slice ->
                createPageResponse(slice)
            }
    }

    private fun createPageResponse(slice: Slice<Feed>): PagedFeedResponse {
        val feeds = slice.content.map { toFeedResponse(it) }
        val lastFeedId = slice.content.lastOrNull()?.feedId ?: ""
        val hasNext = slice.hasNext()

        return PagedFeedResponse(
            feeds = feeds,
            size = feeds.size,
            lastFeedId = lastFeedId,
            hasNext = hasNext
        )
    }
}
