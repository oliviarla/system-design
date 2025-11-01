package com.x.gatewayapp.config

import com.x.gatewayapp.filter.CustomServerSecurityContextRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.ServerSecurityContextRepository

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {
    @Bean
    fun filterChain(
        http: ServerHttpSecurity,
    ): SecurityWebFilterChain {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        http.headers { configurer -> configurer.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable) }
        http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        http.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        http.logout { logoutSpec -> logoutSpec.disable() }
        http.securityContextRepository(serverSecurityContextRepository())
        // TODO: For now, each strict is handled in the individual microservices. Make it stricter later. ex) use anonymous user.
        http.authorizeExchange { authorize ->
            authorize.anyExchange().permitAll()
        }
        return http.build()
    }
    @Bean
    fun serverSecurityContextRepository(): ServerSecurityContextRepository {
        return CustomServerSecurityContextRepository()
    }
}
