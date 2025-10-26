package com.x.feedapp.config

import com.x.feedapp.filter.JwtFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
class SecurityConfig(
    private val jwtFilter: JwtFilter
) {
    @Bean
    fun filterChain(
        http: ServerHttpSecurity,
    ): SecurityWebFilterChain {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        http.headers { configurer -> configurer.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable) }
        http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        http.formLogin(ServerHttpSecurity.FormLoginSpec::disable)

        // Add JWT filter before authorization filter
        http.addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)

        http.authorizeExchange { authorize ->
            authorize
                // Require authentication for POST, PUT, DELETE on feeds
                .pathMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/feeds").authenticated()
                .pathMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/feeds/update/**").authenticated()
                .pathMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/feeds/**").authenticated()
                // Require authentication for /my endpoint and newsfeed
                .pathMatchers("/api/v1/feeds/my").authenticated()
                .pathMatchers("/api/v1/newsfeed/**").authenticated()
                // Allow all other requests (GET endpoints are public)
                .anyExchange().permitAll()
        }
        return http.build()
    }
}
