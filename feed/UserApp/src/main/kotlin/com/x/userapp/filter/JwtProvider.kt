package com.x.userapp.filter

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret:your-secret-key-must-be-at-least-256-bits-long-for-HS256-algorithm}")
    private val secret: String,
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun verifyAndExtract(token: String): DecodedJwt {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        return DecodedJwt(claims)
    }
}

class DecodedJwt(private val claims: Claims) {
    val subject: String
        get() = claims.subject

    fun getClaim(name: String): Claim {
        return Claim(claims[name])
    }
}

class Claim(private val value: Any?) {
    fun asList(clazz: Class<String>): List<String> {
        return when (value) {
            is List<*> -> value.filterIsInstance<String>()
            else -> emptyList()
        }
    }
}