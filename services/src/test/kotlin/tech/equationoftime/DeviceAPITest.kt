package tech.equationoftime
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.json.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.*
import org.eclipse.paho.client.mqttv3.IMqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.ktorm.database.Database
import org.ktorm.entity.add
import org.ktorm.entity.toList
import tech.equationoftime.models.*
import tech.equationoftime.plugins.*
import tech.equationoftime.services.*
import tech.equationoftime.tables.*
import kotlin.test.*
import kotlin.text.toByteArray

class DeviceAPITest {
    @Test
    fun testDevices() = testApplication {

        var database = Database.connect("jdbc:sqlite::memory:")
        database.devices.add(DeviceMetadataEntity {
            name = "main-device"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.1"
            platform = "esp"
            firmware = FirmwareEntity {
               firmwareId = "firmware1"
               family = FirmwareFamilyEntity {
                   name = "firmwareFamily1"
                }
                version = "1.0.0"
                platform = "esp"
                description = "description"
            }
        })

        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit

        val service = DeviceMqttService(mock)
        application {
            configureSerialization()
            configureDeviceAPI(service, "http://localhost", database)
        }

        client.get("/devices").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<DeviceMetadataDTO>>(bodyAsText())
            assertEquals(1, list.count())
        }
    }

    @Test
    fun testDeviceFilter() = testApplication {

        val deviceMetadatumDTOS = mutableListOf<DeviceMetadataDTO>()
        deviceMetadatumDTOS.add(DeviceMetadataDTO("1236", "main-device3", true, "ssid", 12345678, "192.168.1.3", "nano", "name1", "1.0.1"))

        var database = Database.connect("jdbc:sqlite::memory:")
        database.devices.add(DeviceMetadataEntity {
            name = "main-device"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.1"
            platform = "esp"
            firmware = FirmwareEntity {
                firmwareId = "firmware1"
                family = FirmwareFamilyEntity {
                    name = "firmwareFamily1"
                }
                version = "1.0.0"
                platform = "esp"
                description = "description"
            }
        })
        database.devices.add(DeviceMetadataEntity {
            name = "main-device2"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.2"
            platform = "esp"
            firmware = FirmwareEntity {
                firmwareId = "firmware1"
                family = FirmwareFamilyEntity {
                    name = "firmwareFamily1"
                }
                version = "1.0.1"
                platform = "esp"
                description = "description"
            }
        })
        database.devices.add(DeviceMetadataEntity {
            name = "main-device2"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.3"
            platform = "esp"
            firmware = FirmwareEntity {
                firmwareId = "firmware2"
                family = FirmwareFamilyEntity {
                    name = "firmwareFamily1"
                }
                version = "1.0.0"
                platform = "esp"
                description = "description"
            }
        })

        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit

        val service = DeviceMqttService(mock)
        application {
            configureSerialization()
            configureDeviceAPI(service, "http://localhost", database)
        }

        client.get("/devices?firmwareName=name").apply {
            assertEquals(HttpStatusCode.OK, status)
            val list = Json.decodeFromString<List<DeviceMetadataDTO>>(bodyAsText())
            assertEquals(1, list.count())
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
    fun testReboot() = testApplication {

        var database = Database.connect("jdbc:sqlite::memory:")
        database.devices.add(DeviceMetadataEntity {
            name = "main-device2"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.2"
            platform = "esp"
            firmware = FirmwareEntity {
                firmwareId = "firmware1"
                family = FirmwareFamilyEntity {
                    name = "firmwareFamily1"
                }
                version = "1.0.1"
                platform = "esp"
                description = "description"
            }
        })

        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit
        every { mock.publish(any(), any()) } returns Unit


        val service = DeviceMqttService(mock)
        application {
            configureSerialization()
            configureDeviceAPI(service, "http://localhost", database)
        }

        client.post("/devices/1234/reboot").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        verify {
            mock.publish("device/1234/reboot", any())
        }
    }
    @Test
    fun testWifi() = testApplication {

        var database = Database.connect("jdbc:sqlite::memory:")
        database.devices.add(DeviceMetadataEntity {
            name = "main-device2"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.2"
            platform = "esp"
            firmware = FirmwareEntity {
                firmwareId = "firmware1"
                family = FirmwareFamilyEntity {
                    name = "firmwareFamily1"
                }
                version = "1.0.1"
                platform = "esp"
                description = "description"
            }
        })

        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit
        every { mock.publish(any(), any()) } returns Unit


        val service = DeviceMqttService(mock)
        application {
            configureSerialization()
            configureDeviceAPI(service, "http://localhost", database)
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.post("/devices/1234/wifi"){
            header("content-type", "application/json")
            setBody(wifiDTO("ssid", "psk"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val slot = slot<MqttMessage>()
        verify {
            mock.publish("device/1234/wifi", capture(slot))
        }
        assertEquals("ssid:psk", slot.captured.payload.decodeToString())
    }
    @Test
    fun testFlash() = testApplication {

        var database = Database.connect("jdbc:sqlite::memory:")
        database.devices.add(DeviceMetadataEntity {
            name = "main-device2"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.2"
            platform = "esp"
            firmware = FirmwareEntity {
                firmwareId = "firmware1"
                family = FirmwareFamilyEntity {
                    name = "firmwareFamily1"
                }
                version = "1.0.1"
                platform = "esp"
                description = "description"
            }
        })
        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit
        every { mock.publish(any(), any()) } returns Unit

        val service = DeviceMqttService(mock)
        application {
            configureSerialization()
            configureDeviceAPI(service, "http://localhost", database)
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        client.post("/devices/1234/flash"){
            header("content-type", "application/json")
            setBody(FlashDTO("1"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val slot = slot<MqttMessage>()
        verify {
            mock.publish("device/1234/flash", capture(slot))
        }
        assertEquals("http://localhost/firmware/1", slot.captured.payload.decodeToString())
    }
    @Test
    fun testMdns() = testApplication {

        var database = Database.connect("jdbc:sqlite::memory:")
        database.devices.add(DeviceMetadataEntity {
            name = "main-device2"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.2"
            platform = "esp"
            firmware = FirmwareEntity {
                firmwareId = "firmware1"
                family = FirmwareFamilyEntity {
                    name = "firmwareFamily1"
                }
                version = "1.0.1"
                platform = "esp"
                description = "description"
            }
        })
        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit
        every { mock.publish(any(), any()) } returns Unit

        val service = DeviceMqttService(mock)
        application {
            configureSerialization()
            configureDeviceAPI(service, "http://localhost", database)
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.post("/devices/1234/mdns"){
            header("content-type", "application/json")
            setBody(mdnsDTO("newmdnsname"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val slot = slot<MqttMessage>()
        verify {
            mock.publish("device/1234/mdns", capture(slot))
        }
        assertEquals("newmdnsname", slot.captured.payload.decodeToString())
    }
    @Test
    fun testAddDevice() = testApplication {

        var database = Database.connect("jdbc:sqlite::memory:")

        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit
        every { mock.publish(any(), any()) } returns Unit

        val service = DeviceMqttService(mock)
        application {
            configureSerialization()
            configureDeviceAPI(service, "http://localhost", database)
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

        assertEquals(1, database.devices.toList().count())
        assertEquals("1234", database.devices.toList().first().id)
    }
    @Test
    fun testAddExistingDevice() = testApplication {

        val deviceMetadatumDTOS = mutableListOf<DeviceMetadataDTO>()

        var database = Database.connect("jdbc:sqlite::memory:")
        database.devices.add(DeviceMetadataEntity {
            name = "main-device2"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.2"
            platform = "esp"
            firmware = FirmwareEntity {
                firmwareId = "firmware1"
                family = FirmwareFamilyEntity {
                    name = "firmwareFamily1"
                }
                version = "1.0.1"
                platform = "esp"
                description = "description"
            }
        })

        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit
        every { mock.publish(any(), any()) } returns Unit

        val service = DeviceMqttService(mock)
        application {
            configureSerialization()
            configureDeviceAPI(service, "http://localhost", database)
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.post("/devices/1234"){
            header("content-type", "application/json")
            setBody(onlineDTO("name2", "ip2", "ssid2", "platform2", "firmwareName2", "firmwareVersion2"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        assertEquals(1, deviceMetadatumDTOS.count())
        assertEquals("1234", deviceMetadatumDTOS.first().id)
        assertEquals("name2", deviceMetadatumDTOS.first().name)
    }
    @Test
    fun testDeleteDevice() = testApplication {

        var database = Database.connect("jdbc:sqlite::memory:")
        database.devices.add(DeviceMetadataEntity {
            name = "main-device2"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.2"
            platform = "esp"
            firmware = FirmwareEntity {
                firmwareId = "firmware1"
                family = FirmwareFamilyEntity {
                    name = "firmwareFamily1"
                }
                version = "1.0.1"
                platform = "esp"
                description = "description"
            }
        })

        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit
        every { mock.publish(any(), any()) } returns Unit

        val service = DeviceMqttService(mock)
        application {
            configureSerialization()
            configureDeviceAPI(service, "http://localhost", database)
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.delete("/devices/1234"){
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        assertEquals(0, database.devices.toList().count())
    }
    @Test
    fun testDeleteNonExistantDevice() = testApplication {

        var database = Database.connect("jdbc:sqlite::memory:")
        database.devices.add(DeviceMetadataEntity {
            name = "main-device2"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.2"
            platform = "esp"
            firmware = FirmwareEntity {
                firmwareId = "firmware1"
                family = FirmwareFamilyEntity {
                    name = "firmwareFamily1"
                }
                version = "1.0.1"
                platform = "esp"
                description = "description"
            }
        })
        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit
        every { mock.publish(any(), any()) } returns Unit

        val service = DeviceMqttService(mock)
        application {
            configureSerialization()
            configureDeviceAPI(service, "http://localhost", database)
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.delete("/devices/absdef"){
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }

        assertEquals(1, database.devices.toList().count())
    }
}