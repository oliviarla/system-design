package com.x.feedapp.user.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.util.CollectionUtils

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {
    @Bean
    fun filterChain(
        http: ServerHttpSecurity, manager: ReactiveAuthenticationManager?,
        loginFilter: LoginAuthenticationWebFilter?
    ): SecurityWebFilterChain {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        http.authorizeExchange { authorizeExchangeSpec ->
            authorizeExchangeSpec
                .pathMatchers("/join", "/login").permitAll()
                .anyExchange().authenticated()
        }
        http.headers { configurer -> configurer.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable) }
        http.addFilterAt(loginFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        http.logout { logoutSpec ->
            logoutSpec
                .logoutUrl("/logout")
                .logoutHandler { exchange, _ ->
                    exchange.exchange.session
                        .flatMap { session ->
                            return@flatMap if (!CollectionUtils.isEmpty(session.attributes)) {
                                session.invalidate()
                            } else {
                                // Session which is internally made when user didn't log in
                                // doesn't have any attributes and will not be saved into redis.
                                // So just return 401 Response.
                                val response: ServerHttpResponse = exchange.exchange.response
                                response.setStatusCode(HttpStatusCode.valueOf(401))
                                response.setComplete()
                            }
                        }
                }
                .logoutSuccessHandler { exchange, _ ->
                    val response: ServerHttpResponse = exchange.exchange.response
                    response.setStatusCode(HttpStatus.OK)
                    response.setComplete()
                }
        }
        http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        http.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        http.securityContextRepository(serverSecurityContextRepository())
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun serverSecurityContextRepository(): ServerSecurityContextRepository {
        return CustomServerSecurityContextRepository()
    }
}
