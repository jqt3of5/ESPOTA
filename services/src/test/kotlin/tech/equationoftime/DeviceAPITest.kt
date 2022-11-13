package tech.equationoftime
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.json.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.*
import org.eclipse.paho.client.mqttv3.IMqttClient
import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.MqttMessage
import tech.equationoftime.models.*
import tech.equationoftime.plugins.*
import tech.equationoftime.routes.configureMqttService
import tech.equationoftime.services.*
import tech.equationoftime.tables.*
import kotlin.test.*

class DeviceAPITest {

    val _repo : IDeviceRepo
    get () =  mockk<IDeviceRepo>().also {
        val families = listOf(FirmwareMetadataEntity {
            id = 0
            name = "firmwareFamily1"
        })
        every { it.firmware } returns families

        val firmwares = listOf(FirmwareVersionEntity {
            id = 0
            family = families[0]
            version = "1.0.0"
            platform = "esp"
            description = "description"
        },
            FirmwareVersionEntity {
                id = 1
                family = families[0]
                version = "1.0.1"
                platform = "esp"
                description = "description"
            },
            FirmwareVersionEntity {
                id = 2
                family = families[0]
                version = "1.0.0"
                platform = "esp"
                description = "description"
            })
        every { it.getFirmwareVersions(any())} returns firmwares

        val devices = listOf(DeviceMetadataEntity {
            deviceId = "device1"
            name = "main-device"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.1"
            platform = "esp"
            firmware = firmwares[0]
        },
            DeviceMetadataEntity {
                deviceId = "device2"
                name = "main-device2"
                online = true
                ssid = "ssid"
                lastMessage = 12345678
                ip = "192.168.1.2"
                platform = "pico"
                firmware = firmwares[1]
            },
            DeviceMetadataEntity {
                deviceId = "device3"
                name = "main-device3"
                online = true
                ssid = "ssid"
                lastMessage = 12345678
                ip = "192.168.1.3"
                platform = "esp"
                firmware = firmwares[2]
            })

        every {it.devices } returns devices
        every {it.getDevice("device1")} returns devices[0]
        every {it.deleteDevice("device1")} returns Unit
        every {it.getDevice("device2")} returns devices[1]
        every {it.deleteDevice("device2")} returns Unit
        every {it.getDevice("device3")} returns devices[2]
        every {it.deleteDevice("device3")} returns Unit
        every {it.getDevice("1234")} returns null
        every {it.addOrUpdateDevice(any())} returns Unit
    }

