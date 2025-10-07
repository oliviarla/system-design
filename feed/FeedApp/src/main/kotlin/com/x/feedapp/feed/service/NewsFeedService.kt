package com.x.feedapp.feed.service

import com.x.feedapp.feed.domain.Feed
import com.x.feedapp.feed.repository.FeedDBRepository
import com.x.feedapp.feed.repository.FeedRedisRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class NewsFeedService(
    private val feedRedisRepository: FeedRedisRepository,
    private val feedDBRepository: FeedDBRepository,
    private val followService: FollowService,
    private val feedService: FeedService
) {
    /**
     * 1. Redis에서 뉴스피드 ID들을 조회
     * 2. 조회된 ID들이 size보다 작다면 팔로잉 유저들의 이전 피드들을 조합해 Redis에 추가
     * 3. 다시 Redis에서 뉴스피드 ID들을 조회
     * 4. 조회된 ID들로 피드 상세 정보를 조회해 반환
     */
    fun getNewsFeed(username: String, size: Int, lastFeedId: String): Flux<Feed> {
        return feedRedisRepository.getNewsFeedIds(username, size, lastFeedId)
            .map { it.toString() }
            .collectList()
            .filter { it.size >= size }
            .switchIfEmpty(
                combineNewsFeedIds(username, size, lastFeedId)
                    .then(Mono.defer {
                        feedRedisRepository
                            .getNewsFeedIds(username, size, lastFeedId)
                            .map { it.toString() }
                            .collectList()
                    })
            )
            .filter { it.isNotEmpty() }
            .flatMapMany { feedIds -> feedDBRepository.findAllById(feedIds) }
    }


    private fun combineNewsFeedIds(username: String, size: Int, lastFeedId: String): Mono<Unit> {
        return if (lastFeedId.isEmpty()) {
            expandTimeRangeAndFetchFeeds(username, Instant.now(), size)
        } else {
            feedService.getFeed(lastFeedId)
                .flatMap { feed ->
                    val lastCreatedAt = feed.createdAt ?: return@flatMap Mono.empty()
                    expandTimeRangeAndFetchFeeds(username, lastCreatedAt, size)
                }
        }
    }

    private fun expandTimeRangeAndFetchFeeds(
        username: String,
        lastCreatedAt: Instant,
        size: Int,
        dayRange: Int = 1,
        maxDayRange: Int = 3  // 최대 3일까지만 확장
    ): Mono<Unit> {
        return followService.getFollowings(username)
            .collectList()
            .flatMap { followingIds ->
                if (followingIds.isEmpty()) {
                    return@flatMap Mono.empty()
                }

                Flux.fromIterable(followingIds)
                    .flatMap { followingId ->
                        feedService
                            .getFeedIdsBetweenDates(
                                followingId,
                                lastCreatedAt.minus(dayRange.toLong(), ChronoUnit.DAYS),
                                lastCreatedAt.minus(dayRange.toLong() - 1, ChronoUnit.DAYS)
                            )
                            .collectList()
                            .flatMap { feedIds ->
                                feedRedisRepository.saveNewsFeedIds(username, followingId, feedIds.toList())
                            }
                    }
                    .then(feedRedisRepository.getNewsFeedSize(username))
                    .flatMap { feedSize ->
                        if (feedSize in 0..<size && dayRange < maxDayRange) {
                            // 피드가 충분하지 않고 최대 범위에 도달하지 않았으면 시간 범위 확장
                            expandTimeRangeAndFetchFeeds(username, lastCreatedAt, size, dayRange + 1, maxDayRange)
                        } else {
                            // 최대 범위까지 확장했거나, 충분한 데이터를 얻었으면 완료
                            Mono.empty()
                        }
                    }
            }
    }

}
