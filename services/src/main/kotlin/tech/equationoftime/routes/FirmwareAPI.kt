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
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.map
import tech.equationoftime.firmwareRoot
import tech.equationoftime.models.FirmwareMetadataDTO
import tech.equationoftime.tables.FirmwareEntity
import tech.equationoftime.tables.FirmwareFamilyEntity
import tech.equationoftime.tables.families
import tech.equationoftime.tables.firmwares
import java.nio.file.Files
import kotlin.io.path.Path

fun Application.configureFirmwareAPI(database : Database) {

    routing {
        //Get existing firmwares of firmware
        get("/firmwares") {

            val families = database.families.map {it.name}
            call.respond(families)
        }

        post ("/firmwares/{firmware}") {
            val firmwareFamily = call.parameters["firmware"]
            if (firmwareFamily != null)
            {
                database.families.add(FirmwareFamilyEntity {
                    name = firmwareFamily
                })
            }
            call.respond("")
        }

        //get firmware that belongs to this firmware family
        get("/firmware/{family}") {
            val family = call.parameters["family"] as String

            database.families.find {it.name eq family}?.let {familyEntity ->
                val firmwares = database.firmwares.filter {it.familyId eq familyEntity.id}

                call.respond(firmwares.map {
                    FirmwareMetadataDTO(it.firmwareId, it.family.name, it.version, it.platform, it.description)
                })
            }
        }

        //Upload new firmware version
        post("/firmware/{family}/{version}/{platform}") {
            val familyName = call.parameters["familyName"] ?: ""
            val version = call.parameters["version"] ?: ""
            val platform = call.parameters["platform"] ?: ""

            val familyEntity = database.families.find {it.name eq familyName}
            if (familyEntity == null)
            {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            val firmware = database.firmwares
                .filter {it.familyId eq familyEntity.id}
                .filter {it.version eq version}
                .find {it.platform eq platform}

            //Does this firmware already exist?
            if (firmware != null)
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
            database.firmwares.add(FirmwareEntity {
                firmwareId = uuid
                family = familyEntity
                this.version = version
                this.description = description
                this.platform = platform
            })
            call.respond(uuid)
        }

        //Get metadata
        get ("/firmware/{family}/{version}/{platform}") {
            val family = call.parameters["family"] ?: ""
            val version = call.parameters["version"] ?: ""
            val platform = call.parameters["platform"] ?: ""

            val familyEntity = database.families
                .find {it.name eq family }
                ?: return@get call.respond(HttpStatusCode.NotFound)

            val metadata = database.firmwares
                .filter {it.familyId eq familyEntity.id}
                .filter {it.version eq version}
                .find {it.platform eq platform}
                ?: return@get call.respond(HttpStatusCode.NotFound,"firmware not found for $family@$version")

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
