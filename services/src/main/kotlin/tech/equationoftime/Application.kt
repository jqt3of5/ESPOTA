package tech.equationoftime

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.eclipse.paho.client.mqttv3.MqttClient
import tech.equationoftime.models.DeviceMetadata
import tech.equationoftime.models.FirmwareMetadata
import tech.equationoftime.plugins.*
import tech.equationoftime.services.DeviceMqttService

val firmwareRoot = "firmwares"

fun main() {

    var deviceMetadata = mutableListOf<DeviceMetadata>()
    var firmwareMetadata = mutableListOf<FirmwareMetadata>()

    firmwareMetadata.add(FirmwareMetadata("1234", "name", "1.0.0", "esp"))
    firmwareMetadata.add(FirmwareMetadata("12345", "name", "1.0.1", "esp"))
    firmwareMetadata.add(FirmwareMetadata("12346", "name2", "1.0.1", "esp"))

    val client = MqttClient("tcp://localhost:1883","esp-ota-manager")
    val mqttService = DeviceMqttService(client) {
            filter("manager/{deviceId}/online") {topic, message ->
                //TODO: Update model
            }
            filter("manager/{deviceId}/offline") { topic, message ->
                //TODO: Update model
            }
        }

    embeddedServer(Netty, port = 80, host = "0.0.0.0") {
        configureSecurity()
        configureHTTP()
        configureTemplating()
        configureSerialization()
        configureFirmwareAPI(firmwareMetadata)
        configureDeviceAPI(mqttService, deviceMetadata)
    }.start(wait = true)
}
