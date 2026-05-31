package io.github.adulescentia.lumosapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.adulescentia.LUMOS_lib.Device
import io.github.adulescentia.LUMOS_lib.GestureStateManager
import io.github.adulescentia.LUMOS_lib.Lumos
import io.github.adulescentia.LUMOS_lib.Result
import io.github.adulescentia.lumosapp.ui.theme.LUMOS_libTheme
import org.joml.Vector3f

class LUMOS : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LUMOS_libTheme {
                LumosTesterApp()
            }
        }
    }
}

@Composable
fun LumosTesterApp() {
    val lumos = remember { Lumos.getInstance() }
    val logs = remember { mutableStateListOf<String>() }
    val registeredDevices = remember { mutableStateListOf<Device>() }
    val powerStates = remember { mutableStateMapOf<String, Boolean>() }
    val modeLevels = remember { mutableStateMapOf<String, Int>() }
    var selectedDeviceId by rememberSaveable { mutableStateOf<String?>(null) }
    var latestResult by remember { mutableStateOf(Result()) }
    var deviceName by rememberSaveable { mutableStateOf("Living Room TV") }
    var deviceType by rememberSaveable { mutableStateOf("DISPLAY") }
    var x by rememberSaveable { mutableStateOf("0.0") }
    var y by rememberSaveable { mutableStateOf("1.2") }
    var z by rememberSaveable { mutableStateOf("4.0") }

    fun appendLog(message: String) {
        logs.add(0, message)
    }

    LaunchedEffect(lumos) {
        runCatching {
            lumos.initialize()
            lumos.registerExternalResultChannel { result ->
                latestResult = result
                appendLog("결과 콜백 수신: ${result.summary()}")
            }
        }.onSuccess {
            appendLog("Lumos.initialize() 완료 - MediaPipe 없이 API 테스트 모드로 시작")
        }.onFailure { error ->
            appendLog("초기화 실패: ${error.message}")
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeaderSection()
                DeviceInputCard(
                    deviceName = deviceName,
                    onDeviceNameChange = { deviceName = it },
                    deviceType = deviceType,
                    onDeviceTypeChange = { deviceType = it },
                    x = x,
                    onXChange = { x = it },
                    y = y,
                    onYChange = { y = it },
                    z = z,
                    onZChange = { z = it },
                    onAddDevice = {
                        runCatching {
                            lumos.registerDevice(
                                x.toDouble(),
                                y.toDouble(),
                                z.toDouble(),
                                deviceName.trim(),
                                deviceType.trim()
                            )
                        }.onSuccess { device ->
                            if (device != null) {
                                registeredDevices.add(device)
                                powerStates.putIfAbsent(device.id, false)
                                modeLevels.putIfAbsent(device.id, 50)
                            }
                            appendLog("디바이스 등록: ${device?.summary() ?: "null"}")
                        }.onFailure { error ->
                            appendLog("디바이스 등록 실패: ${error.javaClass.simpleName} - ${error.message}")
                        }
                    },
                    onSeedDevices = {
                        val samples = listOf(
                            SampleDevice("Living Room TV", "DISPLAY", 0.0, 1.2, 4.0),
                            SampleDevice("Desk Lamp", "LIGHT", -1.4, 0.8, 2.1),
                            SampleDevice("Speaker", "AUDIO", 1.6, 1.0, 2.8),
                        )
                        samples.forEach { sample ->
                            runCatching {
                                lumos.registerDevice(sample.x, sample.y, sample.z, sample.name, sample.type)
                            }.onSuccess { device ->
                                if (device != null) {
                                    registeredDevices.add(device)
                                    powerStates.putIfAbsent(device.id, false)
                                    modeLevels.putIfAbsent(device.id, 50)
                                }
                                appendLog("샘플 등록: ${device?.summary() ?: "null"}")
                            }.onFailure { error ->
                                appendLog("샘플 등록 실패(${sample.name}): ${error.message}")
                            }
                        }
                    }
                )
                LibraryActionCard(
                    onStart = {
                        runCatching { lumos.startIoTControlProcess() }
                            .onSuccess { appendLog("startIoTControlProcess() 호출 성공") }
                            .onFailure { error -> appendLog("프로세스 시작 실패: ${error.message}") }
                    },
                    onSerialize = {
                        runCatching { lumos.serializeDevices().toList() }
                            .onSuccess { serialized ->
                                appendLog("직렬화 ${serialized.size}건: ${serialized.joinToString()}")
                            }.onFailure { error -> appendLog("직렬화 실패: ${error.message}") }
                    },
                    onReloadSerialized = {
                        runCatching {
                            val serialized = lumos.serializeDevices()
                            lumos.deserializeDevices(serialized).toList()
                        }.onSuccess { devices ->
                            registeredDevices.clear()
                            registeredDevices.addAll(devices)
                            devices.forEach { device ->
                                powerStates.putIfAbsent(device.id, false)
                                modeLevels.putIfAbsent(device.id, 50)
                            }
                            if (selectedDeviceId !in devices.map { it.id }) selectedDeviceId = null
                            appendLog("역직렬화로 ${devices.size}건 재적재 완료")
                        }.onFailure { error -> appendLog("역직렬화 실패: ${error.message}") }
                    },
                    onShowSnapshot = {
                        runCatching { lumos.latestResultSnapshot }
                            .onSuccess { result ->
                                latestResult = result
                                appendLog("스냅샷: ${result.summary()}")
                            }.onFailure { error -> appendLog("스냅샷 조회 실패: ${error.message}") }
                    }
                )
                GestureActionCard(
                    onGesture = { gesture ->
                        runCatching { lumos.updateGesture(gesture, 0.5f) }
                            .onSuccess { appendLog("제스처 입력: $gesture") }
                            .onFailure { error -> appendLog("제스처 실패: ${error.message}") }
                    }
                )
                StatusCard(
                    deviceCount = registeredDevices.size,
                    latestResult = latestResult,
                )
                SmartHomeControlCard(
                    devices = registeredDevices,
                    selectedDeviceId = selectedDeviceId,
                    powerStates = powerStates,
                    modeLevels = modeLevels,
                    onSelect = { device ->
                        runCatching {
                            lumos.updateGesture(GestureStateManager.Gesture.FIST, 0.5f)
                            lumos.updateGesture(GestureStateManager.Gesture.ONE_FINGER, 0.5f)
                        }.onSuccess {
                            selectedDeviceId = device.id
                            appendLog("${device.name} 선택 완료 - LUMOS 제스처 선택 API 호출")
                        }.onFailure { error -> appendLog("선택 실패: ${error.message}") }
                    },
                    onTogglePower = { device ->
                        runCatching {
                            lumos.updateGesture(GestureStateManager.Gesture.FIST, 0.5f)
                            lumos.updateGesture(GestureStateManager.Gesture.PALM, 0.5f)
                        }.onSuccess {
                            powerStates[device.id] = !(powerStates[device.id] ?: false)
                            appendLog("${device.name} 전원 ${if (powerStates[device.id] == true) "ON" else "OFF"} - LUMOS 전원 제스처 API 호출")
                        }.onFailure { error -> appendLog("전원 변경 실패: ${error.message}") }
                    },
                    onChangeMode = { device, delta ->
                        runCatching {
                            lumos.updateGesture(GestureStateManager.Gesture.FIST, 0.5f)
                            lumos.updateGesture(GestureStateManager.Gesture.V_SIGN, 0.5f)
                            lumos.updateGesture(GestureStateManager.Gesture.V_SIGN, 0.5f - (delta / 100f))
                            lumos.updateGesture(GestureStateManager.Gesture.FIST, 0.5f - (delta / 100f))
                            lumos.updateGesture(GestureStateManager.Gesture.V_SIGN, 0.5f - (delta / 100f))
                        }.onSuccess {
                            val next = ((modeLevels[device.id] ?: 50) + delta).coerceIn(0, 100)
                            modeLevels[device.id] = next
                            appendLog("${device.name} 모드값 $next% - LUMOS 모드 제스처 API 호출")
                        }.onFailure { error -> appendLog("모드 변경 실패: ${error.message}") }
                    },
                )
                LogCard(logs = logs)
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "LUMOS 스마트홈 컨트롤러",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "라이브러리 코드는 건드리지 않고 LUMOS API로 IoT 디바이스를 등록·선택·제어하는 데모 앱입니다.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DeviceInputCard(
    deviceName: String,
    onDeviceNameChange: (String) -> Unit,
    deviceType: String,
    onDeviceTypeChange: (String) -> Unit,
    x: String,
    onXChange: (String) -> Unit,
    y: String,
    onYChange: (String) -> Unit,
    z: String,
    onZChange: (String) -> Unit,
    onAddDevice: () -> Unit,
    onSeedDevices: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("디바이스 등록")
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = deviceName,
                onValueChange = onDeviceNameChange,
                label = { Text("이름") },
                singleLine = true,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = deviceType,
                onValueChange = onDeviceTypeChange,
                label = { Text("타입") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoordinateField("X", x, onXChange, Modifier.weight(1f))
                CoordinateField("Y", y, onYChange, Modifier.weight(1f))
                CoordinateField("Z", z, onZChange, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = onAddDevice) { Text("등록") }
                Button(modifier = Modifier.weight(1f), onClick = onSeedDevices) { Text("샘플 3개") }
            }
        }
    }
}

