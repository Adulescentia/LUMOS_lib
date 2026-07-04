package io.github.adulescentia.LUMOS_lib;

import android.content.Context;
import android.media.Image;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mediapipe.framework.image.MPImage;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * LUMOS 라이브러리의 공개 인터페이스.
 * IoT 기기 등록, 제스처 인식 결과 수신 및 제어를 담당합니다.
 */
public interface Lumos {

    /**
     * 새로운 IoT 기기를 등록합니다.
     * @param x 기기의 X 좌표 (미터 단위)
     * @param y 기기의 Y 좌표 (미터 단위)
     * @param z 기기의 Z 좌표 (미터 단위)
     * @param deviceName 기기 이름
     * @param deviceType 기기 종류 (예: DISPLAY, LIGHT 등)
     * @return 등록된 Device 객체
     */
    @Nullable
    Device registerDevice(double x, double y, double z,
                          @NonNull String deviceName,
                          @NonNull String deviceType);

    /**
     * 현재 등록된 모든 기기 목록을 반환합니다.
     */
    Collection<Device> getDeviceList();

    /**
     * 등록된 기기 목록을 문자열 배열로 직렬화합니다.
     */
    @NonNull
    String[] serializeDevices();

    /**
     * 직렬화된 문자열 배열로부터 기기 목록을 복원합니다.
     */
    @NonNull
    Device[] deserializeDevices(@NonNull String[] serializedDevices);

    /**
     * UI 업데이트를 위한 콜백을 등록합니다.
     */
    void registerUIUpdater(Consumer<Image> uiUpdateCallback);

    /**
     * 외부에서 최종 결과를 수신하기 위한 채널을 등록합니다.
     */
    void registerExternalResultChannel(Consumer<Result> resultConsumer);

    /**
     * 라이브러리를 초기화합니다 (MediaPipe 제외).
     */
    void initialize();

    /**
     * 라이브러리를 초기화합니다.
     * @param context Android 컨텍스트
     * @param modelAssetPath MediaPipe 모델 파일의 에셋 경로
     */
    void initialize(@NonNull Context context, @NonNull String modelAssetPath);

    /**
     * IoT 제어 프로세스를 시작합니다.
     */
    void startIoTControlProcess();

    /**
     * 외부 카메라 프레임을 주입합니다.
     */
    void ingestExternalCameraFrame(@NonNull MPImage mpImage, long timestampMs);

    /**
     * 제스처 상태를 업데이트합니다.
     */
    void updateGesture(@NonNull GestureStateManager.Gesture gesture, float wristY);

    /**
     * 가장 최근의 처리 결과를 가져옵니다.
     */
    @NonNull
    Result getLatestResultSnapshot();

    /**
     * 라이브러리 리소스를 해제하고 종료합니다.
     */
    void shutdown();

    /**
     * Lumos 구현체 인스턴스를 가져옵니다.
     */
    static LumosImpl getInstance() {
        return LumosImpl.getInstance();
    }
}