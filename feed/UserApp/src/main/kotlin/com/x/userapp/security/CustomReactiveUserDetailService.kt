package com.x.userapp.security

import com.x.userapp.user.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class CustomReactiveUserDetailService(private val userRepository: UserRepository): ReactiveUserDetailsService {

    override fun findByUsername(username: String?): Mono<UserDetails> {
        if (username.isNullOrBlank()) {
            return Mono.error(UsernameNotFoundException("Username is null or blank."))
        }
        return userRepository.findByUsername(username)
            .map { user -> User(user.username, user.password, listOf(SimpleGrantedAuthority ("USER" ))) as UserDetails }
            .switchIfEmpty(Mono.error(UsernameNotFoundException("User not found.")))
    }
}
