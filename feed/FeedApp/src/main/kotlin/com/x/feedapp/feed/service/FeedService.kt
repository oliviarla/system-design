package com.x.feedapp.feed.service

import com.datastax.oss.driver.api.core.uuid.Uuids
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

    fun createFeed(createFeedRequest: CreateFeedRequest, username: String): Mono<Feed> {
        val feed = Feed(
            feedId = Uuids.timeBased().toString(),
            content = createFeedRequest.content,
            username = username,
            isNewFeed = true,
        )
        // TODO: 카프카 토픽으로 feedId, username 전송
        return feedDBRepository.save(feed)
    }

    fun updateFeed(feedId: String, updateFeedRequest: UpdateFeedRequest, username: String): Mono<Feed> {
        return feedDBRepository.existsById(feedId)
            .flatMap { exists ->
                if (exists) {
                    val feed = Feed(
                        feedId = feedId,
                        username = username,
                        content = updateFeedRequest.content,
                    )
                    feedDBRepository.save(feed)
                } else {
                    Mono.error(RuntimeException("Feed not found"))
                }
            }
    }

    fun deleteFeed(id: String, username: String): Mono<Boolean> {
        return feedDBRepository.findById(id)
            .flatMap { feed ->
                if (feed.username == username) {
                    feedDBRepository.deleteById(id).thenReturn(true)
                } else {
                    Mono.just(false)
                }
            }
            // TODO: 캐시의 피드도 삭제하기
    }

    fun getFeed(id: String): Mono<Feed> {
        return feedDBRepository.findById(id)
    }
}
