package com.x.feedapp.user.security

import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component


@Component
class CustomReactiveAuthenticationManager(
    passwordEncoder: PasswordEncoder?,
    userDetailsService: CustomReactiveUserDetailService
) : UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService) {
    init {
        this.setPasswordEncoder(passwordEncoder)
    }
}
