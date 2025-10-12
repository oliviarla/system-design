package com.x.userapp.user.controller

import com.x.userapp.user.controller.dto.JoinRequest
import com.x.userapp.user.service.FollowService
import com.x.userapp.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
    private val followService: FollowService
) {

    @PostMapping("/join")
    fun join(@RequestBody joinRequest: JoinRequest): Mono<ResponseEntity<Unit>> {
        return userService.join(joinRequest.username, joinRequest.password)
            .map { success ->
                if (success) {
                    ResponseEntity.ok().build()
                } else {
                    ResponseEntity.badRequest().build()
                }
            };
    }

    @GetMapping("/me")
    fun myUsername(): Mono<ResponseEntity<String>> {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getPrincipal)
            .map { username -> ResponseEntity.ok(username as String) }
            .switchIfEmpty(Mono.just(ResponseEntity.status(401).build()));
    }

    @GetMapping("/followings/{username}")
    fun followingsByUsername(@PathVariable username: String): Mono<List<String>> {
        return followService.getFollowings(username)
            .collectList()
    }

    @GetMapping("/followers/{username}")
    fun followersByUsername(@PathVariable username: String): Mono<List<String>> {
        return followService.getFollowers(username)
            .collectList()
    }

    @PostMapping("/follow/{username}")
    fun follow(@PathVariable username: String): Mono<Void> {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getPrincipal)
            .cast(String::class.java)
            .flatMap { currentUsername ->
                followService.follow(currentUsername, username)
            }
    }

    @PostMapping("/unfollow/{username}")
    fun unfollow(@PathVariable username: String): Mono<Void> {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getPrincipal)
            .cast(String::class.java)
            .flatMap { currentUsername ->
                followService.unfollow(currentUsername, username)
            }
    }

    @GetMapping("/followings/count/{username}")
    fun followingsCountByUsername(@PathVariable username: String): Mono<Long> {
        return followService.getFollowingCount(username)
    }

    @GetMapping("/followers/count/{username}")
    fun followersCountByUsername(@PathVariable username: String): Mono<Long> {
        return followService.getFollowerCount(username)
    }
}
