package io.github.adulescentia.LUMOS_lib;

import android.util.Log;

public class GestureStateManager {
    private static final String TAG = "GestureStateManager";

    // 정의된 제스처 열거형
    public enum Gesture {
        FIST,       // 주먹
        PALM,       // 손바닥
        ONE_FINGER, // 한 손가락
        V_SIGN,     // 브이
        UNDEF       // 미정의 또는 인식 실패
    }

    // 시스템 제어 상태 변수
    private Gesture prevGesture = Gesture.UNDEF;
    private boolean isDeviceSelected = false;       // 디바이스 선택 토글 상태
    private boolean isTrackingModeActive = false;   // 디바이스 모드 관리(높이 트래킹) 활성화 상태

    private float baseWristY = 0.0f;       // 트래킹 시작 시점의 기준 손목 Y 좌표
    private float currentModeValue = 0.0f; // 최종 반영될 높이 변화 mode 값

    // [추가 변수] 높이 변화 모드 감도 제어 (기본값: 1.0f)
    // 1.0보다 크면 조금만 움직여도 변화량이 커지고, 작으면 부드럽고 미세하게 조절됩니다.
    private float sensitivity = 1.0f;


    /**
     * 매 프레임 미디어파이프 콜백으로부터 제스처와 손목 좌표를 받아 상태를 제어합니다.
     * @param currentGesture 현재 프레임에서 인식된 제스처
     * @param currentWristY  현재 프레임의 손목 Y 좌표 (실세계 worldLandmark 또는 정규화 좌표)
     */
    public void update(Gesture currentGesture, float currentWristY) {

        // [조건 체크] 오작동 방지: '직전 프레임이 주먹(FIST) 또는 UNDEF'였을 때만 상태 전이(Edge Trigger)를 허용
        boolean isComingFromReadyState = (prevGesture == Gesture.FIST || prevGesture == Gesture.UNDEF);

        switch (currentGesture) {
            case ONE_FINGER:
                // 1. 디바이스 선택 토글 (주먹/UNDEF -> 한 손가락 순간 취할 때만)
                if (isComingFromReadyState && prevGesture != Gesture.ONE_FINGER) {
                    isDeviceSelected = !isDeviceSelected; // 토글 시키기
                    if (isDeviceSelected) {
                        Log.d(TAG, "🎯 [EVENT] 디바이스가 선택(조준 완료) 되었습니다.");
                        isTrackingModeActive = false; // 신규 선택 시 트래킹은 초기화
                    } else {
                        Log.d(TAG, "❌ [EVENT] 디바이스 선택이 해제되었습니다.");
                    }
                }
                break;

            case PALM:
                // 2. 디바이스 켜기/끄기 토글 (주먹 -> 손바닥 순간 취할 때만)
                if (prevGesture == Gesture.FIST) {
                    Log.d(TAG, "🔌 [EVENT] 디바이스 전원(On/Off) 토글 명령이 실행되었습니다.");
                }
                break;

            case V_SIGN:
                // 3. 디바이스 모드 관리 (주먹/UNDEF -> 브이 순간 취할 때만 작동하는 토글)
                if (isComingFromReadyState && prevGesture != Gesture.V_SIGN) {
                    if (isDeviceSelected) {
                        isTrackingModeActive = !isTrackingModeActive; // 트래킹 활성화 여부 토글

                        if (isTrackingModeActive) {
                            baseWristY = currentWristY; // 높이 변화 측정을 위한 기준점(Anchor) 저장
                            currentModeValue = 0.0f;    // 리셋
                            Log.d(TAG, "📏 [MODE] 모드 조절 활성화. 기준 Y: " + baseWristY + " (감도: " + sensitivity + ")");
                        } else {
                            // 다시 브이를 해서 꺼지는 순간 변위 최종 확정 및 mode 반영
                            Log.d(TAG, "💾 [MODE] 모드 조절 비활성화. 최종 높이 변화 mode 반영: " + currentModeValue);
                            applyDeviceMode(currentModeValue);
                        }
                    } else {
                        Log.w(TAG, "⚠️ [WARN] 선택된 디바이스가 없어 모드 조절을 시작할 수 없습니다.");
                    }
                }
                break;

            case FIST:
            case UNDEF:
                break;
        }

        // [실시간 높이 추적 구현]
        if (isTrackingModeActive) {
            // 미디어파이프 카메라 좌표계 특성상 위로 올리면 Y가 감소하므로 (기준점 - 현재값)으로 변위 계산
            float deltaY = baseWristY - currentWristY;

            // 물리적인 변화량에 사용자가 설정한 '감도(sensitivity)' 가중치를 곱해 모드 값 연산
            currentModeValue = deltaY * sensitivity;

            Log.d(TAG, "🔄 [TRACKING] 실시간 높이 변화량 추적 중... deltaY(순수): " + deltaY + " -> 적용값(mode): " + currentModeValue);
        }

        // 현재 프레임의 제스처를 보관하여 다음 프레임에서 직전 상태(Edge)로 활용
        prevGesture = currentGesture;
    }

    /**
     * 최종 계산된 높이 변화 값을 실제 디바이스 코어 엔진에 반영하는 메서드
     */
    private void applyDeviceMode(float finalModeValue) {
        System.out.println("LUMOS Core Engine -> 디바이스에 적용 완료: " + finalModeValue);
    }
}