package tech.equationoftime

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.eclipse.paho.client.mqttv3.MqttClient
import org.ktorm.database.Database
import tech.equationoftime.plugins.*
import tech.equationoftime.routes.configureMqttService

val firmwareRoot = "firmwares"

fun main() {

    var database = Database.connect("jdbc:sqlite:main.db")

    val client = MqttClient("tcp://tiltpi.equationoftime.tech:1883","esp-ota-manager")
    val firmwareHttpClient = HttpClient(CIO) {
        defaultRequest {
            url("http://localhost:80")
        }
        install(ContentNegotiation) {
            json()
        }
    }

    embeddedServer(Netty, port = 80, host = "0.0.0.0") {
        configureSecurity()
        configureHTTP()
        configureTemplating()
        configureSerialization()
        val mqttService = configureMqttService(firmwareHttpClient, client)
        configureDeviceAPI(mqttService, "http://localhost:80/", database)
        configureFirmwareAPI(database)
    }.start(wait = true)
}