@Composable
private fun CoordinateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
    )
}

@Composable
private fun LibraryActionCard(
    onStart: () -> Unit,
    onSerialize: () -> Unit,
    onReloadSerialized: () -> Unit,
    onShowSnapshot: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("라이브러리 API 테스트")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = onStart) { Text("프로세스 시작") }
                Button(modifier = Modifier.weight(1f), onClick = onShowSnapshot) { Text("스냅샷") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = onSerialize) { Text("직렬화") }
                Button(modifier = Modifier.weight(1f), onClick = onReloadSerialized) { Text("재적재") }
            }
        }
    }
}

@Composable
private fun GestureActionCard(onGesture: (GestureStateManager.Gesture) -> Unit) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("제스처 입력")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = { onGesture(GestureStateManager.Gesture.ONE_FINGER) }) { Text("선택") }
                Button(modifier = Modifier.weight(1f), onClick = { onGesture(GestureStateManager.Gesture.FIST) }) { Text("FIST") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = { onGesture(GestureStateManager.Gesture.PALM) }) { Text("전원") }
                Button(modifier = Modifier.weight(1f), onClick = { onGesture(GestureStateManager.Gesture.V_SIGN) }) { Text("모드") }
            }
        }
    }
}

@Composable
private fun StatusCard(deviceCount: Int, latestResult: Result) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle("현재 상태")
            Text("등록 디바이스 수: $deviceCount")
            Text("최신 결과: ${latestResult.summary()}")
        }
    }
}

