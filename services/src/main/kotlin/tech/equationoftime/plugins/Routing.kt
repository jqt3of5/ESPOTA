package tech.equationoftime.plugins

import com.benasher44.uuid.uuid4
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import tech.equationoftime.FirmwareMetadata
import tech.equationoftime.firmwareMetadata
import tech.equationoftime.firmwareRoot
import kotlin.io.path.Path

fun Application.configureRouting() {

    routing {
        //Get all firmwares
        get("/firmware") {
            val name = this.context.request.queryParameters["name"] 
            val version = this.context.request.queryParameters["version"]
            val platform = this.context.request.queryParameters["platform"]

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
            val name = this.context.parameters["name"] ?: ""
            val version = this.context.parameters["version"] ?: ""
            val platform = this.context.parameters["platform"] ?: ""

            var uuid = uuid4().toString()
            firmwareMetadata.add(FirmwareMetadata(uuid, name, version, platform))

        }
        //Get metadata
        get ("/firmware/{platform}/{name}/{version}/metadata") {
            val name = this.context.parameters["name"] ?: ""
            val version = this.context.parameters["version"] ?: ""

            val metadata = firmwareMetadata.find {
                return@find it.version == version && it.name == name
            } ?: return@get call.respond(HttpStatusCode.NotFound,"firmware not found for $name@$version")

            call.respond(metadata)
        }

        //Download
        get ("/firmware/{platform}/{name}/{version}") {
            this.context.parameters["name"] ?.let {name ->
                this.context.parameters["version"]?.let {version ->
                    this.context.parameters["platform"]?.let {platform ->
                        val file = java.io.File(Path(firmwareRoot, name, version, platform).toString())
                        if (!file.exists())
                        {
                            return@get call.respond("file not found")
                        }
                    }
                }
            }

            return@get call.respond("")
        }

        put ("/firmware/{name}/{version}") {

        }

    }
}
