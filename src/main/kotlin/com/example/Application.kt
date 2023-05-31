package com.example

import com.example.exceptions.ErrorResponse
import com.example.exceptions.Exceptions
import com.example.repository.TasksRepository
import com.example.repository.UsersRepository
import com.example.routes.taskRoutes
import com.example.routes.userRoutes
import com.example.services.TasksService
import com.example.services.UsersService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import org.mindrot.jbcrypt.BCrypt
import org.postgresql.ds.PGSimpleDataSource
import kotlin.reflect.KClass

@kotlinx.serialization.Serializable
data class UserSession(val username: String) : Principal

fun main() {
    val dataSource = PGSimpleDataSource().apply {
        user = "test"
        password = "test"
        databaseName = "tasks"
        serverName = "localhost"
        portNumber = 5432
    }
    val db = Database.connect(dataSource)

    val appModule = module {
        single { TasksRepository(db) }  // Inject the database dependency for the TasksRepository
        single { UsersRepository(db) }  // Inject the database dependency for the UsersRepository
        single { UsersService(get()) }  // Inject the UsersRepository dependency for the UsersService
        single { TasksService(get(), get()) } // Inject the TasksRepository and UsersService dependencies for the TasksService
    }

    startKoin {
        modules(appModule)
    }

    // Create the database tables
    transaction(db) {
        SchemaUtils.create(UsersRepository.UsersTable, TasksRepository.TasksTable)
    }

    val usersService: UsersService = getKoin().get()
    val tasksService: TasksService = getKoin().get()

    usersService.initializeAdminUser()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module(tasksService, usersService)
    }.start(wait = true)
}

fun Application.module(tasksService: TasksService, usersService: UsersService) {
    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception(exceptionHandler(HttpStatusCode.NotFound, Exceptions.TaskNotFoundException::class))
        exception(exceptionHandler(HttpStatusCode.UnprocessableEntity, Exceptions.MismatchedTaskIdException::class))
        exception(exceptionHandler(HttpStatusCode.BadRequest, Exceptions.TaskDueDatePastException::class))
        exception(exceptionHandler(HttpStatusCode.BadRequest, Exceptions.InvalidTaskQueryValueException::class))
        exception(exceptionHandler(HttpStatusCode.BadRequest, Exceptions.EndUserWithoutManager::class))
        exception(exceptionHandler(HttpStatusCode.BadRequest, Exceptions.UserWithInvalidManagerId::class))
        exception(exceptionHandler(HttpStatusCode.Unauthorized, Exceptions.NoLoggedInUserException::class))
        exception(exceptionHandler(HttpStatusCode.Unauthorized, Exceptions.NoUserFoundForLoggedInUserException::class))
        exception(exceptionHandler(HttpStatusCode.Unauthorized, Exceptions.TaskNotAuthorizedForUser::class))
    }

    install(Sessions) {
        cookie<UserSession>("SESSION") {
            cookie.extensions["SameSite"] = "lax" // Set the SameSite attribute for the cookie
        }
    }


    install(Authentication) {
        session<UserSession>("sessionAuth") {
            challenge("/login")
            validate { session ->
                // Validate the session and return the principal (user) associated with the session
                UserSession(session.username)
            }
        }
    }

    routing {
        route("/login") {
            get {
                // Serve the login form HTML
                call.respondText(buildLoginForm(), ContentType.Text.Html)
            }
            post {
                // Handle the form submission
                val parameters = call.receiveParameters()
                val username = parameters["username"]
                val password = parameters["password"]

                // Validate the username and password
                val user = username?.let { usersService.getUserByUserName(username) }
                if (user != null && BCrypt.checkpw(password, user.password)) {
                    val session = UserSession(username)
                    call.sessions.set(session)
                    call.respondRedirect("/api") // Redirect to a default protected route

                } else {
                    call.respondRedirect("/login")
                }
            }
        }

        authenticate("sessionAuth") {
            route("/api") {
                // Handle the request to display available APIs
                get {
                    val apiList = listOf(
                        "GET /tasks",
                        "GET /tasks/{id}",
                        "POST /tasks",
                        "PUT /tasks/{id}",
                        "DELETE /tasks/{id}",
                        "GET /users",
                        "GET /users/{id}",
                        "POST /users",
                        "PUT /users/{id}",
                        "DELETE /users/{id}"
                    )
                    call.respondText(apiList.joinToString("\n"), ContentType.Text.Plain)
                }
            }
            taskRoutes(tasksService)
            userRoutes(usersService)
        }
    }
}


fun buildLoginForm(): String {
    // Build and return the HTML form
    return """
        <html>
        <body>
            <h1>Login</h1>
            <form method="POST" action="/login">
                <input type="text" name="username" placeholder="Username"><br>
                <input type="password" name="password" placeholder="Password"><br>
                <button type="submit">Login</button>
            </form>
        </body>
        </html>
    """.trimIndent()
}

fun <T : Exception> exceptionHandler(
    status: HttpStatusCode,
    exceptionType: KClass<T>
): suspend (ApplicationCall, Throwable) -> Unit =
    { call, cause ->
        if (cause::class == exceptionType) {
            call.respond(status, ErrorResponse(cause.message ?: ""))
        }
    }

fun ApplicationCall.getLoggedInUsername(): String {
    val userSession: UserSession? = sessions.get()
    return userSession?.username ?: throw Exceptions.NoLoggedInUserException ()

}