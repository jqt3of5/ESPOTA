package tech.equationoftime.services

import org.eclipse.paho.client.mqttv3.*
import java.util.regex.Pattern

class MqttRoutes(val mqttClient : IMqttClient) {
    fun filter(topicFilterPlus : String, body: (topic : String, payload: RoutedMessage) -> Unit)
    {
        val parts = topicFilterPlus.split("/")
        val topicFilter = parts.map {
            if (Pattern.matches("\\{.*\\}", it))
            {
                return@map "#"
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
            }.toMap()

            body(topic, RoutedMessage(message.payload, params))
        }
    }
    public data class RoutedMessage(val payload : ByteArray, val pathParams : Map<String, String>)
}

class DeviceMqttService(val _client: IMqttClient, val configuration: (MqttRoutes.() -> Unit)? = null) : MqttCallback {

    var _routes : MqttRoutes
    companion object {
    }

    init {
        _client.setCallback(this)
        _routes = MqttRoutes(_client)
    }

    fun connect()
    {
        _client.connect()
        configuration?.invoke(_routes)
    }
    public fun reboot(deviceId : String)
    {
        _client.publish("device/$deviceId/reboot", MqttMessage())
    }
    public fun flash(deviceId : String, firmwareId : String)
    {
        //todo: What is the public facing url?
        _client.publish("device/$deviceId/flash", MqttMessage("firmware/$firmwareId".toByteArray()))
    }
    public fun wifi(deviceId : String, ssid : String, psk : String)
    {
        _client.publish("device/$deviceId/wifi", MqttMessage("$ssid:$psk".toByteArray()))
    }
    public fun mdns(deviceId : String, name : String)
    {
        _client.publish("device/$deviceId/mdns", MqttMessage(name.toByteArray()))
    }

    override fun connectionLost(cause: Throwable?) {
        println(cause)
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        println("$topic - \"${message?.payload.toString()}\"")
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
    }
}