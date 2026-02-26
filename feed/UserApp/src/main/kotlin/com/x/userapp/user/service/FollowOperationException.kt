package com.x.userapp.user.service

class FollowOperationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)