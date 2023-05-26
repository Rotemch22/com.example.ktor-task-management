package com.example.services

import com.example.exceptions.Exceptions
import com.example.models.Role
import com.example.models.User
import com.example.repository.UsersRepository
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class UsersService(private val usersRepository: UsersRepository) {

    fun insertUser(user : User) : Int{
        if (user.role == Role.END_USER && user.manager == null){
            logger.error { "user $user can't be add with role USER and without a manager" }
            throw Exceptions.EndUserWithoutManager(user)
        }

        val id = usersRepository.insertUser(user)
        logger.info { "user $user inserted successfully with id $id" }
        return id
    }

    fun getUserByUserName(username: String): User? {
        return usersRepository.getUserByUserName(username)
    }

    fun getManagersToUsersMap() : Map<User, List<User>> {
        return usersRepository.getManagersToUsersMap()
    }

    fun initializeAdminUser() {
        usersRepository.initializeAdminUser()
    }

}