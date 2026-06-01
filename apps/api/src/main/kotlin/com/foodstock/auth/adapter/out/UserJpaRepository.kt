package com.foodstock.auth.adapter.out

import com.foodstock.auth.domain.model.User
import com.foodstock.auth.domain.port.out.UserRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface UserJpaRepositoryDelegate : JpaRepository<UserJpaEntity, UUID> {
    fun findByEmail(email: String): UserJpaEntity?
    fun existsByEmail(email: String): Boolean
}

@Repository
class UserJpaRepository(
    private val delegate: UserJpaRepositoryDelegate
) : UserRepository {

    override fun save(user: User): User =
        delegate.save(UserJpaEntity.fromDomain(user)).toDomain()

    override fun findByEmail(email: String): User? =
        delegate.findByEmail(email)?.toDomain()

    override fun findById(id: UUID): User? =
        delegate.findById(id).orElse(null)?.toDomain()

    override fun existsByEmail(email: String): Boolean =
        delegate.existsByEmail(email)
}
