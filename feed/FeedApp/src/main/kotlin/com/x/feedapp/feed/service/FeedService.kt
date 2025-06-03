package com.x.feedapp.feed.service

import com.x.feedapp.feed.controller.dto.CreateFeedRequest
import com.x.feedapp.feed.controller.dto.UpdateFeedRequest
import com.x.feedapp.feed.domain.Feed
import com.x.feedapp.feed.repository.FeedDBRepository
import com.x.feedapp.feed.repository.FeedRedisRepository
import com.x.feedapp.user.service.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.lang.RuntimeException

@Service
class FeedService(private val feedDBRepository: FeedDBRepository,
                  private val feedRedisRepository: FeedRedisRepository,
                  private val userService: UserService
) {

    private val logger : Logger? = LoggerFactory.getLogger(FeedService::class.java)

    fun createFeed(createFeedRequest: CreateFeedRequest, userId: Long): Mono<Feed> {
        val feed = Feed(
            content = createFeedRequest.content,
            userId = userId,
        )
        return feedDBRepository.save(feed)
            .doOnNext { feed -> logger?.info(feed.toString()) }
        // TODO: 카프카 토픽으로 feedId, userId 전송
    }

    fun updateFeed(id: Long, updateFeedRequest: UpdateFeedRequest, userId: Long): Mono<Feed> {
        return feedDBRepository.existsById(id)
            .flatMap { exists ->
                if (exists) {
                    val feed = Feed(
                        id = id,
                        userId = userId,
                        content = updateFeedRequest.content,
                    )
                    feedDBRepository.save(feed)
                } else {
                    Mono.error(RuntimeException("Feed not found"))
                }
            }
    }
    fun deleteFeed(id: Long) = feedDBRepository.deleteById(id)
}
