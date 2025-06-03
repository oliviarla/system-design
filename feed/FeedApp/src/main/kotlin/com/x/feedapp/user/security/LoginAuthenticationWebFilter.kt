package com.x.feedapp.user.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.session.ReactiveFindByIndexNameSessionRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebSession
import reactor.core.publisher.Mono
import java.io.IOException
import java.nio.charset.StandardCharsets

@Component
class LoginAuthenticationWebFilter(authenticationManager: ReactiveAuthenticationManager) :
    AuthenticationWebFilter(authenticationManager) {
    init {
        this.setRequiresAuthenticationMatcher(
            ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/login")
        )
        this.setServerAuthenticationConverter { exchange: ServerWebExchange ->
            exchange.request.body
                .next()
                .flatMap { dataBuffer: DataBuffer ->
                    val loginRequest: LoginRequest
                    try {
                        loginRequest = ObjectMapper().readValue(dataBuffer.asInputStream(), LoginRequest::class.java)
                    } catch (e: IOException) {
                        return@flatMap Mono.error<Authentication>(
                            e
                        )
                    }
                    Mono.just<UsernamePasswordAuthenticationToken>(
                        UsernamePasswordAuthenticationToken(
                            loginRequest.username,
                            loginRequest.password
                        )
                    )
                }
        }
        this.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        this.setAuthenticationSuccessHandler(LoginAuthenticationWebFilterSuccessHandler())
        this.setAuthenticationFailureHandler(LoginAuthenticationWebFilterFailureHandler())
    }

    /**
     * 로그인 성공 시 세션에 사용자 정보를 저장하고, 응답을 작성한다.
     */
    private class LoginAuthenticationWebFilterSuccessHandler : ServerAuthenticationSuccessHandler {
        override fun onAuthenticationSuccess(
            webFilterExchange: WebFilterExchange,
            authentication: Authentication
        ): Mono<Void> {
            val exchange = webFilterExchange.exchange
            val response: ServerHttpResponse = exchange.response
            response.headers.contentType = MediaType.APPLICATION_JSON
            return exchange.session
                .doOnNext { session: WebSession ->
                    val attributes = session.attributes
                    val userDetails = authentication.principal as UserDetails
                    attributes["principal"] = userDetails.username
                    attributes["authorities"] = userDetails.authorities
                    attributes[ReactiveFindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME] =
                        userDetails.username
                }
                .then(Mono.defer {
                    val body: ByteArray = "login success".toByteArray(StandardCharsets.UTF_8)
                    val buffer: DataBuffer = response.bufferFactory().wrap(body)
                    response.writeWith(Mono.just(buffer))
                })
        }
    }

    private class LoginAuthenticationWebFilterFailureHandler : ServerAuthenticationFailureHandler {
        override fun onAuthenticationFailure(
            webFilterExchange: WebFilterExchange,
            exception: AuthenticationException?
        ): Mono<Void> {
            val exchange = webFilterExchange.exchange
            val response: ServerHttpResponse = exchange.response
            response.headers.contentType = MediaType.APPLICATION_JSON
            response.statusCode = HttpStatus.UNAUTHORIZED
            return Mono.defer {
                val buffer: DataBuffer = response.bufferFactory().wrap("login failed".toByteArray(StandardCharsets.UTF_8))
                response.writeWith(Mono.just(buffer))
            }
        }
    }
}
