package io.github.adulescentia.LUMOS_lib;


import android.content.Context;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;
import androidx.lifecycle.LifecycleOwner;

import com.google.mediapipe.framework.image.MPImage;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * LUMOS 코어 라이브러리의 외부 통신 및 제어를 담당하는 인터페이스.
 * UI 레벨이나 ViewModel에서는 구체 클래스(LumosImpl) 대신 이 인터페이스를 통해 엔진과 상호작용합니다.
 */
public interface LumosInterface {

    public GestureStateManager gestureStateManager = new GestureStateManager();

    // ========================================================================
    // 디바이스 관리 (Device Management)
    // ========================================================================

    @Nullable
    Device registerDevice(double x, double y, double z,
                          @NonNull String deviceName,
                          @NonNull String deviceType);

    Collection<Device> getDeviceList();

    @NonNull
    String[] serializeDevices();

    @NonNull
    Device[] deserializeDevices(@NonNull String[] serializedDevices);


    // ========================================================================
    // 콜백 및 채널 등록 (Callbacks & Channels)
    // ========================================================================

    void registerUIUpdater(Consumer<Image> uiUpdateCallback);

    void registerExternalResultChannel(Consumer<Result> resultConsumer);


    // ========================================================================
    // 라이프사이클 및 초기화 (Lifecycle & Initialization)
    // ========================================================================

    /** 하위호환 초기화 */
    void initialize();

    /** assets 기본 경로의 MediaPipe 모델 2개로 실사용 초기화 */
    void initialize(@NonNull Context context);

    /** 팔 방향만 사용하는 기존 호환 초기화 */
    void initialize(@NonNull Context context, @NonNull String poseModelAssetPath);

    /** 실사용 초기화 */
    void initialize(@NonNull Context context,
                    @NonNull String poseModelAssetPath,
                    @Nullable String gestureModelAssetPath);

    void shutdown();


    // ========================================================================
    // 프로세스 제어 (Process Control)
    // ========================================================================

    void startIoTControlProcess();

    void startCameraControlProcess(@NonNull Context context, @NonNull LifecycleOwner lifecycleOwner);

    void startCameraControlProcess(@NonNull Context context,
                                   @NonNull LifecycleOwner lifecycleOwner,
                                   int lensFacing);

    void stopCameraControlProcess();

    boolean isCameraControlProcessRunning();


    // ========================================================================
    // 외부 데이터 주입 및 상태 업데이트 (Data Ingestion & State Updates)
    // ========================================================================

    void ingestExternalCameraFrame(@NonNull MPImage mpImage, long timestampMs);

    void ingestExternalCameraFrame(@NonNull MPImage mpImage, long timestampMs, int rotationDegrees);

    void updateGesture(@NonNull GestureStateManager.Gesture gesture, float wristY);
    void ingestExternalCameraFrame(@NonNull ImageProxy img);
    @NonNull
    Result getLatestResultSnapshot();
}