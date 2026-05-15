package io.github.adulescentia.lumosapp
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttPublisher(serverUri: String, clientId: String) {
    private var client: MqttClient = MqttClient(serverUri, clientId, MemoryPersistence())

    fun connect() {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 60
            // 필요한 경우 ID/PW 설정
             userName = "yoonseo"
             password = "2134".toCharArray()
        }
        client.connect(options)
    }

    fun publish(topic: String, payload: String, qos: Int = 1, retained: Boolean = false) {
        if (client.isConnected) {
            val message = MqttMessage(payload.toByteArray()).apply {
                this.qos = qos
                this.isRetained = retained
            }
            client.publish(topic, message)
            println("발행 성공: $topic -> $payload")
        } else {
            println("발행 실패: 브로커와 연결되어 있지 않습니다.")
        }
    }

    fun disconnect() {
        client.disconnect()
    }
}