package io.github.adulescentia.lumosapp

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.adulescentia.LUMOS_lib.LumosImpl
import io.github.adulescentia.LUMOS_lib.LumosInterface
import io.github.adulescentia.LUMOS_lib.NotInitializedErr
import io.github.adulescentia.LUMOS_lib.Result
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.MqttClient
import java.io.EOFException

object LumosExtended : LumosInterface by LumosImpl.getInstance() {

    private var sharedPref: SharedPreferences? = null

    override fun initialize(
        context: Context,
        poseModelAssetPath: String,
        gestureModelAssetPath: String?
    ) {
        //TODO HANDLER
        LumosImpl.getInstance().initialize(context, poseModelAssetPath, gestureModelAssetPath)
        LumosExtended.registerExternalResultChannel { result ->
            val device = result.selectedDevice ?: return@registerExternalResultChannel
            when(result.commandType) {
                Result.CommandType.DEVICE_POWER_TOGGLED -> mqttPublisher.control(device,"toggle")
                Result.CommandType.DEVICE_MODE_APPLIED -> mqttPublisher.control(device,result.commandDetail)
                else -> {}
            }
        }
    }

    // 1. 상태 관찰용 Flow (비밀번호나 기기 목록을 외부에서 실시간 구독 가능)
    var mqttAccount: MqttAccount? = null
        private set
    // 2. 외부(백엔드, 뷰모델 등) 어디서나 호출 가능한 MQTT 통신 함수
    fun tryConnectMqtt(context: Context) : Boolean{
        val mqttAccount = mqttAccount
        if (mqttAccount != null) {
            initializeMqttPublisher(context)
            try {
                mqttPublisher.connect()
                println("MQTT 연결 성공 : $mqttAccount")
                return true
            }catch (e : EOFException){ e.printStackTrace() }
        }
        return false
    }
    lateinit var mqttPublisher: MqttPublisher
    fun initializeMqttPublisher(context : Context) {
        if(mqttAccount == null) throw NotInitializedErr("mqtt account is not set")
        mqttPublisher = MqttPublisher(mqttAccount!!,context)
    }

    @Serializable
    data class MqttAccount(val uri : String,val port : String,val pw : String, val name: String)
    fun getOrCreateClientId(context : Context): String {
        val sharedPref = context.getSharedPreferences("IoT_Prefs", Context.MODE_PRIVATE)
        var clientId = sharedPref.getString("mqtt_client_id", null)

        if (clientId == null) {
            // 최초 1회만 랜덤 생성 후 저장
            clientId = "LUMOS_APP_" + MqttClient.generateClientId()
            sharedPref.edit {
                putString("mqtt_client_id", clientId)
            }
        }
        return clientId
    }
    fun saveMqttAccount(acc : MqttAccount) {
        sharedPref?.edit {
            putString("mqtt_acc", Json.encodeToString(acc))
        }


        mqttAccount = acc // 비밀번호가 바뀌면 감지하고 있는 모든 곳에 전파
    }
    fun saveAll(){
        sharedPref?.edit {
            putString("device_list", Json.encodeToString(serializeDevices()))
        }
    }
    fun loadAll(){
        loadAccount()
        loadDeviceList()
    }
    fun loadAccount(){
        val k = sharedPref?.getString("mqtt_acc","").takeIf { it?.isEmpty() == false } ?: return
        mqttAccount = Json.decodeFromString<MqttAccount>(k)
    }
    fun loadDeviceList() {
        val deviceListStr = sharedPref?.getString("device_list","").takeIf { it?.isEmpty() == false } ?: return
        val deviceListStrArr = Json.decodeFromString<Array<String>>(deviceListStr)
        val deviceList = deserializeDevices(deviceListStrArr)
        deviceList.forEach {
            registerDevice(
                it.position.x.toDouble(),
                it.position.y.toDouble(), it.position.z.toDouble(),it.name,it.type)
        }
    }
}