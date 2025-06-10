package com.x.feedapp.feed.service

import com.x.feedapp.feed.domain.Feed
import com.x.feedapp.feed.repository.FeedDBRepository
import com.x.feedapp.feed.repository.FeedRedisRepository
import com.x.feedapp.user.service.UserService
import reactor.core.publisher.Flux

class NewsFeedService(private val feedRedisRepository: FeedRedisRepository,
                      private val feedDBRepository: FeedDBRepository,
                      private val userService: UserService
) {

    fun getNewsFeed(userId: Long, page: Int): Flux<Feed> {
        return feedRedisRepository.getNewsFeedIds(userId, page)
            .switchIfEmpty(
                combineNewsFeedIds(userId, page)
            )
            .flatMap { feedId -> feedDBRepository.findById(feedId) }
    }

    private fun combineNewsFeedIds(userId: Long, page: Int): Flux<Long> {
        return Flux.empty()
//        userService.userServicegetFollowingIds(userId)
        // 팔로잉 중인 유저들이 작성한 모든 피드를 정렬해서 해당 페이지에 속하는 피드 아이디들을 조회한다.
    }

}
