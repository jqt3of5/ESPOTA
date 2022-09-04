package tech.equationoftime.plugins

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import tech.equationoftime.deviceMetadata

fun Application.configureDeviceAPI() {

    routing {

        get ("/devices") {
            val platform = call.request.queryParameters["platform"]
            val version = call.request.queryParameters["firmwareVersion"]
            val firmwareName = call.request.queryParameters["firmwareName"]

            val metadatas = deviceMetadata.filter {
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

        post ("/devices/{id}/flash")
        {

        }
        post ("/devices/{id}/reboot")
        {

        }
        post ("/devices/{id}/wifi")
        {

        }
        post ("/devices/{id}/mdns")
        {

        }
    }
}
