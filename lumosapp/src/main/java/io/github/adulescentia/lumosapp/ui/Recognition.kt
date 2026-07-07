package io.github.adulescentia.lumosapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.adulescentia.LUMOS_lib.Device
import io.github.adulescentia.lumosapp.LumosExtended

@Composable
fun RecognitionScreen(viewModel: RecognitionViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // StateFlow를 Compose State로 변환하여 관찰
    val resultState by viewModel.lumosResult.collectAsState()

    // 화면이 처음 Composition 될 때 한 번만 실행
    LaunchedEffect(Unit) {
        viewModel.initAndStart(context, lifecycleOwner)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 배경에 카메라 프리뷰 뷰를 배치 (별도의 CameraX Preview 뷰 필요)
        CameraFrameAnalyzerScreen()
        // 이전에 만들었던 손 벡터 오버레이를 띄웁니다.
        // resultState에서 손목 좌표를 추출할 수 있다면 함께 넘겨줍니다.
        VectorOverlay(
            wristX = 500f, // TODO: Result에서 실제 화면 X 좌표 추출 필요
            wristY = 500f, // TODO: Result에서 실제 화면 Y 좌표 추출 필요
            armVector = resultState?.direction
        )

        // 화면 상단에 인식 결과 텍스트 오버레이
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            if (resultState != null) {
                val selectedDevice : Device? = resultState!!.selectedDevice
                val cmdType = resultState!!.commandType

                Text(
                    text = "선택된 기기: ${selectedDevice?.name ?: "없음"}",
                    fontSize = 24.sp,
                    color = Color.White
                )

                Text(
                    text = "명령 상태: $cmdType",
                    fontSize = 18.sp,
                    color = Color.Yellow
                )

                Text(
                    text = "벡터: ${resultState!!.direction}",
                    fontSize = 14.sp,
                    color = Color.Green
                )

//                Text(
//                        text = "제스쳐: ${}",
//                fontSize = 14.sp,
//                color = Color.Green
//                )


            } else {
                Text("엔진 초기화 중...", color = Color.White)
            }
        }
    }
}
