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
fun Application.configureDeviceAPI(service : DeviceMqttService?, firmwareAPIURL : String, repo : IDeviceRepo) {

    routing {

        get ("/devices") {
            val platform = call.request.queryParameters["platform"]
            val version = call.request.queryParameters["firmwareVersion"]
            val firmwareName = call.request.queryParameters["firmwareName"]

            val metadatas = repo.devices.filter {
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
            }.map {
                DeviceMetadataDTO(it.deviceId, it.name, it.online, it.ssid, it.lastMessage, it.ip, it.platform, it.firmware.family.name, it.firmware.version)
            }
            call.respond(metadatas)
        }
        get ("/devices/{id}"){
            val id = call.parameters["id"] ?: ""

            repo.getDevice(id)?.let {
                call.respond(DeviceMetadataDTO(it.deviceId, it.name, it.online, it.ssid, it.lastMessage, it.ip, it.platform, it.firmware.family.name, it.firmware.version))
            } ?: call.respond(HttpStatusCode.NotFound)
        }
        post ("/devices/{id}") {
            val dto = call.receive<onlineDTO>()
            val id = call.parameters["id"] ?: ""

            val familyEntity = FirmwareFamilyEntity {
                name = dto.firmwareName
            }

            val firmwareEntity = FirmwareEntity{
                version = dto.firmwareVersion
                family = familyEntity
                platform = dto.platform
            }

            val deviceEntity = DeviceMetadataEntity(){
                deviceId = id
                name = dto.name
                online = true
                ssid = dto.ssid
                lastMessage = Instant.now().epochSecond
                ip = dto.ip
                platform = dto.platform
                firmware = firmwareEntity
            }

            repo.addOrUpdateDevice(deviceEntity)

            return@post call.respond(HttpStatusCode.OK)
        }

        delete("/devices/{id}") {
            val id = call.parameters["id"] ?: ""

            repo.getDevice(id)?.let {
                repo.deleteDevice(id)
            } ?: return@delete call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK)
        }

        post ("/devices/{id}/flash") {
            val dto = call.receive<FlashDTO>()
            val id = call.parameters["id"] ?: ""

            repo.getDevice(id)?.let {
                //todo: What is the public facing url?
                service?.flash(call.parameters["id"] ?: "", firmwareAPIURL, dto.firmwareId)
            } ?: return@post call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK)
        }
        post ("/devices/{id}/reboot") {
            val id = call.parameters["id"] ?: ""

            repo.getDevice(id)?.let {
                service?.reboot(call.parameters["id"] ?: "")
            } ?: return@post call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK)
        }

        post ("/devices/{id}/wifi") {
            val dto = call.receive<wifiDTO>()
            val id = call.parameters["id"] ?: ""

            repo.getDevice(id)?.let {
                service?.wifi(call.parameters["id"] ?: "", dto.ssid, dto.psk)
            } ?: return@post call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK)
        }
        post ("/devices/{id}/mdns") {
            val dto = call.receive<mdnsDTO>()
            val id = call.parameters["id"] ?: ""

            repo.getDevice(id)?.let {
                service?.mdns(call.parameters["id"] ?: "", dto.name)
            } ?: return@post call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK)
        }
    }
}
