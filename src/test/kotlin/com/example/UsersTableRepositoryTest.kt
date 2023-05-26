package com.example

import com.example.models.Role
import com.example.models.User
import com.example.repository.UsersRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.postgresql.ds.PGSimpleDataSource
import kotlin.test.assertEquals


class UsersTableRepositoryTest {

    private val dataSource = PGSimpleDataSource().apply {
        user = "test"
        password = "test"
        databaseName = "tasks_test"
        serverName = "localhost"
        portNumber = 5433
    }
    private val db = Database.connect(dataSource)

    private val usersRepository = UsersRepository(db)

    @Before
    fun resetDB(){
        transaction (db) {
            SchemaUtils.drop(UsersRepository.UsersTable)
            SchemaUtils.createMissingTablesAndColumns(UsersRepository.UsersTable)
        }
    }

    @After
    fun cleanDB(){
        transaction (db) {
            SchemaUtils.drop(UsersRepository.UsersTable)
        }
    }

    @Test
    fun testGetManagersToUsersMap(){
        val manager1 = User("manager1", "manager1", "manager1@email.com", Role.MANAGER, null, 1)
        val manager2 = User("manager2", "manager1", "manager2@email.com", Role.MANAGER, null, 2)
        val user1 = User("user1", "user1", "user1@email.com", Role.END_USER, manager1, 11)
        val user2 = User("user2", "user2", "user2@email.com", Role.END_USER, manager1, 22)
        val user3 = User("user3", "user3", "user3@email.com", Role.END_USER, manager2, 33)

        transaction (db) {
            usersRepository.insertUser(manager1)
            usersRepository.insertUser(manager2)
            usersRepository.insertUser(user1)
            usersRepository.insertUser(user2)
            usersRepository.insertUser(user3)
        }

        val readItems = usersRepository.getManagersToUsersMap()
        assertEquals(mapOf(
            manager1 to listOf(user1, user2),
            manager2 to listOf(user3)
        ), readItems)
    }

}