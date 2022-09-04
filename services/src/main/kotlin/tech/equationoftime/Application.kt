package tech.equationoftime

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import tech.equationoftime.models.DeviceMetadata
import tech.equationoftime.models.FirmwareMetadata
import tech.equationoftime.plugins.*

var firmwareMetadata = mutableListOf<FirmwareMetadata>()
var deviceMetadata = mutableListOf<DeviceMetadata>()
val firmwareRoot = "firmwares"

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureSecurity()
        configureHTTP()
        configureTemplating()
        configureSerialization()
        configureFirmwareAPI()
        configureDeviceAPI()
    }.start(wait = true)
}
