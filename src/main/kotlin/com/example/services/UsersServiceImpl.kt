package com.example.services

import UsersService
import com.example.exceptions.Exceptions
import com.example.models.Role
import com.example.models.User
import com.example.repository.UsersRepository
import com.example.routes.toUserResponse
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class UsersServiceImpl(private val usersRepository: UsersRepository) : UsersService{

    override fun insertUser(user : User) : Int{
        if (user.role == Role.END_USER && (user.manager == null || user.manager.role != Role.MANAGER)){
            logger.error { "user $user can't be add with role USER and without a manager" }
            throw Exceptions.EndUserWithoutManager(user.toUserResponse())
        }

        val id = usersRepository.insertUser(user)
        logger.info { "user $user inserted successfully with id $id" }
        return id
    }

    override fun getAllUsers(): List<User> {
        return usersRepository.getAllUsers()
    }

    override fun getUserById(userId : Int?): User? {
        return usersRepository.getUserById(userId)
    }

    override fun getUserByUserName(username: String): User? {
        return usersRepository.getUserByUserName(username)
    }

    override fun getManagersToUsersMap() : Map<User, List<User>> {
        return usersRepository.getManagersToUsersMap()
    }

    override fun initializeAdminUser(): Int {
        return usersRepository.initializeAdminUser()
    }

}