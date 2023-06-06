package com.example.repository.interfaces

import com.example.models.User

/**
 * Repository interface for managing users in the database.
 */
interface UsersRepository {
    /**
     * Inserts a new user into the database.
     * @param user The user to be inserted.
     * @return The ID of the inserted user.
     */
    fun insertUser(user: User): Int

    /**
     * Retrieves a user by username.
     * @param username The username of the user.
     * @return The user with the specified username, or null if not found.
     */
    fun getUserByUserName(username: String): User?

    /**
     * Retrieves a user by ID.
     * @param userId The ID of the user.
     * @return The user with the specified ID, or null if not found.
     */
    fun getUserById(userId: Int?): User?

    /**
     * Retrieves all users from the database.
     * @return A list of users.
     */
    fun getAllUsers(): List<User>

    /**
     * Retrieves a mapping of managers to their associated users.
     * @return A map where the keys are managers and the values are lists of users.
     */
    fun getManagersToUsersMap(): Map<User, List<User>>

    /**
     * Initializes the admin user if it doesn't already exist and returns its ID.
     * @return The ID of the admin user.
     */
    fun initializeAdminUser(): Int
}
