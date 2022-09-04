package tech.equationoftime

import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*
import tech.equationoftime.plugins.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureFirmwareAPI()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }
}