package tech.equationoftime.plugins

import com.benasher44.uuid.uuid4
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import tech.equationoftime.models.FirmwareMetadata
import tech.equationoftime.firmwareMetadata
import tech.equationoftime.firmwareRoot
import java.io.File
import kotlin.io.path.Path

fun Application.configureFirmwareAPI() {

    routing {
        //Get all firmwares
        get("/firmware") {
            val name = call.request.queryParameters["name"]
            val version = call.request.queryParameters["version"]
            val platform = call.request.queryParameters["platform"]

            val metadatas = firmwareMetadata.filter {
                if (name != null && name != it.name)
                {
                    return@filter false
                }
                if (version != null && version != it.version)
                {
                    return@filter false
                }
                if (platform != null && platform != it.platform)
                {
                    return@filter false
                }
                return@filter true
            }
            call.respond(metadatas)
        }
        
        //Upload new firmware version
        post("/firmware/{platform}/{name}/{version}") {
            val name = call.parameters["name"] ?: ""
            val version = call.parameters["version"] ?: ""
            val platform = call.parameters["platform"] ?: ""

            val uuid = uuid4().toString()
            firmwareMetadata.add(FirmwareMetadata(uuid, name, version, platform))

            call.receiveMultipart().forEachPart {
                when(it) {
                    is PartData.FileItem -> {
                        val bytes = it.streamProvider().readBytes()
                        File("$firmwareRoot/$uuid").writeBytes(bytes)
                    }
                    else -> {}
                }
            }

            call.respond("")
        }
        //Get metadata
        get ("/firmware/{platform}/{name}/{version}/metadata") {
            val name = call.parameters["name"] ?: ""
            val version = call.parameters["version"] ?: ""

            val metadata = firmwareMetadata.find {
                return@find it.version == version && it.name == name
            } ?: return@get call.respond(HttpStatusCode.NotFound,"firmware not found for $name@$version")

            call.respond(metadata)
        }

        //Download
        get ("/firmware/{platform}/{name}/{version}") {
            call.parameters["name"] ?.let {name ->
                call.parameters["version"]?.let {version ->
                    call.parameters["platform"]?.let {platform ->
                        val file = java.io.File(Path(firmwareRoot, name, version, platform).toString())
                        if (!file.exists())
                        {
                            return@get call.respond("file not found")
                        }
                        call.respondFile(file)
                    }
                }
            }
            return@get call.respond("One or more path parameters were null")
        }
    }
}
