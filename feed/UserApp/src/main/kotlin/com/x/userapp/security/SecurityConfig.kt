package com.x.userapp.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.x.userapp.filter.JwtFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.util.CollectionUtils

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    private val jwtFilter: JwtFilter
) {
    @Bean
    fun filterChain(
        http: ServerHttpSecurity,
        loginFilter: LoginAuthenticationWebFilter?
    ): SecurityWebFilterChain {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        http.authorizeExchange { authorizeExchangeSpec ->
            authorizeExchangeSpec
                .pathMatchers("/api/v1/users/join", "/api/v1/users/login").permitAll()
                .pathMatchers("/api/v1/users/followings/**", "/api/v1/users/followers/**").permitAll()
                .anyExchange().authenticated()
        }
        http.headers { configurer -> configurer.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable) }
        http.addFilterAt(loginFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        http.addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        http.logout { logoutSpec ->
            logoutSpec
                .logoutUrl("/api/v1/users/logout")
                .logoutHandler { exchange, _ ->
                    exchange.exchange.session
                        .doOnNext { session ->
                            if (!CollectionUtils.isEmpty(session.attributes)) {
                                session.invalidate()
                            } else {
                                // Session which is internally made when user didn't log in
                                // doesn't have any attributes and will not be saved into redis.
                                // So just return 401 Response.
                                val response: ServerHttpResponse = exchange.exchange.response
                                response.statusCode = HttpStatusCode.valueOf(401)
                                response.setComplete()
                            }
                        }
                        .then()
                }
                .logoutSuccessHandler { exchange, _ ->
                    val response: ServerHttpResponse = exchange.exchange.response
                    response.setStatusCode(HttpStatus.OK)
                    response.setComplete()
                }
        }
        http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        http.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().registerKotlinModule()
    }
}
