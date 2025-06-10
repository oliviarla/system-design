package com.x.feedapp.user.service

import com.x.feedapp.user.domain.User
import com.x.feedapp.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserService(private val userRepository: UserRepository, private val passwordEncoder: PasswordEncoder) {
    fun getFollowingIds(userId: Long): Flux<Long> {
        // TODO: Mock implementation, replace with actual logic
        return Flux.just(123, 234, 345);
    }

    fun join(username: String, password: String): Mono<Boolean> {
        return validateUsername(username)
            .then(validatePassword(password))
            .map { isValid ->
                if (isValid) {
                    val user = User(username = username, password = passwordEncoder.encode(password))
                    userRepository.save(user).thenReturn(true)
                } else {
                    Mono.just(false)
                }
            }.flatMap { it }
    }

    private fun validateUsername(username: String): Mono<Boolean> {
        if (username.isBlank()) {
            return Mono.just(false)
        }
        return userRepository.existsByUsername(username)
            .flatMap { exists ->
                if (exists) {
                    Mono.error(IllegalArgumentException("Username already exists."))
                } else {
                    Mono.just(true)
                }
            }
    }

    private fun validatePassword(password: String): Mono<Boolean> {
        val allowedSpecialChars = "!@#$%^&*()"
        val specialCharRegex = Regex("[$allowedSpecialChars]")
        val isValid = password.length in 8..20 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() } &&
                password.any { specialCharRegex.containsMatchIn(it.toString()) } &&
                !password.contains("\\s".toRegex())
        if (!isValid) {
            return Mono.error(IllegalArgumentException(
                "Password must be 8-20 characters long," +
                        " contain at least one uppercase letter," +
                        " one lowercase letter," +
                        " one digit, and one special character ($allowedSpecialChars)," +
                        " and must not contain whitespace."
            ))
        }
        return Mono.just(isValid)
    }
}
