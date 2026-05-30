package io.github.adulescentia.lumosapp

import android.content.Context
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.adulescentia.lumosapp.ui.theme.LUMOS_libTheme
import androidx.core.content.edit
import io.github.adulescentia.LUMOS_lib.Lumos

class LUMOS : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lumos.getInstance().initialize()
        enableEdgeToEdge()
        setContent {
            LUMOS_libTheme {
                //testing still
            }
        }
    }
    val sharedPref = getSharedPreferences("IoT_Prefs", Context.MODE_PRIVATE)

    // 2. 비밀번호 저장하기
    fun saveMqttPassword(pw: String) {
        sharedPref.edit {
            putString("mqtt_pw", pw)
        } // 또는 commit()
    }

    // 3. 비밀번호 불러오기
    fun getMqttPassword(): String? {
        // 저장된 게 없으면 null을 반환
        return sharedPref.getString("mqtt_pw", null)
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

