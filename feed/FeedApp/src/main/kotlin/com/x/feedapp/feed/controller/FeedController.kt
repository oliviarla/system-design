package com.x.feedapp.feed.controller

import com.x.feedapp.feed.controller.dto.CreateFeedRequest
import com.x.feedapp.feed.controller.dto.FeedDto
import com.x.feedapp.feed.controller.dto.UpdateFeedRequest
import com.x.feedapp.feed.domain.Feed
import com.x.feedapp.feed.service.FeedService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/feeds")
class FeedController(private val feedService: FeedService) {

    @PostMapping
    fun createFeed(@RequestBody createFeedRequest: CreateFeedRequest): Mono<ResponseEntity<Unit>> {
        return ReactiveSecurityContextHolder.getContext()
            .map { context -> context.authentication.principal as String }
            .flatMap { username ->
                feedService.createFeed(createFeedRequest, username)
                    .thenReturn(ResponseEntity.ok().build())
            }
//            .onErrorResume { e ->
//                Mono.just(ResponseEntity.status(500).build())
//            }
    }

    @PostMapping("/update/{feedId}")
    fun updateFeed(@PathVariable feedId: String, @RequestBody updateFeedRequest: UpdateFeedRequest): Mono<ResponseEntity<Unit>> {
        return ReactiveSecurityContextHolder.getContext()
            .map { context -> context.authentication.principal as String }
            .flatMap { username ->
                feedService.updateFeed(feedId, updateFeedRequest, username)
                    .thenReturn(ResponseEntity.ok().build())
            }

    }

    @DeleteMapping("/{feedId}")
    fun deleteFeed(@PathVariable feedId: String): Mono<ResponseEntity<Unit>> {
        return ReactiveSecurityContextHolder.getContext()
            .map { context -> context.authentication.principal as String }
            .flatMap { username ->
                feedService.deleteFeed(feedId, username)
                    .thenReturn(ResponseEntity.ok().build())
            }
    }

    @GetMapping("/{feedId}")
    fun getFeed(@PathVariable feedId: String): Mono<ResponseEntity<FeedDto>> {
        return feedService.getFeed(feedId)
            .map { feed -> ResponseEntity.ok(fromFeed(feed)) }
            .switchIfEmpty(
                Mono.just(ResponseEntity.notFound().build())
            )

    }

    private fun fromFeed(feed: Feed) = FeedDto (
        id = feed.id,
        content = feed.content,
        authorId = feed.username,
        createdAt = feed.createdAt.toString(),
    )
}
