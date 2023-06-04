package com.example

import com.example.exceptions.Exceptions
import com.example.models.Role
import com.example.models.User
import com.example.repository.UsersRepository
import com.example.services.UsersServiceImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UsersServiceTest {

    private val usersRepository = mockk<UsersRepository>()
    private val usersService = UsersServiceImpl(usersRepository)

    @Test
    fun testInsertManagerAndUser(){
        val manager = User("manager", "manager", "manager@email.com", Role.MANAGER, null)
        val user = User("user", "user", "user@email.com", Role.END_USER, manager)

        every { usersRepository.insertUser(manager) } returns 1
        every { usersRepository.insertUser(user) } returns 2

        assertEquals(1, usersService.insertUser(manager))
        assertEquals(2, usersService.insertUser(user))
    }

    @Test
    fun testInsertUserWithoutManager(){
        val user = User("user1", "user1", "user1@email.com", Role.END_USER, null)
        assertFailsWith(Exceptions.EndUserWithoutManager::class) { usersService.insertUser(user) }
    }
}