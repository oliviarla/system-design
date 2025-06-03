package com.x.feedapp.user.controller.dto

import org.apache.kafka.common.config.types.Password

data class JoinRequest(val username: String, val password: String)
