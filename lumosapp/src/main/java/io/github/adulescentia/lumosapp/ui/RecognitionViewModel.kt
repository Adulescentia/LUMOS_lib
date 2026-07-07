package io.github.adulescentia.lumosapp.ui

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import io.github.adulescentia.LUMOS_lib.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.adulescentia.lumosapp.LumosExtended

class RecognitionViewModel : ViewModel() {


    // UI에서 관찰할 상태 (초기값은 null)
    private val _lumosResult = MutableStateFlow<Result?>(null)
    val lumosResult: StateFlow<Result?> = _lumosResult.asStateFlow()

    fun initAndStart(context: Context, lifecycleOwner: LifecycleOwner) {
        // 1. 초기화


        // 2. 테스트용 가상 기기 등록 (필요시)
        if (LumosExtended.deviceList.isEmpty()) {
            LumosExtended.registerDevice(1.0, 0.0, 5.0, "스마트 전등", "LIGHT")
            LumosExtended.registerDevice(-2.0, 1.0, 4.0, "에어컨", "AC")
        }

        // 3. 콜백을 Flow로 연결
        LumosExtended.registerExternalResultChannel { result ->
            // 백그라운드 스레드에서 넘어올 수 있으므로 value 속성에 바로 할당합니다.
            // (StateFlow는 thread-safe 하게 상태를 업데이트합니다)
            _lumosResult.value = result
        }

        // 4. 카메라 프로세스 시작
        //LumosExtended.startCameraControlProcess(context, lifecycleOwner)

    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel이 소멸될 때 안전하게 리소스 해제
        LumosExtended.shutdown()
    }
}


@Composable
fun CameraFrameAnalyzerScreen() {
    val lumos = LumosExtended
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.GREEN)
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_START

                post {
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        // 1. 화면에 보여주기 위한 Preview UseCase
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(surfaceProvider)
                        }

                        // 2. 엔진에 먹여줄 프레임을 추출하는 ImageAnalysis UseCase
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            // 밀린 프레임은 버리고 항상 최신 프레임만 유지 (메모리 폭발, 지연 방지)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            // MediaPipe 변환을 쉽게 하기 위해 포맷을 RGBA로 설정
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()

                        imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            try {
                                LumosExtended.ingestExternalCameraFrame(imageProxy)
                            } catch (e: Exception) {
                                Log.e("CameraAnalyzer", "Frame Processing Error", e)
                            } finally {
                                // 7. 🔥 매우 중요: 이 프레임을 닫아주지 않으면 다음 프레임이 안 들어옵니다!
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        try {
                            // 기존에 바인딩된 UseCase를 모두 해제하고 새로 바인딩
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalyzer // Preview와 Analyzer를 동시에 등록
                            )
                        } catch (e: Exception) {
                            Log.e("CameraAnalyzer", "Use case binding failed", e)
                        }

                    }, ContextCompat.getMainExecutor(ctx))
                }
            }

            return@AndroidView previewView // AndroidView가 렌더링할 최종 뷰 반환
        }
    )
}