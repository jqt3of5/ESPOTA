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
import org.ktorm.database.Database
import org.ktorm.entity.add
import org.ktorm.entity.toList
import tech.equationoftime.models.FirmwareMetadataDTO
import tech.equationoftime.plugins.*
import tech.equationoftime.tables.*
import java.io.File
import kotlin.io.path.Path

class FirmwareAPITest {
    @Test
    fun testFirmwareEmpty() = testApplication {

        var database = Database.connect("jdbc:sqlite::memory:")
        application {
            configureSerialization()
            configureFirmwareAPI(database)
        }
        client.get("/firmware").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
    @Test
    fun testFirmwareFilled() = testApplication {

        val database = Database.connect("jdbc:sqlite::memory:")
        val familyEntity = FirmwareFamilyEntity {
            name = "family1"
        }
        database.families.add(familyEntity)
        database.firmwares.add(FirmwareEntity {
            firmwareId = "1234"
            family = familyEntity
            version = "1.0.0"
            platform = "esp"
            description = "description"
        })
        database.firmwares.add(FirmwareEntity {
            firmwareId = "12345"
            family = familyEntity
            version = "1.0.2"
            platform = "esp"
            description = "description"
        })
        database.firmwares.add(FirmwareEntity {
            firmwareId = "123456"
            family = familyEntity
            version = "1.0.1"
            platform = "esp"
            description = "description"
        })

        application {
            configureSerialization()
            configureFirmwareAPI(database)
        }
        client.get("/firmware").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<FirmwareMetadataDTO>>(bodyAsText())
            assertEquals(3, list.count())
        }
    }
    @Test
    fun testFirmwareFiltered() = testApplication {

        val database = Database.connect("jdbc:sqlite::memory:")
        val familyEntity = FirmwareFamilyEntity {
            name = "family1"
        }
        database.families.add(familyEntity)
        database.firmwares.add(FirmwareEntity {
            firmwareId = "1234"
            family = familyEntity
            version = "1.0.0"
            platform = "esp"
            description = "description"
        })
        database.firmwares.add(FirmwareEntity {
            firmwareId = "12345"
            family = familyEntity
            version = "1.0.2"
            platform = "esp"
            description = "description"
        })
        database.firmwares.add(FirmwareEntity {
            firmwareId = "123456"
            family = familyEntity
            version = "1.0.1"
            platform = "esp"
            description = "description"
        })

        application {
            configureSerialization()
            configureFirmwareAPI(database)
        }
        client.get("/firmware?name=family1").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<FirmwareMetadataDTO>>(bodyAsText())
            assertEquals(2, list.count())
        }
        client.get("/firmware?version=1.0.0").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<FirmwareMetadataDTO>>(bodyAsText())
            assertEquals(1, list.count())
        }
        client.get("/firmware?platform=esp").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<FirmwareMetadataDTO>>(bodyAsText())
            assertEquals(3, list.count())
        }
    }

    @Test
    fun testAddNewFirmware() = testApplication {

        val database = Database.connect("jdbc:sqlite::memory:")
        val familyEntity = FirmwareFamilyEntity {
            name = "family1"
        }
        database.families.add(familyEntity)

        application {
            configureSerialization()
            configureFirmwareAPI(database)
        }

        client.submitFormWithBinaryData("/firmware/esp/family1/1.0.0", formData {
            append("description", "testfilename")
            append("file", File("gradlew").readBytes(), Headers.build {
                append(HttpHeaders.ContentType, "application/octet-stream")
                append(HttpHeaders.ContentDisposition, "filename=\"firmware\"")
            })
        })
        .apply {
            assertEquals(HttpStatusCode.OK, status)
            assert(File(Path(firmwareRoot, body<String>()).toString()).exists())
            assertEquals(1, database.firmwares.toList().count())
        }
    }
    @Test
    fun testAddExistingFirmware() = testApplication {

        val database = Database.connect("jdbc:sqlite::memory:")

        val familyEntity = FirmwareFamilyEntity {
            name = "name"
        }
        database.families.add(familyEntity)
        database.firmwares.add(FirmwareEntity {
            firmwareId = "1234"
            family = familyEntity
            version = "version"
            platform = "platform"
            description = "description"
        })
        application {
            configureSerialization()
            configureFirmwareAPI(database)
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
                assertEquals(1, database.firmwares.toList().count())
            }
    }
    @Test
    fun testGetNewFirmware() = testApplication {

        val database = Database.connect("jdbc:sqlite::memory:")

        application {
            configureSerialization()
            configureFirmwareAPI(database)
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
            val metadata = Json.decodeFromString<FirmwareMetadataDTO>(bodyAsText())
            assertEquals("esp", metadata.platform)
            assertEquals("name", metadata.name)
            assertEquals("1.0.0", metadata.version)
            assertEquals(uuid, metadata.id)
        }
    }
    @Test
    fun testGetNonFirmware() = testApplication {

        val database = Database.connect("jdbc:sqlite::memory:")
        application {
            configureSerialization()
            configureFirmwareAPI(database)
        }

        client.get("/firmware/esp/name/1.0.0/metadata").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }
}