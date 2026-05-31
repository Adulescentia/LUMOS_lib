package io.github.adulescentia.lumosapp
import android.content.Context
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttPublisher(val mqttAccount: LumosExtended.MqttAccount,context : Context) {
    private var client: MqttClient = MqttClient("tcp://${mqttAccount.uri}:${mqttAccount.port}", LumosExtended.getOrCreateClientId(context), MemoryPersistence())
    private val callbacks = mutableMapOf<String,(MqttMessage?) -> Unit>()
    fun connect() {
        val options = MqttConnectOptions().apply {
            isCleanSession = false
            connectionTimeout = 10
            keepAliveInterval = 60
            // 필요한 경우 ID/PW 설정
             userName = mqttAccount.name
             password = mqttAccount.pw.toCharArray()
        }
        client.connect(options)
        setMqttCallback()
    }
    // 2. 특정 토픽 구독(Subscribe) 함수
    fun subscribe(topic: String, qos: Int = 1, callback: (MqttMessage?) -> Unit) {
        if (client.isConnected) {
            client.subscribe(topic, qos)
            println("구독 성공: $topic")
            callbacks[topic] = callback
        } else {
            println("구독 실패: 브로커와 연결되어 있지 않습니다.")
        }
    }

    // 3. 브로커로부터 메시지가 도착했을 때 동작할 콜백 설정
    private fun setMqttCallback() {
        client.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                // 연결이 끊겼을 때 처리 (예: 재연결 로직 트리거)
                println("MQTT 연결 유실: ${cause?.localizedMessage}")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                // 🚀 여기가 핵심! 구독한 토픽으로 메시지가 들어오면 이 함수가 실행됩니다.
                val payload = message?.payload?.let { String(it) } ?: ""
                println("📩 메시지 수신 성공! [토픽: $topic] -> 내용: $payload")
                callbacks[topic]?.invoke(message)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // 내가 publish한 메시지가 브로커에 잘 도착했을 때 호출됨 (필요 없으면 비워둠)
            }
        })
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