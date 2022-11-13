package tech.equationoftime

import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import tech.equationoftime.models.FirmwareFamilyMetadataDTO
import tech.equationoftime.models.FirmwareVersionMetadataDTO
import tech.equationoftime.plugins.*
import tech.equationoftime.tables.*
import java.io.File
import kotlin.io.path.Path

class FirmwareAPITest {
    val _repo : IDeviceRepo
        get () = mockk<IDeviceRepo>().also {
            val families = listOf(FirmwareMetadataEntity {
                id = 0
                name = "firmwareFamily1"
            })
            every { it.firmware } returns families
            every { it.getFamily("firmwareFamily1") } returns families[0]
            every { it.getFamily("nonfirmware") } returns null

            val firmwares = listOf(FirmwareVersionEntity {
                id = 0
                firmwareId = "a"
                family = families[0]
                version = "1.0.0"
                platform = "esp"
                description = "description"
            },
            FirmwareVersionEntity {
                id = 1
                firmwareId = "b"
                family = families[0]
                version = "1.0.1"
                platform = "esp"
                description = "description"
            },
            FirmwareVersionEntity {
                id = 2
                firmwareId = "c"
                family = families[0]
                version = "1.0.2"
                platform = "esp"
                description = "description"
            })
            every { it.getFirmware("firmwareFamily1", "1.0.0", "esp") } returns firmwares[0]
            every { it.getFirmwareVersions(any()) } returns firmwares
            every { it.addOrUpdateFirmwareVersion(any()) } returns Unit
        }
    @Test
    fun testGetFirmwares() = testApplication {

        application {
            configureSerialization()
            configureFirmwareAPI(_repo)
        }
        client.get("/firmwares").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<FirmwareFamilyMetadataDTO>>(bodyAsText())
            assertEquals(1, list.count())
        }
    }

    @Test
    fun testAddNewFirmware() = testApplication {

        val repo = _repo

        application {
            configureSerialization()
            configureFirmwareAPI(repo)
        }

        client.submitFormWithBinaryData("/firmware/firmwareFamily1/2.0.0/esp", formData {
            append("description", "testfilename")
            append("file", File("gradlew").readBytes(), Headers.build {
                append(HttpHeaders.ContentType, "application/octet-stream")
                append(HttpHeaders.ContentDisposition, "filename=\"firmware\"")
            })
        })
        .apply {
            assertEquals(HttpStatusCode.OK, status)
            assert(File(Path(firmwareRoot, body<String>()).toString()).exists())

            val slot = slot<FirmwareVersionEntity>()
            verify { repo.addOrUpdateFirmwareVersion(capture(slot))}

            assertEquals("2.0.0", slot.captured.version)
            assertEquals("esp", slot.captured.platform)
            assertEquals("testfilename", slot.captured.description)
            assertEquals("firmwareFamily1", slot.captured.family.name)
            assertEquals(body<String>(), slot.captured.firmwareId)
        }
    }

    @Test
    fun testGetSpcificFirmware() = testApplication {

        application {
            configureSerialization()
            configureFirmwareAPI(_repo)
        }

        client.get("/firmware/firmwareFamily1/1.0.0/esp").apply {
            assertEquals(HttpStatusCode.OK, status)
            val metadata = Json.decodeFromString<FirmwareVersionMetadataDTO>(bodyAsText())
            assertEquals("esp", metadata.platform)
            assertEquals("firmwareFamily1", metadata.name)
            assertEquals("1.0.0", metadata.version)
            assertEquals("a", metadata.id)
        }
    }
    @Test
    fun testGetNonFirmware() = testApplication {

        application {
            configureSerialization()
            configureFirmwareAPI(_repo)
        }

        client.get("/firmware/nonfirmware/1.0.0/esp").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }
}