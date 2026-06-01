package com.foodstock.auth.adapter.out

import com.foodstock.auth.domain.port.out.PasswordHashPort
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptPasswordHashAdapter : PasswordHashPort {
    private val encoder = BCryptPasswordEncoder()

    override fun hash(raw: String): String = encoder.encode(raw)
    override fun matches(raw: String, hashed: String): Boolean = encoder.matches(raw, hashed)
}
