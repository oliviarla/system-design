package com.x.feedapp.feed

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.RuntimeException

@Service
class FeedService(private val feedDBRepository: FeedDBRepository) {

    private val logger : Logger? = LoggerFactory.getLogger(FeedService::class.java)

    fun createFeed(createFeedRequest: CreateFeedRequest, userId: Long): Mono<Feed> {
        val feed = Feed(
            content = createFeedRequest.content,
            userId = userId,
        )
        return feedDBRepository.save(feed)
            .doOnNext { feed -> logger?.info(feed.toString()) }
        // TODO: 카프카 토픽으로 feedId, userId 전송
//            .flatMap {  }
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

    /**
     * feeds are up to 30
     */
    fun getNewsFeed(userId: Long): Flux<Feed> {
        // 1. Cache에 Sorted Set 있는지 확인 -> 있다면 해당 ID들로 DB에서 조회
        // 2. Cache에 없으면 회원 ID의 팔로잉 ID 목록 조회
        // 3. 각 회원마다 최신 피드를 캐싱하고 있으므로 최신 5개씩 가져와 이를 조합한다. -> ID들로 DB에서 조회

        return Flux.just(Feed(
            id = 1,
            userId = userId,
            content = "dummy",
        ))
    }

    private fun getFeedsByIds(feedIds: Flux<Long>): Flux<Feed> {
        return feedDBRepository.findAllById(feedIds)
            .doOnNext { feed -> logger?.info(feed.toString()) }
    }
}
