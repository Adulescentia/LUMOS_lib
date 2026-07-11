package io.github.adulescentia.lumosapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.adulescentia.lumosapp.ui.LUMOS_libApp
import io.github.adulescentia.lumosapp.ui.theme.LUMOS_libTheme

class LUMOS : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LumosExtended.initialize(this, "pose_landmarker_full.task", "gesture_recognizer.task");
        val pr = ContextCompat.checkSelfPermission(
            baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!pr) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0)
        }
        LumosExtended.loadAll()
        LumosExtended.tryConnectMqtt(this)
        enableEdgeToEdge()
        setContent {
            LUMOS_libTheme {
                LUMOS_libApp()
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        LumosExtended.saveAll()
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



