package com.x.feedapp.user.controller

import com.x.feedapp.user.controller.dto.JoinRequest
import com.x.feedapp.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

    @PostMapping("/join")
    fun join(joinRequest: JoinRequest): Mono<ResponseEntity<Unit>> {
        return userService.join(joinRequest.username, joinRequest.password)
            .map { success ->
                if (success) {
                    ResponseEntity.ok().build()
                } else {
                    ResponseEntity.badRequest().build()
                }
            };
    }

}