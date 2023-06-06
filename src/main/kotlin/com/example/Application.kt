package com.example

import TasksService
import UsersService
import com.example.exceptions.ErrorResponse
import com.example.exceptions.Exceptions
import com.example.models.User
import com.example.repository.TasksRepository
import com.example.repository.TasksRepositoryImpl
import com.example.repository.UsersRepository
import com.example.repository.UsersRepositoryImpl
import com.example.routes.taskRoutes
import com.example.routes.userRoutes
import com.example.services.TasksServiceImpl
import com.example.services.UsersServiceImpl
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
import java.util.*

@kotlinx.serialization.Serializable
data class UserSession(val username: String) : Principal
data class RequestContext(val user : User)

fun main() {
    // Initialize the application components
    val db = initializeDatabase()
    // Create the database tables
    createDatabaseTables(db)


    // Setup Koin dependencies
    startKoin {
        modules(koinAppModule(db))
    }


    // get services via Koin and add admin user if it doesn't exist
    val usersService: UsersService = getKoin().get()
    val tasksService: TasksService = getKoin().get()

    usersService.initializeAdminUser()

    // Start the application server
    startServer(tasksService, usersService)
}

fun initializeDatabase(): Database {
    val properties = Properties()

    // Load the properties from the application.properties file
    properties.load(Application::class.java.classLoader.getResourceAsStream("application.properties"))

    // Load database configuration from the environment or a config file
    val dataSource = PGSimpleDataSource().apply {
        user = properties.getProperty("DB_USER") ?: "test"
        password = properties.getProperty("DB_PASSWORD") ?: "test"
        databaseName = properties.getProperty("DB_NAME") ?: "tasks"
        serverName = properties.getProperty("DB_HOST") ?: "localhost"
        portNumber = properties.getProperty("DB_PORT")?.toIntOrNull() ?: 5432
    }

    return Database.connect(dataSource)
}

fun koinAppModule(db: Database) = module {
    single<TasksRepository> { TasksRepositoryImpl(db) }
    single<UsersRepository> { UsersRepositoryImpl(db) }
    single<UsersService> { UsersServiceImpl(get()) }
    single<TasksService> { TasksServiceImpl(get(), get()) }
}

fun createDatabaseTables(db: Database) {
    transaction(db) {
        SchemaUtils.createMissingTablesAndColumns(UsersRepositoryImpl.UsersTable, TasksRepositoryImpl.TasksTable, TasksRepositoryImpl.TasksRevisionsTable)
    }
}

fun startServer(tasksService: TasksService, usersService: UsersService) {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module(tasksService, usersService)
    }.start(wait = true)
}

fun Application.module(tasksService: TasksService, usersService: UsersService) {
    // Install json content negotiation
    install(ContentNegotiation) {
        json()
    }


    // Convert exceptions thrown from the server side code to the corresponding status code with the error message
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                Exceptions.statusMap[cause::class]
                    ?: HttpStatusCode.InternalServerError,
                ErrorResponse(cause.message ?: "")
            )
        }
    }

    install(Sessions) {
        cookie<UserSession>("SESSION")
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
                        "GET /tasks/{id}/history",
                        "POST /tasks",
                        "PUT /tasks/{id}",
                        "DELETE /tasks/{id}",
                        "GET /users",
                        "GET /users/{id}",
                        "POST /users"
                    )
                    call.respondText(apiList.joinToString("\n"), ContentType.Text.Plain)
                }
            }
            taskRoutes(tasksService, usersService)
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

fun ApplicationCall.getLoggedInUsername(): String {
    val userSession: UserSession? = sessions.get()
    return userSession?.username ?: throw Exceptions.NoLoggedInUserException()
}