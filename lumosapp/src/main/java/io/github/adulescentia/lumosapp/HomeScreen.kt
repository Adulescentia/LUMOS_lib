package io.github.adulescentia.lumosapp

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home() {
    // 1. 상태 관리
    var cloudUrl by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // 로딩 상태 관리
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sharedPref = remember {
        context.getSharedPreferences("IoT_Prefs", Context.MODE_PRIVATE)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // 은은한 그라데이션 배경으로 고급스러움 추가
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // 헤더 아이콘 & 텍스트
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "클라우드 설정",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "서버 정보를 입력하여 원격 제어를 시작하세요",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 입력 카드
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CustomInputField("서버 주소", cloudUrl, Icons.Default.Language) { cloudUrl = it }
                    CustomInputField("포트", port, Icons.Default.SettingsEthernet, isNumber = true) { port = it }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    CustomInputField("사용자명", username, Icons.Default.AccountCircle) { username = it }
                    CustomInputField("비밀번호", password, Icons.Default.VpnKey, isPassword = true) { password = it }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 메인 버튼
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        delay(2000) //TODO NETWORK CHECK
                        val mqttAcc = LumosExtended.MqttAccount(cloudUrl,port,password,username)
                        LumosExtended.saveMqttAccount(mqttAcc)
                        LumosExtended.initializeMqttPublisher(context)
                        LumosExtended.mqttPublisher.connect()
                        LumosExtended.mqttPublisher.publish("connected.check", "app")
                        LumosExtended.mqttPublisher.subscribe("testt") { println("subsub!!!!!!!!! : $it") }
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                enabled = !isLoading && cloudUrl.isNotBlank() && username.isNotBlank(),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (isLoading) {
                    // 버튼 내부 로딩 스피너
                    LinearProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        trackColor = ProgressIndicatorDefaults.linearTrackColor,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                } else {
                    Text("계정 저장", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        delay(2000) //TODO NETWORK CHECK
                        LumosExtended.initializeMqttPublisher(context)
                        LumosExtended.mqttPublisher.connect()
                        LumosExtended.mqttPublisher.publish("connected.check", "app")
                        LumosExtended.mqttPublisher.subscribe("testt") { println("subsub!!!!!!!!! : $it") }
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                enabled = true,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (isLoading) {
                    // 버튼 내부 로딩 스피너
                    LinearProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        trackColor = ProgressIndicatorDefaults.linearTrackColor,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                } else {
                    Text("연결 테스트 시작", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
@Preview
@Composable
@Deprecated("of no use")
fun a(){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)) // 뒤를 어둡게
            .pointerInput(Unit) {}, // 클릭 방지
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("서버에 접속 중...", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// 공통 입력을 위한 컴포저블 함수 분리 (코드 깔끔하게)
@Composable
fun CustomInputField(
    label: String,
    value: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    isNumber: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isNumber) KeyboardType.Number else if (isPassword) KeyboardType.Password else KeyboardType.Text
        ),
//        colors = TextFieldDefaults.outlinedTextFieldColors(
//            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
//        )
    )
}