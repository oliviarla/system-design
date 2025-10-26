package com.x.gatewayapp.filter

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.util.CollectionUtils
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebSession
import reactor.core.publisher.Mono


class CustomServerSecurityContextRepository : ServerSecurityContextRepository {
    override fun save(exchange: ServerWebExchange, context: SecurityContext): Mono<Void> {
        return Mono.empty()
    }

    override fun load(exchange: ServerWebExchange): Mono<SecurityContext> {
        return exchange.session
            .flatMap { session: WebSession ->
                val principal = session.getAttribute<Any>("principal")
                val authorities = session.getAttribute<Set<SimpleGrantedAuthority>>("authorities")
                if (principal == null || CollectionUtils.isEmpty(authorities)) {
                    return@flatMap Mono.empty()
                }
                val authentication: Authentication =
                    UsernamePasswordAuthenticationToken(principal, null, authorities)
                Mono.just<SecurityContextImpl>(SecurityContextImpl(authentication))
            }
    }
}
