package io.github.adulescentia.lumosapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import org.joml.Vector3f // 기존에 사용하시던 JOML 라이브러리

@Composable
fun VectorOverlay(
    wristX: Float,
    wristY: Float,
    armVector: Vector3f?,
    modifier: Modifier = Modifier
) {
    // Canvas를 화면 전체에 꽉 채웁니다.
    Canvas(modifier = modifier.fillMaxSize()) {
        // 벡터 데이터가 없으면 그리지 않고 종료합니다.
        if (armVector == null) return@Canvas

        // 화면에 그릴 벡터의 길이 스케일 (실제 3D 벡터는 1.0 전후로 작기 때문에 화면 픽셀에 맞게 증폭)
        val lineLength = 300f

        // 시작점 (손목)
        val startPoint = Offset(wristX, wristY)

        // 끝점 (벡터 방향으로 lineLength 만큼 이동한 좌표)
        val endPoint = Offset(
            x = wristX + (armVector.x * lineLength),
            y = wristY + (armVector.y * lineLength)
        )

        // 1. 벡터 선 그리기
        drawLine(
            color = Color.Red,
            start = startPoint,
            end = endPoint,
            strokeWidth = 15f,
            cap = StrokeCap.Round
        )

        // 2. 화살표 머리 (간단히 원으로 표현)
        drawCircle(
            color = Color.Red,
            radius = 20f,
            center = endPoint
        )
    }
}


@Composable
fun ARCameraScreen() {
    // 상태값(State): 엔진의 콜백에서 이 값들을 업데이트해주면 Compose가 자동으로 다시 그립니다.
    var currentWristX by remember { mutableFloatStateOf(0f) }
    var currentWristY by remember { mutableFloatStateOf(0f) }
    var currentArmVector by remember { mutableStateOf<Vector3f?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 가장 밑바닥에 카메라 프리뷰를 깔아줍니다. (CameraX 등 연동)
        // CameraPreviewComposable()

        // 2. 그 위에 투명한 벡터 오버레이를 겹칩니다.
        VectorOverlay(
            wristX = currentWristX,
            wristY = currentWristY,
            armVector = currentArmVector
        )
    }
}