    @Test
    fun testDevices() = testApplication {

        application {
            configureSerialization()
            configureDeviceAPI(_repo)
        }

        client.get("/devices").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<DeviceMetadataDTO>>(bodyAsText())
            assertEquals(3, list.count())
        }
    }

    @Test
    fun testDeviceFilter() = testApplication {

        application {
            configureSerialization()
            configureDeviceAPI(_repo)
        }

        client.get("/devices?firmwareName=firmwareFamily1").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<DeviceMetadataDTO>>(bodyAsText())
            assertEquals(3, list.count())
        }
        client.get("/devices?firmwareVersion=1.0.0").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<DeviceMetadataDTO>>(bodyAsText())
            assertEquals(2, list.count())
        }
        client.get("/devices?platform=esp").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<DeviceMetadataDTO>>(bodyAsText())
            assertEquals(2, list.count())
        }
    }
    @Test
    fun testAddDevice() = testApplication {

        val repo = _repo

        application {
            configureSerialization()
            configureDeviceAPI(repo)
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.post("/devices/1234"){
            header("content-type", "application/json")
            setBody(onlineDTO("name", "ip", "ssid", "platform", "firmwareName", "firmwareVersion"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        val slot = slot<DeviceMetadataEntity>()
        verify { repo.addOrUpdateDevice(capture(slot)) }

        assertEquals("1234", slot.captured.deviceId)
        assertEquals("name", slot.captured.name)
        assertEquals("firmwareName", slot.captured.firmware.family.name)
        assertEquals("firmwareVersion", slot.captured.firmware.version)
    }
    @Test
    fun testDeleteDevice() = testApplication {

        val repo = _repo

        application {
            configureSerialization()
            configureDeviceAPI(repo)
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.delete("/devices/device1"){
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        verify {repo.deleteDevice("device1")}
    }
    @Test
    fun testDeleteNonExistentDevice() = testApplication {

        val repo = _repo

        application {
            configureSerialization()
            configureDeviceAPI(repo)
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.delete("/devices/1234"){
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun testReboot() = testApplication {

        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit
        every { mock.publish(any(), any()) } returns Unit
        every { mock.connect() } returns Unit
        every { mock.subscribe(any(), any<IMqttMessageListener>()) } returns Unit

        val firmwareHttpClient = HttpClient(CIO) {
            defaultRequest {
                url("http://localhost:80")
            }
            install(ContentNegotiation) {
                json()
            }
        }
        application {
            configureSerialization()
            configureDeviceAPI(_repo)
            configureMqttService(firmwareHttpClient, mock, _repo, "http://localhost:80/")
        }

        client.post("/devices/device1/reboot").apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        verify {
            mock.publish("device/device1/reboot", any())
        }
    }

    @Test
    fun testWifi() = testApplication {

        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit
        every { mock.publish(any(), any()) } returns Unit
        every { mock.connect() } returns Unit
        every { mock.subscribe(any(), any<IMqttMessageListener>()) } returns Unit

        val firmwareHttpClient = HttpClient(CIO) {
            defaultRequest {
                url("http://localhost:80")
            }
            install(ContentNegotiation) {
                json()
            }
        }
        application {
            configureSerialization()
            configureDeviceAPI(_repo)
            configureMqttService(firmwareHttpClient, mock, _repo, "http://localhost:80/")
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.post("/devices/device1/wifi"){
            header("content-type", "application/json")
            setBody(wifiDTO("ssid", "psk"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val slot = slot<MqttMessage>()
        verify {
            mock.publish("device/device1/wifi", capture(slot))
        }
        assertEquals("ssid:psk", slot.captured.payload.decodeToString())
    }
    @Test
    fun testFlash() = testApplication {

        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit
        every { mock.publish(any(), any()) } returns Unit
        every { mock.connect() } returns Unit
        every { mock.subscribe(any(), any<IMqttMessageListener>()) } returns Unit

        val firmwareHttpClient = HttpClient(CIO) {
            defaultRequest {
                url("http://localhost:80")
            }
            install(ContentNegotiation) {
                json()
            }
        }
        application {
            configureSerialization()
            configureDeviceAPI(_repo)
            configureMqttService(firmwareHttpClient, mock, _repo, "http://localhost:80")
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        client.post("/devices/device1/flash") {
            header("content-type", "application/json")
            setBody(FlashDTO("1"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val slot = slot<MqttMessage>()
        verify {
            mock.publish("device/device1/flash", capture(slot))
        }
        assertEquals("http://localhost:80/firmware/1", slot.captured.payload.decodeToString())
    }
    @Test
    fun testMdns() = testApplication {

        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit
        every { mock.publish(any(), any()) } returns Unit
        every { mock.connect() } returns Unit
        every { mock.subscribe(any(), any<IMqttMessageListener>()) } returns Unit

        val firmwareHttpClient = HttpClient(CIO) {
            defaultRequest {
                url("http://localhost:80")
            }
            install(ContentNegotiation) {
                json()
            }
        }
        application {
            configureSerialization()
            configureDeviceAPI(_repo)
            configureMqttService(firmwareHttpClient, mock, _repo, "http://localhost:80/")
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.post("/devices/device1/mdns"){
            header("content-type", "application/json")
            setBody(mdnsDTO("newmdnsname"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val slot = slot<MqttMessage>()
        verify {
            mock.publish("device/device1/mdns", capture(slot))
        }
        assertEquals("newmdnsname", slot.captured.payload.decodeToString())
    }

}