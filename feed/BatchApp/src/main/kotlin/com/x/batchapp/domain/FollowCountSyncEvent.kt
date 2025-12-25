package com.x.batchapp.domain

data class FollowCountSyncEvent(
    val sourceUsername: String,
    val targetUsername: String,
    val action: FollowAction
)

enum class FollowAction {
    FOLLOW,
    UNFOLLOW
}