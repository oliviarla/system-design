package com.x.feedapp.user.service

import com.x.feedapp.user.domain.FollowerByUser
import com.x.feedapp.user.domain.FollowerKey
import com.x.feedapp.user.domain.FollowingByUser
import com.x.feedapp.user.domain.FollowingKey
import com.x.feedapp.user.repository.FollowerByUserRepository
import com.x.feedapp.user.repository.FollowingByUserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

@Service
class FollowService(
    private val followingByUserRepository: FollowingByUserRepository,
    private val followerByUserRepository: FollowerByUserRepository
) {
    fun getFollowings(username: String): Flux<String> {
        return followingByUserRepository.findAllByKeyUsername(username)
            .map { followingByUser -> followingByUser.key.followingUsername }
    }

    fun getFollowers(username: String): Flux<String> {
        return followerByUserRepository.findAllByKeyUsername(username)
            .map { followerByUser -> followerByUser.key.followerUsername }
    }

    fun follow(currentUsername: String, usernameToFollow: String): Mono<Void> {
        return checkIfAlreadyFollowing(currentUsername, usernameToFollow)
            .flatMap { exists ->
                if (exists) {
                    Mono.error(IllegalStateException("Already following."))
                } else {
                    saveFollowData(currentUsername, usernameToFollow)
                }
            }
    }

    private fun checkIfAlreadyFollowing(currentUsername: String, usernameToFollow: String): Mono<Boolean> {
        val followingKey = FollowingKey(currentUsername, usernameToFollow)
        val followerKey = FollowerKey(usernameToFollow, currentUsername)

        val existsInFollowing = followingByUserRepository.existsById(followingKey)
        val existsInFollower = followerByUserRepository.existsById(followerKey)

        return Mono.zip(existsInFollowing, existsInFollower)
            .map { (existsInFollowing, existsInFollower) ->
                existsInFollowing || existsInFollower
            }
    }

    private fun saveFollowData(currentUsername: String, usernameToFollow: String): Mono<Void> {
        val following = FollowingByUser(key = FollowingKey(currentUsername, usernameToFollow))
        val follower = FollowerByUser(key = FollowerKey(usernameToFollow, currentUsername))

        return followingByUserRepository.save(following)
            .then(followerByUserRepository.save(follower))
            .then()
    }

    fun unfollow(currentUsername: String, usernameToUnfollow: String): Mono<Void> {
        return checkIfAlreadyFollowing(currentUsername, usernameToUnfollow)
            .flatMap { exists ->
                if (!exists) {
                    Mono.error(IllegalStateException("Not following."))
                } else {
                    deleteFollowData(currentUsername, usernameToUnfollow)
                }
            }
    }

    private fun deleteFollowData(currentUsername: String, usernameToUnfollow: String): Mono<Void> {
        val followingKey = FollowingKey(currentUsername, usernameToUnfollow)
        val followerKey = FollowerKey(usernameToUnfollow, currentUsername)

        return followingByUserRepository.deleteById(followingKey)
            .then(followerByUserRepository.deleteById(followerKey))
    }
}