@Composable
private fun SmartHomeControlCard(
    devices: List<Device>,
    selectedDeviceId: String?,
    powerStates: Map<String, Boolean>,
    modeLevels: Map<String, Int>,
    onSelect: (Device) -> Unit,
    onTogglePower: (Device) -> Unit,
    onChangeMode: (Device, Int) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("스마트홈 제어")
            if (devices.isEmpty()) {
                Text("디바이스를 등록하거나 샘플 3개를 추가하면 실제 제어 패널이 활성화됩니다.")
            } else {
                devices.forEach { device ->
                    val isSelected = selectedDeviceId == device.id
                    val isPowered = powerStates[device.id] == true
                    val mode = modeLevels[device.id] ?: 50
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(device.name, fontWeight = FontWeight.SemiBold)
                                    Text("${device.type} · ${device.position.format()}")
                                }
                                Text(if (isSelected) "선택됨" else "대기")
                            }
                            Text("전원: ${if (isPowered) "ON" else "OFF"} · 모드: $mode%")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(modifier = Modifier.weight(1f), onClick = { onSelect(device) }) { Text("조준") }
                                Button(modifier = Modifier.weight(1f), onClick = { onTogglePower(device) }) { Text("전원") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(modifier = Modifier.weight(1f), onClick = { onChangeMode(device, -10) }) { Text("모드 -") }
                                Button(modifier = Modifier.weight(1f), onClick = { onChangeMode(device, 10) }) { Text("모드 +") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogCard(logs: List<String>) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("실행 로그")
            Spacer(modifier = Modifier.height(8.dp))
            if (logs.isEmpty()) {
                Text("아직 로그가 없습니다.")
            } else {
                logs.take(20).forEach { log -> Text("• $log") }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

private data class SampleDevice(
    val name: String,
    val type: String,
    val x: Double,
    val y: Double,
    val z: Double,
)

private fun Device.summary(): String = "$id / $name / $type / ${position.format()}"

private fun Result.summary(): String {
    val selected = runCatching { selectedDevice.name }.getOrDefault("없음")
    return "direction=${direction.format()}, selected=$selected, command=$commandType/$commandDetail"
}

private fun Vector3f.format(): String = "(%.2f, %.2f, %.2f)".format(x, y, z)

@Preview(showBackground = true)
@Composable
private fun LumosTesterAppPreview() {
    LUMOS_libTheme {
        LumosTesterApp()
    }
}
