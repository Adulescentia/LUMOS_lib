package io.github.adulescentia.lumosapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.adulescentia.LUMOS_lib.Device
import io.github.adulescentia.LUMOS_lib.LumosImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Preview(name = "gg")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    deviceViewModel: DeviceViewModel = viewModel()
){
    var showDeviceAddModal by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(true)
    // Lumos 인스턴스에서 디바이스 목록을 가져옵니다.
    val devices by deviceViewModel.devices.collectAsState()
    val isLoading by deviceViewModel.isLoading.collectAsState()
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDeviceAddModal = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = {
                    deviceViewModel.fetchDevicesFromBackend()
                },
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                DeviceList(devices)
                if (showDeviceAddModal) {
                    DeviceAddScreen({ showDeviceAddModal = false; deviceViewModel.fetchDevicesFromBackend() }, sheetState)
                }
            }
        }
    }
}
@Composable
fun DeviceList(devices : List<Device>){
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 디바이스 목록이 비어있을 때의 처리를 추가하여 'List is empty' 관련 렌더링 오류를 방지합니다.
        if (devices.isEmpty()) {
            item {
                Text(
                    text = "등록된 기기가 없습니다.",
                    modifier = Modifier.padding(top = 100.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(devices.toList()) { device ->
                DeviceItem(device)
            }
        }
    }
}

@Composable
fun DeviceItem(device: Device) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DeviceHub,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // device.position은 Vector3f 타입이며, JOML의 Vector3f는 x, y, z 필드를 직접 노출합니다.
                val pos = device.position
                Text(
                    text = "ID: ${device.id} | Pos: (${pos.x}, ${pos.y}, ${pos.z})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceAddScreen(onDismissRequest : () -> Unit, sheetState: SheetState) {
    // 1. 상태 관리
    var deviceName by rememberSaveable { mutableStateOf("") }
    var deviceX by rememberSaveable { mutableStateOf("") }
    var deviceY by rememberSaveable { mutableStateOf("") }
    var deviceZ by rememberSaveable { mutableStateOf("") }
    var deviceIp by rememberSaveable { mutableStateOf("") }
    
    // 로딩 및 성공 상태 관리
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isCordInputError = deviceX.toDoubleOrNull() == null || deviceY.toDoubleOrNull() == null || deviceZ.toDoubleOrNull() == null

    ModalBottomSheet(
        onDismissRequest = if (isLoading) ({}) else onDismissRequest,
        sheetState = sheetState,
        dragHandle = {
            // 직접 핸들을 디자인합니다.
            Surface(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = CircleShape
            ) {
                Box(Modifier.size(width = 32.dp, height = 4.dp))
            }
        }
    ) {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Image(
                    imageVector = Icons.Default.DeviceHub,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "라즈베리 파이 등록",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "연결할 기기의 이름과 IP 주소를 입력해주세요.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("기기 이름 (예: 거실 전등)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading && !isSuccess
                )

                Spacer(modifier = Modifier.height(16.dp))
                if (isCordInputError) {
                    Text(
                        text = "올바른 숫자를 입력해주세요.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                // Row에 fillMaxWidth를 추가하고 weight를 균등하게 배분하여 레이아웃 계산 오류를 방지합니다.
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = deviceX,
                        onValueChange = { deviceX = it },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f),
                        isError = deviceX.toDoubleOrNull() == null,
                        singleLine = true,
                        placeholder = { Text("m") },
                        enabled = !isLoading && !isSuccess
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = deviceY,
                        onValueChange = { deviceY = it },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f),
                        isError = deviceY.toDoubleOrNull() == null,
                        singleLine = true,
                        placeholder = { Text("m") },
                        enabled = !isLoading && !isSuccess
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = deviceZ,
                        onValueChange = { deviceZ = it },
                        label = { Text("Z") },
                        modifier = Modifier.weight(1f),
                        isError = deviceZ.toDoubleOrNull() == null,
                        singleLine = true,
                        placeholder = { Text("m") },
                        enabled = !isLoading && !isSuccess
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = deviceIp,
                    onValueChange = { deviceIp = it },
                    label = { Text("라즈베리 파이 IP (예: 192.168.0.100)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("또는 ZeroTier 가상 IP") },
                    enabled = !isLoading && !isSuccess
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            // 인위적인 지연시간 추가 (네트워크 통신 시뮬레이션)
                            delay(1500)

                            try {
                                LumosImpl.getInstance().registerDevice(
                                    deviceX.toDoubleOrNull() ?: 0.0,
                                    deviceY.toDoubleOrNull() ?: 0.0,
                                    deviceZ.toDoubleOrNull() ?: 0.0,
                                    deviceName,
                                    "Lamp"
                                )
                                isLoading = false
                                isSuccess = true
                                // 성공 메시지를 잠시 보여준 뒤 창 닫기
                                delay(1000)
                                println("yeah it running")
                                onDismissRequest()
                            } catch (e: Exception) {
                                isLoading = false
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = deviceName.isNotBlank() && deviceIp.isNotBlank() && !isCordInputError && !isLoading && !isSuccess
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else if (isSuccess) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("등록 완료")
                    } else {
                        Text("기기 추가 완료", fontSize = 16.sp)
                    }
                }
            }

            // 로딩/성공 시 오버레이 (선택사항)
            if (isLoading || isSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSuccess) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "등록 성공!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

    }
}
