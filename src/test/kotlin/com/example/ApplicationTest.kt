package com.example

import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.ktor.client.*

class ApplicationTest {



    @Test
    fun `test get empty list of tasks`() = testApplication {
        application{
            module()
        }
        val response = client.get("/tasks")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `test get list of tasks`() = testApplication {
        application{
            module()
        }
        val response = client.get("/tasks")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }
}
