package tech.equationoftime.plugins

import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import tech.equationoftime.models.DeviceMetadata
import tech.equationoftime.services.DeviceMqttService
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
fun Application.configureDeviceAPI(service : DeviceMqttService?, firmwareAPIURL : String, deviceMetadatas : MutableList<DeviceMetadata>) {

    routing {

        get ("/devices") {
            val platform = call.request.queryParameters["platform"]
            val version = call.request.queryParameters["firmwareVersion"]
            val firmwareName = call.request.queryParameters["firmwareName"]

            val metadatas = deviceMetadatas.filter {
                if (platform != null && platform != it.platform)
                {
                    return@filter false
                }
                if (version != null && version != it.firmwareVersion)
                {
                    return@filter false
                }
                if (firmwareName != null && firmwareName != it.firmwareName)
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
            //see if we already have it
            val metadata = deviceMetadatas.find {
                it.id == id
            //if we do, remove it from the list
            }?.also {
               deviceMetadatas.remove(it)
            //if we do, copy it and update values
            }?.copy(
                name = dto.name,
                online = true,
                ssid = dto.ssid,
                lastMessage = Instant.now().epochSecond,
                ip = dto.ip,
                platform = dto.platform,
                firmwareName = dto.firmwareName,
                firmwareVersion = dto.firmwareVersion
            //if we don't, create a new one
            ) ?: DeviceMetadata(
                id,
                dto.name,
                true,
                dto.ssid,
                Instant.now().epochSecond,
                dto.ip,
                dto.platform,
                dto.firmwareName,
                dto.firmwareVersion
            )

            //add or update
            deviceMetadatas.add(metadata)
            call.respond("")
        }

        delete("/devices/{id}") {
            //TODO: Check that device is actually under our control and online
            val id = call.parameters["id"] ?: ""
            if (!deviceMetadatas.removeIf {
                it.id == id
            }) {
                call.respond(HttpStatusCode.NotFound)
            }
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
