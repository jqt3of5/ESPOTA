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
import tech.equationoftime.models.FirmwareFamilyMetadataDTO
import tech.equationoftime.models.FirmwareVersionMetadataDTO
import tech.equationoftime.tables.*
import java.nio.file.Files
import kotlin.io.path.Path

fun Application.configureFirmwareAPI(repo : IDeviceRepo, firmwareRoot : String) {

    routing {
        //Get existing firmwares of firmware
        get("/firmwares") {

            val families = repo.firmware.map {FirmwareFamilyMetadataDTO(it.name)}
            call.respond(families)
        }

        post ("/firmware/{firmware}") {
            val firmwareFamily = call.parameters["firmware"]
            if (firmwareFamily != null)
            {
                repo.addOrUpdateFirmwareFamily(FirmwareMetadataEntity {
                   name = firmwareFamily
                })
            }
            call.respond(HttpStatusCode.OK)
        }

        //get firmware that belongs to this firmware family
        get("/firmware/{family}") {
            val family = call.parameters["family"] as String

            repo.getFirmwareVersions(family)?.let {
                call.respond(it.map {
                    FirmwareVersionMetadataDTO(it.id.toString(), it.family.name, it.version, it.platform, it.description)
                })
            }
        }

        //Upload new firmware version
        post("/firmware/{family}/{version}/{platform}") {
            val familyName = call.parameters["family"] ?: ""
            val version = call.parameters["version"] ?: ""
            val platform = call.parameters["platform"] ?: ""

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

            val firmware = FirmwareVersionEntity{
                this.firmwareId = uuid
                this.version = version
                this.platform = platform
                this.description = description
                this.family = FirmwareMetadataEntity {
                    this.name = familyName
                }
            }
            repo.addOrUpdateFirmwareVersion(firmware)

            call.respond(firmware.firmwareId)
        }

        //Get metadata
        get ("/firmware/{family}/{version}/{platform}") {
            val family = call.parameters["family"] ?: ""
            val version = call.parameters["version"] ?: ""
            val platform = call.parameters["platform"] ?: ""

            repo.getFamily(family) ?: return@get call.respond(HttpStatusCode.NotFound)

            val metadata = repo.getFirmware(family, version, platform)
                ?: return@get call.respond(HttpStatusCode.NotFound,"firmware not found for $family@$version")

            call.respond(FirmwareVersionMetadataDTO(metadata.firmwareId, family, metadata.version, metadata.platform, metadata.description))
        }

        //Download
        get ("/firmware/{firmwareId}") {
            call.parameters["firmwareId"]?.let {

                repo.getFirmware(it)?.let {
                    val file = java.io.File(Path(firmwareRoot,it.firmwareId).toString())
                    if (!file.exists())
                    {
                        return@get call.respond("file not found")
                    }
                    return@get call.respondFile(file)
                }
            }

            return@get call.respond("One or more path parameters were null")
        }
    }
}
