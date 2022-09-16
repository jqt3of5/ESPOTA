package tech.equationoftime.plugins

import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import jdk.incubator.vector.VectorOperators.Binary
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import org.ktorm.expression.BinaryExpression
import org.ktorm.schema.ColumnDeclaring
import tech.equationoftime.models.DeviceMetadataDTO
import tech.equationoftime.services.DeviceMqttService
import tech.equationoftime.tables.*
import java.time.Instant
import java.util.Date

@kotlinx.serialization.Serializable
data class FlashDTO (val firmwareId : String)
@kotlinx.serialization.Serializable
data class wifiDTO (val ssid: String, val psk : String)
@kotlinx.serialization.Serializable
data class mdnsDTO (val name: String)
@kotlinx.serialization.Serializable
data class onlineDTO(val name: String, val ip: String, val ssid : String, val platform : String, val firmwareName : String, val firmwareVersion : String)
fun Application.configureDeviceAPI(service : DeviceMqttService?, firmwareAPIURL : String, database : Database) {

    routing {

        get ("/devices") {
            val platform = call.request.queryParameters["platform"]
            val version = call.request.queryParameters["firmwareVersion"]
            val firmwareName = call.request.queryParameters["firmwareName"]

            val devices = database.devices.toList()

            val metadatas = devices.filter {
                if (platform != null && platform != it.platform)
                {
                    return@filter false
                }
                if (version != null && version != it.firmware.version)
                {
                    return@filter false
                }
                if (firmwareName != null && firmwareName != it.firmware.family.name)
                {
                    return@filter false
                }
                return@filter true
            }
            call.respond(metadatas)
        }
        post ("/devices/{id}") {
            val dto = call.receive<onlineDTO>()
            val id = call.parameters["id"] ?: ""

            var family = database.families.find { it.name eq dto.firmwareName }
            if (family == null) {
                family = FirmwareFamilyEntity {
                    this.name = dto.firmwareName
                }
                database.families.add(family)
            }
            var firmwareEntity = database.firmwares
                .filter { it.familyId eq  family.id}
                .filter { it.version eq dto.firmwareVersion}
                .find {it.platform eq dto.platform}

            if (firmwareEntity == null) {
              firmwareEntity = FirmwareEntity {
                  this.platform = dto.platform
                  this.description = ""
                  this.family = family
                  this.firmwareId = id
                  this.version = dto.firmwareVersion
              }
                database.firmwares.add(firmwareEntity)
            }

            var device = database.devices.find { it.deviceId eq id }

            if (device == null)
            {
                device = DeviceMetadataEntity(){
                    deviceId = id
                    name = dto.name
                    online = true
                    ssid = dto.ssid
                    lastMessage = Instant.now().epochSecond
                    ip = dto.ip
                    platform = dto.platform
                    firmware = firmwareEntity
                }
                database.devices.add(device)
                return@post call.respond(HttpStatusCode.OK)
            }

            device.apply {
                deviceId = id
                name = dto.name
                online = true
                ssid = dto.ssid
                lastMessage = Instant.now().epochSecond
                ip = dto.ip
                platform = dto.platform
                firmware = firmwareEntity
            }

            database.devices.update(device)

            return@post call.respond(HttpStatusCode.OK)
        }

        delete("/devices/{id}") {
            //TODO: Check that device is actually under our control and online
            val id = call.parameters["id"] ?: ""

            database.devices.find { it.deviceId eq id } ?: return@delete call.respond(HttpStatusCode.NotFound)

            database.devices.removeIf { it.deviceId eq id }
            call.respond(HttpStatusCode.OK)
        }

        post ("/devices/{id}/flash") {
            //TODO: Check that device is actually under our control and online
            val dto = call.receive<FlashDTO>()
            //todo: What is the public facing url?
            service?.flash(call.parameters["id"] ?: "", firmwareAPIURL, dto.firmwareId)
            call.respond("")
        }
        post ("/devices/{id}/reboot")
        {
            //TODO: Check that device is actually under our control and online
            service?.reboot(call.parameters["id"] ?: "")
            call.respond("")
        }
        post ("/devices/{id}/wifi")
        {
            //TODO: Check that device is actually under our control and online
            val dto = call.receive<wifiDTO>()
            service?.wifi(call.parameters["id"] ?: "", dto.ssid, dto.psk)
            call.respond("")
        }
        post ("/devices/{id}/mdns")
        {
            //TODO: Check that device is actually under our control and online
            val dto = call.receive<mdnsDTO>()
            service?.mdns(call.parameters["id"] ?: "", dto.name)
            call.respond("")
        }
    }
}
