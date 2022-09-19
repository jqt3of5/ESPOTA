package tech.equationoftime
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
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.ktorm.database.Database
import org.ktorm.entity.add
import org.ktorm.entity.count
import org.ktorm.entity.first
import org.ktorm.entity.toList
import tech.equationoftime.models.*
import tech.equationoftime.plugins.*
import tech.equationoftime.services.*
import tech.equationoftime.tables.*
import java.sql.DriverManager
import kotlin.test.*

class DeviceAPITest {

    @Test
    fun testDevices() = testApplication {
        //An unfortunate little trick to keep this in memory db alive between transactions
        val keepAlive = DriverManager.getConnection("jdbc:sqlite:file:test?mode=memory&cache=shared")
        var database = Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared")
        database.createTable(DeviceTable)
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
            val
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

        keepAlive.close()
    }

    @Test
    fun testDeviceFilter() = testApplication {

        val keepAlive = DriverManager.getConnection("jdbc:sqlite:file:test?mode=memory&cache=shared")
        var database = Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared")
//        var database = Database.connect("jdbc:sqlite:testDeviceFilter.db")
        database.createTable(DeviceTable)
        database.createTable(FirmwareVersionTable)
        database.createTable(FirmwareMetadataTable)
         val family1 = FirmwareFamilyEntity {
             name = "firmwareFamily1"
         }
        database.families.add(family1)

        val firmware1 =  FirmwareEntity {
            firmwareId = "firmware1"
            family = family1
            version = "1.0.0"
            platform = "esp"
            description = "description"
        }
        database.firmwares.add(firmware1)
        val firmware11 = FirmwareEntity {
            firmwareId = "firmware1"
            family = family1
            version = "1.0.1"
            platform = "esp"
            description = "description"
        }
        database.firmwares.add(firmware11)
        val firmware2 =  FirmwareEntity {
            firmwareId = "firmware2"
            family = family1
            version = "1.0.0"
            platform = "esp"
            description = "description"
        }
        database.firmwares.add(firmware2)

        database.devices.add(DeviceMetadataEntity {
            name = "main-device"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.1"
            platform = "esp"
            firmware = firmware11
        })
        database.devices.add(DeviceMetadataEntity {
            name = "main-device2"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.2"
            platform = "pico"
            firmware = firmware1
        })
        database.devices.add(DeviceMetadataEntity {
            name = "main-device2"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.3"
            platform = "esp"
            firmware = firmware2
        })

        val mock = mockk<IMqttClient>()
        every { mock.setCallback(any()) } returns Unit

        val service = DeviceMqttService(mock)
        application {
            configureSerialization()
            configureDeviceAPI(service, "http://localhost", database)
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
        keepAlive.close()
    }

    @Test
    fun testReboot() = testApplication {

        val keepAlive = DriverManager.getConnection("jdbc:sqlite:file:test?mode=memory&cache=shared")
        var database = Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared")
        database.createTable(DeviceTable)
        database.createTable(FirmwareVersionTable)
        database.createTable(FirmwareMetadataTable)
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
        keepAlive.close()
    }
    @Test
    fun testWifi() = testApplication {

        val keepAlive = DriverManager.getConnection("jdbc:sqlite:file:test?mode=memory&cache=shared")
        var database = Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared")
        database.createTable(DeviceTable)
        database.createTable(FirmwareVersionTable)
        database.createTable(FirmwareMetadataTable)
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
        keepAlive.close()
    }
    @Test
    fun testFlash() = testApplication {

        val keepAlive = DriverManager.getConnection("jdbc:sqlite:file:test?mode=memory&cache=shared")
        var database = Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared")
        database.createTable(DeviceTable)
        database.createTable(FirmwareVersionTable)
        database.createTable(FirmwareMetadataTable)
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
        client.post("/devices/1234/flash") {
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
        keepAlive.close()
    }
    @Test
    fun testMdns() = testApplication {

        val keepAlive = DriverManager.getConnection("jdbc:sqlite:file:test?mode=memory&cache=shared")
        var database = Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared")
        database.createTable(DeviceTable)
        database.createTable(FirmwareVersionTable)
        database.createTable(FirmwareMetadataTable)
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
        keepAlive.close()
    }
    @Test
    fun testAddDevice() = testApplication {

        val keepAlive = DriverManager.getConnection("jdbc:sqlite:file:test?mode=memory&cache=shared")
        var database = Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared")
        database.createTable(DeviceTable)
        database.createTable(FirmwareVersionTable)
        database.createTable(FirmwareMetadataTable)

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
        assertEquals("1234", database.devices.toList().first().deviceId)
        keepAlive.close()
    }
    @Test
    fun testAddExistingDevice() = testApplication {

        val keepAlive = DriverManager.getConnection("jdbc:sqlite:file:test?mode=memory&cache=shared")
        var database = Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared")
//        val database = Database.connect("jdbc:sqlite:testAddExistingDevice.db")
        database.createTable(DeviceTable)
        database.createTable(FirmwareVersionTable)
        database.createTable(FirmwareMetadataTable)
        val metadata = FirmwareFamilyEntity {
            name = "family1"
        }
        val firmware = FirmwareEntity {
            firmwareId = "firmware1"
            family = metadata
            version = "1.0.1"
            platform = "esp"
            description = "description"
        }

        val device = DeviceMetadataEntity {
            name = "name1"
            deviceId = "device1"
            online = true
            ssid = "ssid"
            lastMessage = 12345678
            ip = "192.168.1.2"
            platform = "esp"
            this.firmware = firmware
        }
        database.families.add(metadata)
        database.firmwares.add(firmware)
        database.devices.add(device)

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

        client.post("/devices/device1"){
            header("content-type", "application/json")
            setBody(onlineDTO("name2", "ip2", "ssid2", "esp", "family2", "firmware2"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        assertEquals(1, database.devices.count())
        assertEquals("device1", database.devices.first().deviceId)
        assertEquals("name2", database.devices.first().name)
        keepAlive.close()
    }
    @Test
    fun testDeleteDevice() = testApplication {

        val keepAlive = DriverManager.getConnection("jdbc:sqlite:file:test?mode=memory&cache=shared")
        var database = Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared")
        database.createTable(DeviceTable)
        database.createTable(FirmwareMetadataTable)
        database.createTable(FirmwareVersionTable)
        database.devices.add(DeviceMetadataEntity {
            deviceId = "1234"
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
        keepAlive.close()
    }
    @Test
    fun testDeleteNonExistentDevice() = testApplication {

        val keepAlive = DriverManager.getConnection("jdbc:sqlite:file:test?mode=memory&cache=shared")
        var database = Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared")
        database.createTable(DeviceTable)
        database.createTable(FirmwareMetadataTable)
        database.devices.add(DeviceMetadataEntity {
            deviceId = "1234"
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
            assertEquals(HttpStatusCode.NotFound, status)
        }

        assertEquals(1, database.devices.toList().count())
        keepAlive.close()
    }
}