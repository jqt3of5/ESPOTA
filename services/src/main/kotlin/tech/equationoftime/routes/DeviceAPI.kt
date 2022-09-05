package tech.equationoftime.plugins

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import tech.equationoftime.models.DeviceMetadata
import tech.equationoftime.services.DeviceMqttService

@kotlinx.serialization.Serializable
data class FlashDTO (val firmwareId : String)
@kotlinx.serialization.Serializable
data class wifiDTO (val ssid: String, val psk : String)
@kotlinx.serialization.Serializable
data class mdnsDTO (val name: String)

fun Application.configureDeviceAPI(service : DeviceMqttService, deviceMetadatas : List<DeviceMetadata>) {

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

        post ("/devices/{id}/flash") {
            //TODO: Check that device is actually under our control and online
            val dto = call.receive<FlashDTO>()
            service.flash(call.parameters["id"] ?: "", dto.firmwareId)
            call.respond("")
        }
        post ("/devices/{id}/reboot")
        {
            //TODO: Check that device is actually under our control and online
            service.reboot(call.parameters["id"] ?: "")
            call.respond("")
        }
        post ("/devices/{id}/wifi")
        {
            //TODO: Check that device is actually under our control and online
            val dto = call.receive<wifiDTO>()
            service.wifi(call.parameters["id"] ?: "", dto.ssid, dto.psk)
            call.respond("")
        }
        post ("/devices/{id}/mdns")
        {
            //TODO: Check that device is actually under our control and online
            val dto = call.receive<mdnsDTO>()
            service.mdns(call.parameters["id"] ?: "", dto.name)
            call.respond("")
        }
    }
}
