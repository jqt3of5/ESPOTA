package tech.equationoftime

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import tech.equationoftime.plugins.*
import kotlin.io.path.Path

var firmwareMetadata = mutableListOf<FirmwareMetadata>()
val firmwareRoot = "firmwares"

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureSecurity()
        configureHTTP()
        configureTemplating()
        configureSerialization()
        configureRouting()
    }.start(wait = true)
}
