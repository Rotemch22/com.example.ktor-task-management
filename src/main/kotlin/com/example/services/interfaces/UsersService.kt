import com.example.models.User
import com.example.exceptions.Exceptions

/**
 * Interface for managing user-related operations.
 */
interface UsersService {
    /**
     * Inserts a new user into the system.
     *
     * @param user The user to be inserted.
     * @return The ID of the inserted user.
     * @throws Exceptions.EndUserWithoutManager if the user is an end user without a manager.
     */
    fun insertUser(user: User): Int

    /**
     * Retrieves all users in the system.
     *
     * @return A list of all users.
     */
    fun getAllUsers(): List<User>

    /**
     * Retrieves a user by their ID.
     *
     * @param userId The ID of the user to retrieve.
     * @return The user with the specified ID, or null if not found.
     */
    fun getUserById(userId: Int?): User?

    /**
     * Retrieves a user by their username.
     *
     * @param username The username of the user to retrieve.
     * @return The user with the specified username, or null if not found.
     */
    fun getUserByUserName(username: String): User?

    /**
     * Retrieves a mapping of managers to their associated users.
     *
     * @return A map where each manager is mapped to a list of associated users.
     */
    fun getManagersToUsersMap(): Map<User, List<User>>

    /**
     * Initializes the admin user in the system.
     * This should be called during system initialization.
     * @return the user id of the admin user
     */
    fun initializeAdminUser(): Int
}
