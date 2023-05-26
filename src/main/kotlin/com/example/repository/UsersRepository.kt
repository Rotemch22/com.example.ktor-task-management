package com.example.repository

import com.example.models.*
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

private val logger = KotlinLogging.logger {}

class UsersRepository (private val db: Database){


    fun insertUser(user: User): Int {
        val id = transaction(db) {
            UsersTable.insertAndGetId {
                it[username] = user.username
                it[password] = BCrypt.hashpw(user.password, BCrypt.gensalt())
                it[email] = user.email
                it[role] = user.role
                it[manager] = user.manager?.userId?.let { managerId ->
                    EntityID(managerId, UsersTable)
                }
            }
        }

        logger.info { "user $user successfully created in db with id ${id.value}" }
        return id.value
    }


    fun getManagersToUsersMap() : Map<User, List<User>>{
        return transaction(db) {
            // query all the users with role END_USER and group them by their manager
            // filter out the users with null managers even though there shouldn't be any
            UsersTable.getUsersWithManagerData(UsersTable.role eq Role.END_USER)
                .filter { it.manager != null }
                .groupBy { it.manager!! }
        }
    }

    fun getUserByUserName(username: String): User? {
        return transaction (db) {
            UsersTable.getUsersWithManagerData(UsersTable.username eq username).firstOrNull()
        }
    }

    fun getUserById(userId : Int?): User? {
        return transaction (db) {
            UsersTable.getUsersWithManagerData(UsersTable.id eq userId).firstOrNull()
        }
    }

    fun getAllUsers(): List<User> {
        return transaction (db) {
            UsersTable.getUsersWithManagerData()
        }
    }

    fun initializeAdminUser() {
        transaction (db) {
            // Check if admin user already exists
            val adminUser = UsersTable.select { UsersTable.role eq Role.ADMIN }.singleOrNull()

            if (adminUser == null) {
                // Admin user doesn't exist, create it
                UsersTable.insert {
                    it[username] = "admin"
                    it[password] = BCrypt.hashpw("admin", BCrypt.gensalt())
                    it[email] = "admin@email.com"
                    it[role] = Role.ADMIN
                }
            }
        }
    }

    object UsersTable : IntIdTable() {
        val username = varchar("user_name", 100).uniqueIndex()
        val password = varchar("hashed_password", 100)
        val email = varchar("email", 100)
        val role = enumeration("role", Role::class)
        val manager = reference("manager", UsersTable).nullable()
    }

    private fun UsersTable.getUsersWithManagerData(where : Op<Boolean> = Op.TRUE): List<User> {
        val managerTable = UsersTable.alias("managerTable")
        return UsersTable.leftJoin(managerTable, { manager }, { managerTable[UsersTable.id] })
            .select { where }.map {
                User(
                    userId = it[UsersTable.id].value,
                    username = it[username],
                    password = it[password],
                    email = it[email],
                    role = it[role],
                    manager = it[manager]?.let { managerId ->
                        User(
                            userId = managerId.value,
                            username = it[managerTable[username]],
                            password = it[managerTable[password]],
                            email = it[managerTable[email]],
                            role = it[managerTable[role]],
                            manager = null
                        )
                    }
                )
            }
    }
}

