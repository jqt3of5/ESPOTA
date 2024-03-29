package tech.equationoftime.routes

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import tech.equationoftime.plugins.onlineDTO
import tech.equationoftime.services.DeviceMqttService
import kotlinx.serialization.*
import org.eclipse.paho.client.mqttv3.IMqttClient
import tech.equationoftime.plugins.FlashDTO
import tech.equationoftime.plugins.mdnsDTO
import tech.equationoftime.plugins.wifiDTO
import tech.equationoftime.tables.IDeviceRepo

var mqttService : DeviceMqttService? = null

fun Application.configureMqttService(http: HttpClient, mqtt: IMqttClient, repo : IDeviceRepo, deviceAPIHost : String) : DeviceMqttService {
    //we're going to receive mqtt messages from the devices for online/offline statuses. listen for them, and then post them to the other microservice
    //TODO: Authentication of any kind?
    mqttService = DeviceMqttService(mqtt) {
       deviceHello() {topic, message ->

            try {
                val payload = message.payload.decodeToString()
                val dto = Json.decodeFromString<onlineDTO>(payload)
                http.post("${deviceAPIHost}/devices/${message.pathParams["deviceId"]}") {
                    header("content-type", "application/json")
                    setBody(dto)
                }
            }catch (e : Exception) {
               println(e)
            }
        }
        deviceOnline() { topic, message ->
            //TODO: What if this returns error code?
            //TODO: Should this accept a body to update metadata?
            http.post("${deviceAPIHost}/devices/${message.pathParams["deviceId"]}/online")
        }
        deviceOffline(){ topic, message ->
            http.post("${deviceAPIHost}/devices/${message.pathParams["deviceId"]}/offline")
        }
        deviceUpdating(){ topic, message ->
            http.post("${deviceAPIHost}/devices/${message.pathParams["deviceId"]}/updating")
        }
    }

    routing {
        post("/devices/{id}/flash") {
            val dto = call.receive<FlashDTO>()
            val id = call.parameters["id"] ?: ""

            repo.getDevice(id)?.let {
                mqttService?.flash(call.parameters["id"] ?: "", "$deviceAPIHost/firmware/$dto.firmwareId")
            } ?: return@post call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK)
        }
        post("/devices/{id}/reboot") {
            val id = call.parameters["id"] ?: ""

            repo.getDevice(id)?.let {
                mqttService?.reboot(call.parameters["id"] ?: "")
            } ?: return@post call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK)
        }

        post("/devices/{id}/wifi") {
            val dto = call.receive<wifiDTO>()
            val id = call.parameters["id"] ?: ""

            repo.getDevice(id)?.let {
                mqttService?.wifi(call.parameters["id"] ?: "", dto.ssid, dto.psk)
            } ?: return@post call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK)
        }
        post("/devices/{id}/mdns") {
            val dto = call.receive<mdnsDTO>()
            val id = call.parameters["id"] ?: ""

            repo.getDevice(id)?.let {
                mqttService?.mdns(call.parameters["id"] ?: "", dto.name)
            } ?: return@post call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK)
        }
    }

    mqttService?.connect()
    return mqttService as DeviceMqttService
}