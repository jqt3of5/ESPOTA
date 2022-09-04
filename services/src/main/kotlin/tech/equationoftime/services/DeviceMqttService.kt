package tech.equationoftime.services

import org.eclipse.paho.client.mqttv3.MqttClient

class DeviceMqttService {

    var _client : MqttClient? = null
    object DeviceMqttService {
        public val clientId : String = "esp-ota-manager"
    }

    fun connect(host : String, port : Int = 1883)
    {
        val client = MqttClient(host, DeviceMqttService.clientId)

        register(client){
           filter("") {

           }
        }
        _client = client
    }


    public fun reboot(deviceId : String)
    {

    }

    public fun flash(deviceId : String, firmwareId : String)
    {

    }

    public fun wifi(deviceId : String, ssid : String, psk : String)
    {

    }
    public fun mdsn(deviceId : String, name : String)
    {

    }

    fun register(client: MqttClient, configuration: MqttClient.() -> Unit)
    {

    }
    fun filter(topicFilter : String, body: MqttClient.() -> Unit)
    {
       _client.
    }

}