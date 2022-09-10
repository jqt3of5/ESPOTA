package tech.equationoftime.plugins

import com.benasher44.uuid.uuid4
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.equationoftime.firmwareRoot
import tech.equationoftime.models.FirmwareMetadata
import java.nio.file.Files
import kotlin.io.path.Path

fun Application.configureFirmwareAPI(families : MutableList<String>, firmwareMetadatas: MutableList<FirmwareMetadata>) {

    routing {
        //Get existing families of firmware
        get("/families") {

            call.respond(families)
        }

        post ("/families/{family}") {
            val family = call.parameters["family"]
            if (family != null)
            {
                families.add(family)
            }
            call.respond("")
        }

        //get firmware that belongs to this family
        get("/firmware/{name}") {
            val name = call.parameters["name"]

            val metadatas = firmwareMetadatas.filter {
                if (name != null && name != it.name)
                {
                    return@filter false
                }
                return@filter true
            }
            call.respond(metadatas)
        }

        //Upload new firmware version
        post("/firmware/{name}/{version}/{platform}") {
            val name = call.parameters["name"] ?: ""
            val version = call.parameters["version"] ?: ""
            val platform = call.parameters["platform"] ?: ""

            //Does this firmware already exist?
            if (firmwareMetadatas.find {
                it.name == name && it.version == version && it.platform == platform
                } != null)
            {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val uuid = uuid4().toString()
            var description = ""
            call.receiveMultipart().forEachPart {
                when(it) {
                    is PartData.FileItem -> {
                        val bytes = it.streamProvider().readBytes()
                        if (!Files.exists(Path(firmwareRoot)))
                        {
                            withContext(Dispatchers.IO) {
                                Files.createDirectory(Path(firmwareRoot))
                            }
                        }
                        val file = java.io.File(Path(firmwareRoot, uuid).toString())
                        file.writeBytes(bytes)
                    }
                    is PartData.FormItem -> {
                        when (it.name)
                        {
                            "description" -> {
                                description = it.value
                            }
                        }
                    }
                    else -> {}
                }
            }

            //TODO: Validation Logic for version numbers/sorting/ids/etc
            firmwareMetadatas.add(FirmwareMetadata(uuid, name, version, platform, description))
            call.respond(uuid)
        }
        //Get metadata
        get ("/firmware/{name}/{version}/{platform}") {
            val name = call.parameters["name"] ?: ""
            val version = call.parameters["version"] ?: ""

            val metadata = firmwareMetadatas.find {
                return@find it.version == version && it.name == name
            } ?: return@get call.respond(HttpStatusCode.NotFound,"firmware not found for $name@$version")

            call.respond(metadata)
        }

        //Download
        get ("/firmware/{firmwareId}") {
            call.parameters["firmwareId"]?.let {firmwareId ->
                val file = java.io.File(Path(firmwareRoot,firmwareId).toString())
                if (!file.exists())
                {
                    return@get call.respond("file not found")
                }
                call.respondFile(file)
            }
            call.respond("One or more path parameters were null")
        }
    }
}
