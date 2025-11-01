package com.x.userapp.filter

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtFilter(
    private val jwtProvider: JwtProvider
): WebFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain
    ): Mono<Void?> {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange)
        }

        val token = authHeader.removePrefix("Bearer ").trim()

        return try {
            val decoded = jwtProvider.verifyAndExtract(token)
            val userId = decoded.subject
            val roles = decoded.getClaim("roles").asList(String::class.java)

            // SecurityContext에 사용자 정보 저장
            val authentication = UsernamePasswordAuthenticationToken(userId, null, roles.map { SimpleGrantedAuthority(it) })
            val context = SecurityContextImpl(authentication)

            chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)))

        } catch (e: Exception) {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            exchange.response.setComplete()
        }
    }
}