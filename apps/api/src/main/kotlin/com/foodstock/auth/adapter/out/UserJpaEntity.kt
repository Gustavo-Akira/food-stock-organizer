package com.foodstock.auth.adapter.out

import com.foodstock.auth.domain.model.User
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "users")
class UserJpaEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false, unique = true)
    val email: String = "",

    @Column(name = "password", nullable = false)
    val passwordHash: String = ""
) {
    fun toDomain(): User = User(id = id, name = name, email = email, passwordHash = passwordHash)

    companion object {
        fun fromDomain(user: User) = UserJpaEntity(
            id = user.id,
            name = user.name,
            email = user.email,
            passwordHash = user.passwordHash
        )
    }
}
