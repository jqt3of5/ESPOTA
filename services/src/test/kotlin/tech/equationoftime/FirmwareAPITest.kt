package tech.equationoftime

import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import tech.equationoftime.models.FirmwareMetadata
import tech.equationoftime.plugins.*
import java.io.File
import kotlin.io.path.Path

class FirmwareAPITest {
    @Test
    fun testFirmwareEmpty() = testApplication {

        val firmwareMetadata= mutableListOf<FirmwareMetadata>()

        application {
            configureSerialization()
            configureFirmwareAPI(firmwareMetadata)
        }
        client.get("/firmware").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
    @Test
    fun testFirmwareFilled() = testApplication {

        val firmwareMetadata= mutableListOf<FirmwareMetadata>()

        firmwareMetadata.add(FirmwareMetadata("1234", "name", "1.0.0", "esp"))
        firmwareMetadata.add(FirmwareMetadata("12345", "name", "1.0.1", "esp"))
        firmwareMetadata.add(FirmwareMetadata("12346", "name2", "1.0.1", "esp"))

        application {
            configureSerialization()
            configureFirmwareAPI(firmwareMetadata)
        }
        client.get("/firmware").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<FirmwareMetadata>>(bodyAsText())
            assertEquals(3, list.count())
        }
    }
    @Test
    fun testFirmwareFiltered() = testApplication {

        val firmwareMetadata= mutableListOf<FirmwareMetadata>()

        firmwareMetadata.add(FirmwareMetadata("1234", "name", "1.0.0", "esp"))
        firmwareMetadata.add(FirmwareMetadata("12345", "name", "1.0.1", "esp"))
        firmwareMetadata.add(FirmwareMetadata("12346", "name2", "1.0.1", "esp"))

        application {
            configureSerialization()
            configureFirmwareAPI(firmwareMetadata)
        }
        client.get("/firmware?name=name").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<FirmwareMetadata>>(bodyAsText())
            assertEquals(2, list.count())
        }
        client.get("/firmware?version=1.0.0").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<FirmwareMetadata>>(bodyAsText())
            assertEquals(1, list.count())
        }
        client.get("/firmware?platform=esp").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<FirmwareMetadata>>(bodyAsText())
            assertEquals(3, list.count())
        }
    }

    @Test
    fun testAddNewFirmware() = testApplication {

        val firmwareMetadata= mutableListOf<FirmwareMetadata>()

        application {
            configureSerialization()
            configureFirmwareAPI(firmwareMetadata)
        }

        client.submitFormWithBinaryData("/firmware/esp/name/1.0.0", formData {
            append("description", "testfilename")
            append("file", File("gradlew").readBytes(), Headers.build {
                append(HttpHeaders.ContentType, "application/octet-stream")
                append(HttpHeaders.ContentDisposition, "filename=\"firmware\"")
            })
        })
        .apply {
            assertEquals(HttpStatusCode.OK, status)
            assert(File(Path(firmwareRoot, body<String>()).toString()).exists())
            assertEquals(1, firmwareMetadata.count())
        }
    }
    @Test
    fun testAddExistingFirmware() = testApplication {

        val firmwareMetadata= mutableListOf<FirmwareMetadata>()
        firmwareMetadata.add(FirmwareMetadata("id", "name", "version", "platform"))

        application {
            configureSerialization()
            configureFirmwareAPI(firmwareMetadata)
        }

        client.submitFormWithBinaryData("/firmware/platform/name/version", formData {
            append("description", "testfilename")
            append("file", File("gradlew").readBytes(), Headers.build {
                append(HttpHeaders.ContentType, "application/octet-stream")
                append(HttpHeaders.ContentDisposition, "filename=\"firmware\"")
            })
        })
            .apply {
                assertEquals(HttpStatusCode.BadRequest, status)
                assert(File(Path(firmwareRoot, body<String>()).toString()).exists())
                assertEquals(1, firmwareMetadata.count())
            }
    }
    @Test
    fun testGetNewFirmware() = testApplication {

        val firmwareMetadata= mutableListOf<FirmwareMetadata>()

        application {
            configureSerialization()
            configureFirmwareAPI(firmwareMetadata)
        }

        val uuid = client.submitFormWithBinaryData("/firmware/esp/name/1.0.0", formData {
            append("description", "testfilename")
            append("file", File("gradlew").readBytes(), Headers.build {
                append(HttpHeaders.ContentType, "application/octet-stream")
                append(HttpHeaders.ContentDisposition, "filename=\"firmware\"")
            })
        })
        .apply {
            assertEquals(HttpStatusCode.OK, status)
        }.body<String>()

        client.get("/firmware/esp/name/1.0.0/metadata").apply {
            assertEquals(HttpStatusCode.OK, status)
            val metadata = Json.decodeFromString<FirmwareMetadata>(bodyAsText())
            assertEquals("esp", metadata.platform)
            assertEquals("name", metadata.name)
            assertEquals("1.0.0", metadata.version)
            assertEquals(uuid, metadata.id)
        }
    }
    @Test
    fun testGetNonFirmware() = testApplication {

        val firmwareMetadata= mutableListOf<FirmwareMetadata>()

        application {
            configureSerialization()
            configureFirmwareAPI(firmwareMetadata)
        }

        client.get("/firmware/esp/name/1.0.0/metadata").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }
}