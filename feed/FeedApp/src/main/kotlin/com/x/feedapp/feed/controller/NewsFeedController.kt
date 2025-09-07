package com.x.feedapp.feed.controller

import com.x.feedapp.feed.controller.dto.FeedResponse
import com.x.feedapp.feed.controller.dto.toFeedResponse
import com.x.feedapp.feed.service.NewsFeedService
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/newsfeed")
class NewsFeedController(private val newsFeedService: NewsFeedService) {

    @GetMapping
    fun getNewsFeed(@RequestParam(defaultValue = "30") size: Int,
                    @RequestParam(defaultValue = "") lastFeedId: String): Mono<List<FeedResponse>> {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getPrincipal)
            .flatMapMany { username -> newsFeedService.getNewsFeed(username as String, size, lastFeedId) }
            .map { toFeedResponse(it) }
            .collectList()
            .map { it.sortedByDescending { feed -> feed.id } }
    }

}
