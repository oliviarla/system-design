package com.x.feedapp.user.service

import com.x.feedapp.user.domain.User
import com.x.feedapp.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserService(private val userRepository: UserRepository, private val passwordEncoder: PasswordEncoder) {

    fun join(username: String, password: String): Mono<Boolean> {
        return validateUsername(username)
            .then(validatePassword(password))
            .map { isValid ->
                if (isValid) {
                    val user = User(
                        username = username,
                        password = passwordEncoder.encode(password),
                        isNewUser = true,
                    )
                    userRepository.save(user).thenReturn(true)
                } else {
                    Mono.just(false)
                }
            }.flatMap { it }
    }

    private fun validateUsername(username: String): Mono<Boolean> {
        if (username.isBlank() || username.length < 3 || username.length > 20) {
            return Mono.error(IllegalArgumentException("username은 3자 이상 20자 이하로 입력해야 합니다."))
        }
        val usernameRegex = Regex("^[a-z0-9_]+$")
        if (!usernameRegex.matches(username)) {
            return Mono.error(IllegalArgumentException("username은 영문 소문자, 숫자, _ 만 사용할 수 있습니다."))
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
