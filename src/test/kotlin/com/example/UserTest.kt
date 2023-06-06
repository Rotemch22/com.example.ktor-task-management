package com.example

import com.example.models.Role
import com.example.models.User
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UserTest {

    @Test
    fun testEquals() {
        val user1 = User("john", "password123", "john@example.com", Role.END_USER, null, 1)
        val user2 = User("john", "password123", "john@example.com", Role.END_USER, null, 1)
        val user3 = User("jane", "password123", "jane@example.com", Role.END_USER, null, 2)

        // Same properties, should be equal
        assertEquals(user1, user2)

        // Different username, should not be equal
        assertNotEquals(user1, user3)
    }

    @Test
    fun testHashCode() {
        val user1 = User("john", "password123", "john@example.com", Role.END_USER, null, 1)
        val user2 = User("john", "password123", "john@example.com", Role.END_USER, null, 1)
        val user3 = User("jane", "password123", "jane@example.com", Role.END_USER, null, 2)

        // Same properties, should have the same hash code
        assertEquals(user1.hashCode(), user2.hashCode())

        // Different username, should have different hash codes
        assertNotEquals(user1.hashCode(), user3.hashCode())
    }
}
