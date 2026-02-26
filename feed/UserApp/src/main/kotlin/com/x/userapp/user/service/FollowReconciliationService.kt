package com.x.userapp.user.service

import com.x.userapp.user.domain.FollowerByUser
import com.x.userapp.user.domain.FollowerKey
import com.x.userapp.user.domain.FollowingKey
import com.x.userapp.user.repository.FollowerByUserRepository
import com.x.userapp.user.repository.FollowingByUserRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import reactor.core.publisher.Mono

//@Service
class FollowReconciliationService(
    private val followingByUserRepository: FollowingByUserRepository,
    private val followerByUserRepository: FollowerByUserRepository
) {
    private val logger = LoggerFactory.getLogger(FollowReconciliationService::class.java)

    @KafkaListener(topics = [FollowService.TOPIC_FOLLOW_FAILED], groupId = "follow-reconciliation-group")
    fun handleFailedFollow(message: String) {
        val parts = message.split(" ")
        if (parts.size != 2) {
            logger.error("Invalid message format in DLQ: $message")
            return
        }

        val currentUsername = parts[0]
        val usernameToFollow = parts[1]

        logger.info("Processing failed follow reconciliation: $currentUsername -> $usernameToFollow")

        reconcileFollow(currentUsername, usernameToFollow)
            .doOnSuccess {
                logger.info("Successfully reconciled follow: $currentUsername -> $usernameToFollow")
            }
            .doOnError { error ->
                logger.error("Failed to reconcile follow: $currentUsername -> $usernameToFollow. Will retry on next poll.", error)
            }
            .subscribe()
    }

    private fun reconcileFollow(currentUsername: String, usernameToFollow: String): Mono<Void> {
        val followingKey = FollowingKey(currentUsername, usernameToFollow)
        val followerKey = FollowerKey(usernameToFollow, currentUsername)

        // Check if following record exists
        return followingByUserRepository.existsById(followingKey)
            .flatMap { followingExists ->
                if (!followingExists) {
                    logger.warn("Following record no longer exists for $currentUsername -> $usernameToFollow. Skipping reconciliation.")
                    return@flatMap Mono.empty<Void>()
                }

                // Check if follower record exists
                followerByUserRepository.existsById(followerKey)
                    .flatMap { followerExists ->
                        if (followerExists) {
                            logger.info("Follower record already exists for $usernameToFollow <- $currentUsername. Reconciliation not needed.")
                            Mono.empty()
                        } else {
                            // Create the missing follower record
                            val follower = FollowerByUser(key = followerKey)
                            followerByUserRepository.save(follower)
                                .doOnSuccess {
                                    logger.info("Created missing follower record during reconciliation: $usernameToFollow <- $currentUsername")
                                }
                                .then()
                        }
                    }
            }
            .onErrorResume { error ->
                logger.error("Error during reconciliation for $currentUsername -> $usernameToFollow", error)
                Mono.error(error) // Re-throw to trigger retry
            }
    }

    /**
     * Manual cleanup method for orphaned following records
     * Can be called by scheduled job or admin API
     */
    fun cleanupOrphanedFollowing(currentUsername: String, usernameToFollow: String): Mono<Void> {
        val followingKey = FollowingKey(currentUsername, usernameToFollow)

        logger.info("Cleaning up orphaned following record: $currentUsername -> $usernameToFollow")

        return followingByUserRepository.deleteById(followingKey)
            .doOnSuccess {
                logger.info("Deleted orphaned following record: $currentUsername -> $usernameToFollow")
            }
            .onErrorResume { error ->
                logger.error("Failed to cleanup orphaned following record: $currentUsername -> $usernameToFollow", error)
                Mono.error(error)
            }
    }
}