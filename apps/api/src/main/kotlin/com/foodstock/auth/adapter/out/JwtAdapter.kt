package com.foodstock.auth.adapter.out

import com.foodstock.auth.domain.model.User
import com.foodstock.auth.domain.port.out.JwtPort
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JwtAdapter(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration-ms:86400000}") private val expirationMs: Long
) : JwtPort {

    private val key by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    override fun generateToken(user: User): String = Jwts.builder()
        .subject(user.email)
        .claim("userId", user.id.toString())
        .claim("name", user.name)
        .issuedAt(Date())
        .expiration(Date(System.currentTimeMillis() + expirationMs))
        .signWith(key)
        .compact()

    override fun validateToken(token: String): Boolean = runCatching {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        true
    }.getOrDefault(false)

    override fun extractEmail(token: String): String =
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.subject
}
