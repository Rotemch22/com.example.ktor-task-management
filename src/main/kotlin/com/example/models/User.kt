package com.example.models

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class User
    (val username: String,
     val password: String,
     val email: String,
     val role: Role,
     val manager: User?,
     val userId : Int = 0) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false

        return username == other.username &&
                email == other.email &&
                role == other.role &&
                manager == other.manager
    }

    override fun hashCode(): Int {
        return Objects.hash(username, email, role, manager)
    }
}

enum class Role {
    END_USER,
    MANAGER,
    ADMIN
}