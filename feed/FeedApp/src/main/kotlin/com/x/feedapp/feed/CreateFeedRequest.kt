package com.x.feedapp.feed

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class CreateFeedRequest (
    @Min(1) @Max(300)
    val content: String,
)
