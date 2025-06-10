package com.x.feedapp.user.controller

import com.x.feedapp.user.controller.dto.JoinRequest
import com.x.feedapp.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

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
    fun myInfo(): Mono<Any> {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getPrincipal);
    }
}
