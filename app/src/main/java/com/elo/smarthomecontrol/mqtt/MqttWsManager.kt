package com.elo.smarthomecontrol.mqtt

import android.content.Context
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MqttWsManager(
    context: Context,
    clientId: String
) {

    private val serverUri = "ws://broker.hivemq.com:8000/mqtt"

    private val client = MqttAndroidClient(
        context,
        serverUri,
        clientId
    )

    fun connect() {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            isAutomaticReconnect = true
            connectionTimeout = 10
            keepAliveInterval = 20
        }

        if (!client.isConnected) {
            client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // CONNECTED
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken?,
                    exception: Throwable?
                ) {
                    exception?.printStackTrace()
                }
            })
        }
    }

    fun publish(topic: String, payload: String) {
        if (!client.isConnected) return

        val msg = MqttMessage(payload.toByteArray()).apply {
            qos = 0
            isRetained = false
        }
        client.publish(topic, msg)
    }

    fun publishRgb(r: Int, g: Int, b: Int) {
        publish("smarthome/rgb", "$r,$g,$b")
    }

    fun publishSwitch(device: String, state: String) {
        publish("smarthome/$device", state)
    }
}