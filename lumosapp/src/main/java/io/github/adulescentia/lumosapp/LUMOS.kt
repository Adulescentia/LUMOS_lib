package io.github.adulescentia.lumosapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.adulescentia.LUMOS_lib.Lumos
import io.github.adulescentia.lumosapp.ui.theme.LUMOS_libTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.MqttClient

class LUMOS : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LumosExtended.initialize()
        enableEdgeToEdge()
        setContent {
            LUMOS_libTheme {
                LUMOS_libApp()
            }
        }

    }
}

object LumosExtended : Lumos by Lumos.getInstance() {

    private var sharedPref: SharedPreferences? = null
    fun initializeWithContext(context: Context, modelAssetPath: String) {
        initialize(context,modelAssetPath)
    }

    // 1. 상태 관찰용 Flow (비밀번호나 기기 목록을 외부에서 실시간 구독 가능)
    private val _mqttAccount = MutableStateFlow<MqttAccount?>(null)
    val mqttPassword: StateFlow<MqttAccount?> = _mqttAccount.asStateFlow()
    // 2. 외부(백엔드, 뷰모델 등) 어디서나 호출 가능한 MQTT 통신 함수
    fun connectMqtt() {
        val mqttAccount = _mqttAccount.value
        if (mqttAccount != null) {
            // 🚀 실제 MQTT 연결 로직 실행 (백엔드 서비스에서도 이 함수를 호출!)
            println("MQTT 연결 성공 : $mqttAccount")
        }
    }
    @Serializable
    data class MqttAccount(val pw : String, val name: String)
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


        _mqttAccount.value = acc // 비밀번호가 바뀌면 감지하고 있는 모든 곳에 전파
    }
    fun saveAll(){
        sharedPref?.edit {
            putString("device_list",Json.encodeToString(serializeDevices()))
        }
    }
    fun loadAll(){
        val deviceListStr = sharedPref?.getString("device_list","") ?: ""
        val deviceListStrArr = Json.decodeFromString<Array<String>>(deviceListStr)
        val deviceList = deserializeDevices(deviceListStrArr)
    }
}
@Preview
@Composable
fun LUMOS_libApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val navController = rememberNavController()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it; navController.navigate(currentDestination.toString()) }
                )
            }
        }
    ) {
        NavHost(navController, startDestination = AppDestinations.DEVICES.toString(),modifier = Modifier.fillMaxSize()){
            composable(AppDestinations.HOME.toString()) { Home() }
            composable(AppDestinations.AUTO_RECOGNITION.toString()) { Greeting("AUTO_RECOGNITION") }
            composable(AppDestinations.DEVICES.toString()) { DeviceScreen() }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    AUTO_RECOGNITION("Auto Recognition", Icons.Filled.Accessibility),
    DEVICES("Devices", Icons.Filled.Devices),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

