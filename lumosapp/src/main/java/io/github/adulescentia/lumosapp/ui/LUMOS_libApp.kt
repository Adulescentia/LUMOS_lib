package io.github.adulescentia.lumosapp.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.adulescentia.lumosapp.AppDestinations

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
                    onClick = {
                        currentDestination =
                            it; navController.navigate(currentDestination.toString())
                    }
                )
            }
        }
    ) {
        NavHost(
            navController,
            startDestination = AppDestinations.DEVICES.toString(),
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AppDestinations.HOME.toString()) { Home() }
            composable(AppDestinations.AUTO_RECOGNITION.toString()) { RecognitionScreen() }
            composable(AppDestinations.DEVICES.toString()) { DeviceScreen() }
        }
    }
}