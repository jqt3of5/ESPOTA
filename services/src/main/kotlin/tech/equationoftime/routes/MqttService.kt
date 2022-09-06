package tech.equationoftime.routes

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.MqttClient
import tech.equationoftime.plugins.onlineDTO
import tech.equationoftime.services.DeviceMqttService
import kotlinx.serialization.*
import org.eclipse.paho.client.mqttv3.IMqttClient

var mqttService : DeviceMqttService? = null

fun Application.configureMqttService(http: HttpClient, mqtt: IMqttClient) : DeviceMqttService {
    //we're going to receive mqtt messages from the devices for online/offline statuses. listen for them, and then post them to the other microservice
    mqttService = DeviceMqttService(mqtt) {
        filter("manager/{deviceId}/online") {topic, message ->

            try {
                val payload = message.payload.decodeToString()
                val dto = Json.decodeFromString<onlineDTO>(payload)
                http.post("/devices/${message.pathParams["deviceId"]}") {
                    header("content-type", "application/json")
                    setBody(dto)
                }
            }catch (e : Exception) {
               println(e)
                return@filter
            }
        }
        filter("manager/{deviceId}/offline") { topic, message ->
            http.delete("/devices/${message.pathParams["deviceId"]}")
        }
    }

    mqttService?.connect()
    return mqttService as DeviceMqttService
}