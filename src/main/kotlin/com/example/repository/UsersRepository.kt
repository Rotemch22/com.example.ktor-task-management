package com.example.repository

import com.example.models.*
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class UsersRepository(private val db: Database) {
    private val cache = ConcurrentHashMap<Int, User>()
    private var loadedAllUsers = false

    fun insertUser(user: User): Int {
        val hashedPassword = BCrypt.hashpw(user.password, BCrypt.gensalt())
        val newUserId = transaction(db) {
            UsersTable.insertAndGetId {
                it[username] = user.username
                it[password] = hashedPassword
                it[email] = user.email
                it[role] = user.role
                it[manager] = user.manager?.userId?.let { managerId ->
                    EntityID(managerId, UsersTable)
                }
            }
        }.value

        logger.info { "user $user successfully created in db with id $newUserId" }
        addToCache(user.copy(userId = newUserId, password = hashedPassword))
        return newUserId
    }


    fun getManagersToUsersMap(): Map<User, List<User>> {
        val users = getAllUsers()
        return users.filter { it.role == Role.END_USER && it.manager != null }.groupBy { it.manager!! }
    }

    fun getUserByUserName(username: String): User? {
        return cache.values.find { it.username == username } ?: transaction(db) {
            UsersTable.getUsersWithManagerData(UsersTable.username eq username).singleOrNull()
                ?.also { user -> addToCache(user) }
        }
    }

    fun getUserById(userId: Int?): User? {
        return userId?.let {
            cache[userId] ?: transaction(db) {
                UsersTable.getUsersWithManagerData(UsersTable.id eq userId).singleOrNull()
                    ?.also { user -> addToCache(user) }
            }
        }
    }

    fun getAllUsers(): List<User> {
        if (!loadedAllUsers) {
            cache.putAll(transaction(db) {
                UsersTable.getUsersWithManagerData()
            }.associateBy { it.userId })
            loadedAllUsers = true
        }

        return cache.values.toList()
    }

    fun initializeAdminUser(): Int {
        return transaction(db) {
            // Check if admin user already exists
            val adminUser = UsersTable.select { UsersTable.role eq Role.ADMIN }.singleOrNull()
            adminUser?.let {
                adminUser[UsersTable.id].value
            } ?: run {
                val newUserId = UsersTable.insertAndGetId {
                    it[username] = "admin"
                    it[password] = BCrypt.hashpw("admin", BCrypt.gensalt())
                    it[email] = "admin@example.com"
                    it[role] = Role.ADMIN
                }.value

                logger.info { "Admin user created with ID: $newUserId" }
                newUserId
            }
        }
    }

    private fun addToCache(user: User) {
        cache[user.userId] = user
    }

    object UsersTable : IntIdTable() {
        val username = varchar("user_name", 100).uniqueIndex()
        val password = varchar("hashed_password", 100)
        val email = varchar("email", 100)
        val role = enumeration("role", Role::class)
        val manager = reference("manager", UsersTable).nullable()
    }

    private fun UsersTable.getUsersWithManagerData(where: Op<Boolean> = Op.TRUE): List<User> {
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

