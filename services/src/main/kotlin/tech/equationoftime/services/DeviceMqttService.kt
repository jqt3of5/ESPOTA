package tech.equationoftime.services

import kotlinx.coroutines.runBlocking
import org.eclipse.paho.client.mqttv3.*
import java.util.regex.Pattern

class MqttRoutes(val mqttClient : IMqttClient) {
    fun filter(topicFilterPlus : String, body: (suspend (topic : String, payload: RoutedMessage) -> Unit))
    {
        val parts = topicFilterPlus.split("/")
        val topicFilter = parts.map {
            if (Pattern.matches("\\{.*\\}", it))
            {
                return@map "+"
            }
            return@map it
        }.joinToString(separator = "/")

        mqttClient.subscribe(topicFilter) { topic, message ->
            val topicParts = topic.split("/")
            val params = parts.zip(topicParts).filter {
                if (Pattern.matches("\\{.*\\}", it.first))
                {
                    return@filter true
                }
                return@filter false
            }.map {
                val (tagName) = Regex("\\{(.*)\\}").find(it.first)!!.destructured
                tagName to it.second
            }.toMap()

            runBlocking {
                body(topic, RoutedMessage(message.payload, params))
            }
        }
    }
    public data class RoutedMessage(val payload : ByteArray, val pathParams : Map<String, String>)
}

class DeviceMqttService(val _client: IMqttClient, val configuration: (DeviceMqttService.() -> Unit)? = null) : MqttCallback {

    var _routes : MqttRoutes
    val _topicPrefix = "ESPOTA"

    companion object {
    }

    init {
        _client.setCallback(this)
        _routes = MqttRoutes(_client)
    }

    fun connect()
    {
        _client.connect()
        configuration?.invoke(this)
    }

    public fun deviceHello(handler: suspend (topic: String, payload: MqttRoutes.RoutedMessage) -> Unit)
    {
        _routes.filter("${_topicPrefix}/manager/{deviceId}/hello", handler)
    }
    public fun deviceOnline(handler: suspend (topic: String, payload: MqttRoutes.RoutedMessage) -> Unit)
    {
        _routes.filter("${_topicPrefix}/manager/{deviceId}/online", handler)
    }
    public fun deviceOffline(handler: suspend (topic: String, payload: MqttRoutes.RoutedMessage) -> Unit)
    {
        _routes.filter("${_topicPrefix}/manager/{deviceId}/offline", handler)
    }
    public fun deviceUpdating(handler: suspend (topic: String, payload: MqttRoutes.RoutedMessage) -> Unit)
    {
        _routes.filter("${_topicPrefix}/manager/{deviceId}/updating", handler)
    }
    public fun reboot(deviceId : String)
    {
        _client.publish("${_topicPrefix}/device/$deviceId/reboot", MqttMessage("\"\"".toByteArray()))
    }
    public fun flash(deviceId : String, firmwareAPIUrl : String)
    {
        _client.publish("${_topicPrefix}/device/$deviceId/flash", MqttMessage(firmwareAPIUrl.toByteArray()))
    }
    public fun wifi(deviceId : String, ssid : String, psk : String)
    {
        _client.publish("${_topicPrefix}/device/$deviceId/wifi", MqttMessage("$ssid:$psk".toByteArray()))
    }
    public fun mdns(deviceId : String, name : String)
    {
        _client.publish("${_topicPrefix}/device/$deviceId/mdns", MqttMessage(name.toByteArray()))
    }

    override fun connectionLost(cause: Throwable?) {
        println(cause)
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        println("$topic - \"${message?.payload.toString()}\"")
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        println(token)
    }
}

