package com.x.gatewayapp.filter

import com.x.gatewayapp.jwt.JwtGenerator
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SecurityContextToJwtGatewayFilterFactory(
    private val jwtGenerator: JwtGenerator
) : AbstractGatewayFilterFactory<SecurityContextToJwtGatewayFilterFactory.Config>(Config::class.java) {

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            ReactiveSecurityContextHolder.getContext()
                .flatMap { securityContext ->
                    val authentication = securityContext.authentication
                    val jwtToken = if (authentication != null && authentication.isAuthenticated) {
                        generateJwtFromAuthentication(authentication)
                    } else {
                        null
                    }

                    val modifiedRequest = createRequestWithoutCookies(exchange, jwtToken)
                    val modifiedExchange = exchange.mutate().request(modifiedRequest).build()
                    chain.filter(modifiedExchange)
                }
                .switchIfEmpty(
                    // No security context found, forward request without cookies
                    chain.filter(
                        exchange.mutate()
                            .request(createRequestWithoutCookies(exchange, null))
                            .build()
                    )
                )
        }
    }

    private fun generateJwtFromAuthentication(authentication: org.springframework.security.core.Authentication): String {
        val username = authentication.principal.toString()
        val roles = authentication.authorities.map { it.authority }.toList()
        return jwtGenerator.generateToken(username, roles)
    }

    private fun createRequestWithoutCookies(
        exchange: org.springframework.web.server.ServerWebExchange,
        jwtToken: String?
    ): org.springframework.http.server.reactive.ServerHttpRequest {
        return exchange.request.mutate()
            .apply {
                if (jwtToken != null) {
                    header(HttpHeaders.AUTHORIZATION, "Bearer $jwtToken")
                }
            }
            .headers { headers ->
                headers.remove(HttpHeaders.COOKIE)
            }
            .build()
    }

    class Config {
        // Configuration properties can be added here if needed
    }
}
