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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material3.Button
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(){
    var showDeviceAddModal by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(true)
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDeviceAddModal = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.Companion.padding(innerPadding).fillMaxSize()) {
            LazyColumn(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                //TODO("Lazy loading Device 구현")
            }
        }
        if (showDeviceAddModal) {
            DeviceAddScreen({ showDeviceAddModal = false }, sheetState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceAddScreen(onDismissRequest : () -> Unit, sheetState: SheetState) {
    // 1. 상태 관리
    var deviceName by rememberSaveable { mutableStateOf("") }
    var deviceX by rememberSaveable { mutableStateOf("") }
    var deviceZ by rememberSaveable { mutableStateOf("") }
    var deviceIp by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = {
            // 직접 핸들을 디자인합니다.
            Surface(
                modifier = Modifier.Companion
                    .padding(top = 12.dp, bottom = 8.dp), // 핸들 주변 여백
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), // 살짝 투명하게
                shape = CircleShape
            ) {
                // 핸들의 가로 세로 크기를 여기서 조절하세요.
                Box(Modifier.Companion.size(width = 32.dp, height = 4.dp))
            }
        }
    ) {
        Scaffold { innerPadding ->
            // 3. 메인 콘텐츠 영역 (Scaffold의 padding 적용 필수)
            Column(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(innerPadding) // 상단 바 영역 제외
                    .background(MaterialTheme.colorScheme.surface) // 테마 색상 사용
                    .padding(24.dp), // 내부 전체 여백
                horizontalAlignment = Alignment.Companion.CenterHorizontally
            ) {
                // 4. 시각적 요소 추가 (아이콘/이미지)
                Spacer(modifier = Modifier.Companion.height(20.dp))
                Image(
                    imageVector = Icons.Default.DeviceHub,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    colorFilter = ColorFilter.Companion.tint(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.Companion.height(16.dp))

                // 5. 제목 텍스트 세련되게 수정
                Text(
                    text = "라즈베리 파이 등록",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Companion.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "연결할 기기의 이름과 IP 주소를 입력해주세요.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Companion.Center,
                    modifier = Modifier.Companion.padding(top = 8.dp, bottom = 32.dp)
                )

                // 6. 텍스트필드 업그레이드 (꽉 차게, 아이콘 추가)
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("기기 이름 (예: 거실 전등)") },
                    modifier = Modifier.Companion.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.Companion.height(16.dp))

                Row {
                    OutlinedTextField(
                        value = deviceX,
                        onValueChange = { deviceX = it },
                        label = { Text("X") },
                        modifier = Modifier.Companion.weight(0.3f),
                        singleLine = true,
                        placeholder = { Text("단위 m (미터)") }
                    )

                    Spacer(modifier = Modifier.Companion.width(16.dp))

                    OutlinedTextField(
                        value = deviceZ,
                        onValueChange = { deviceZ = it },
                        label = { Text("Z") },
                        modifier = Modifier.Companion.weight(0.3f),
                        singleLine = true,
                        placeholder = { Text("단위 m (미터)") }
                    )
                }

                Spacer(modifier = Modifier.Companion.height(16.dp))

                OutlinedTextField(
                    value = deviceIp,
                    onValueChange = { deviceIp = it },
                    label = { Text("라즈베리 파이 IP (예: 192.168.0.100)") },
                    modifier = Modifier.Companion.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("또는 ZeroTier 가상 IP") }
                )

                // 7. 남은 공간을 버튼 위로 밀어내기
                Spacer(modifier = Modifier.Companion.weight(1f))

                // 8. 하단 완료 버튼 추가
                Button(
                    onClick = { /* 저장 및 연결 테스트 로직 */ },
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = deviceName.isNotBlank() && deviceIp.isNotBlank() // 입력 필수
                ) {
                    Text("기기 추가 완료", fontSize = 16.sp)
                }
            }
        }
    }
    // 2. Scaffold를 사용해 상단 바 배치
}