package com.x.feedapp.feed.service

import com.x.feedapp.feed.domain.Feed
import com.x.feedapp.feed.repository.FeedDBRepository
import com.x.feedapp.feed.repository.FeedRedisRepository
import com.x.feedapp.user.service.FollowService
import com.x.feedapp.user.service.UserService
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.temporal.ChronoUnit

class NewsFeedService(
    private val feedRedisRepository: FeedRedisRepository,
    private val feedDBRepository: FeedDBRepository,
    private val userService: UserService,
    private val followService: FollowService,
    private val feedService: FeedService
) {
    fun getNewsFeed(username: String, size: Int, lastFeedId: String): Flux<Feed> {
        return feedRedisRepository.getNewsFeedIds(username, size, lastFeedId)
            .collectList()
            .flatMapMany { feedIds ->
                if (feedIds.size >= size) {
                    return@flatMapMany Flux.fromIterable(feedIds)
                } else {
                    // 조회된 feed ids가 size 보다 작다면
                    // 피드들을 조합해 ZSET에 추가한 후 다시 조회
                    combineNewsFeedIds(username, size, lastFeedId)
                        .then(Mono.defer {
                            feedRedisRepository.getNewsFeedIds(username, size, lastFeedId)
                                .collectList()
                        })
                        .flatMapMany { updatedFeedIds ->
                            Flux.fromIterable(updatedFeedIds)
                        }
                }
            }
            .flatMap { feedId -> feedDBRepository.findById(feedId.toString()) }
    }

    private fun combineNewsFeedIds(username: String, size: Int, lastFeedId: String): Mono<Unit> {
        return feedService.getFeed(lastFeedId)
            .flatMap { feed ->
                val lastCreatedAt = feed.createdAt ?: return@flatMap Mono.empty()
                expandTimeRangeAndFetchFeeds(username, lastCreatedAt, size)
            }
    }

    private fun expandTimeRangeAndFetchFeeds(
        username: String,
        lastCreatedAt: Instant,
        size: Int,
        dayRange: Int = 1,
        maxDayRange: Int = 3  // 최대 3일까지만 확장
    ): Mono<Unit> {
        val start = lastCreatedAt.minus(dayRange.toLong(), ChronoUnit.DAYS)
        val end = lastCreatedAt

        return followService.getFollowings(username)
            .collectList()
            .flatMap { followingIds ->
                if (followingIds.isEmpty()) {
                    return@flatMap Mono.empty()
                }

                Flux.fromIterable(followingIds)
                    // 각 사용자마다 주어진 시간 범위 내의 피드 ID를 가져와서 Redis에 저장
                    .flatMap { followingId ->
                        feedService.getFeedIdsBetweenDates(followingId, start, end)
                            .collectList()
                            .flatMap { feedIds ->
                                feedRedisRepository.saveNewsFeedIds(username, followingId, feedIds.toList())
                            }
                    }
                    .then(feedRedisRepository.getNewsFeedSize(username))
                    .flatMap { feedSize ->
                        if (feedSize < size && dayRange < maxDayRange) {
                            // 피드가 충분하지 않고 최대 범위에 도달하지 않았으면 시간 범위 확장
                            expandTimeRangeAndFetchFeeds(username, lastCreatedAt, size, dayRange + 1, maxDayRange)
                        } else {
                            // 최대 범위까지 확장했거나 충분한 데이터를 얻었으면 완료
                            Mono.empty()
                        }
                    }
            }
    }

}
