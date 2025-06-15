package com.x.feedapp.feed.controller

import com.x.feedapp.feed.controller.dto.CreateFeedRequest
import com.x.feedapp.feed.controller.dto.FeedResponse
import com.x.feedapp.feed.controller.dto.PagedFeedResponse
import com.x.feedapp.feed.controller.dto.UpdateFeedRequest
import com.x.feedapp.feed.controller.dto.toFeedResponse
import com.x.feedapp.feed.domain.Feed
import com.x.feedapp.feed.service.FeedService
import org.springframework.data.cassandra.core.query.CassandraPageRequest
import org.springframework.data.domain.PageRequest
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
import java.nio.ByteBuffer
import java.util.Base64

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
        @RequestParam(required = false) pagingState: String?
    ): Mono<PagedFeedResponse> {
        val pageRequest = createPageRequest(size, pagingState)
        return feedService.findMyFeeds(pageRequest)
            .map { slice ->
                createPageResponse(slice)
            }
    }

    @GetMapping("/{username}")
    fun getUserFeeds(
        @PathVariable username: String,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) pagingState: String?
    ): Mono<PagedFeedResponse> {
        val pageRequest = createPageRequest(size, pagingState)
        return feedService.getFeedsByUser(username, pageRequest)
            .map { slice ->
                createPageResponse(slice)
            }
    }

    private fun createPageResponse(slice: Slice<Feed>): PagedFeedResponse =
        if (slice.nextPageable().isUnpaged) {
            PagedFeedResponse(
                feeds = slice.content.map { toFeedResponse(it) },
                size = slice.size,
                pagingState = null
            )
        } else {
            val nextPagingState = (slice.nextPageable() as CassandraPageRequest)
                .pagingState?.let { buf ->
                    val bytes = ByteArray(buf.remaining())
                    buf.get(bytes)
                    Base64.getUrlEncoder().encodeToString(bytes)
                }
            PagedFeedResponse(
                feeds = slice.content.map { toFeedResponse(it) },
                size = slice.size,
                pagingState = nextPagingState
            )
        }

    private fun createPageRequest(
        size: Int,
        pagingState: String?
    ): CassandraPageRequest {
        val pageRequest = pagingState?.let {
            CassandraPageRequest.of(
                PageRequest.ofSize(size),
                ByteBuffer.wrap(Base64.getUrlDecoder().decode(pagingState))
            )
        }
            ?: CassandraPageRequest.first(size)
        return pageRequest
    }
}
