package io.github.adulescentia.LUMOS_lib;

import android.util.Log;

public class GestureStateManager {
    private static final String TAG = "GestureStateManager";

    public enum Gesture {
        FIST,
        PALM,
        ONE_FINGER,
        V_SIGN,
        UNDEF
    }

    public interface ActionListener {
        void onDeviceSelectionToggled(boolean isSelected);
        void onDevicePowerToggled();
        void onDeviceModeApplied(float modeValue);
    }

    private Gesture prevGesture = Gesture.UNDEF;
    private boolean isDeviceSelected = false;
    private boolean isTrackingModeActive = false;

    private float baseWristY = 0.0f;
    private float currentModeValue = 0.0f;
    private float sensitivity = 1.0f;

    private ActionListener actionListener;

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }

    public boolean isDeviceSelected() {
        return isDeviceSelected;
    }

    public boolean isTrackingModeActive() {
        return isTrackingModeActive;
    }

    public void update(Gesture currentGesture, float currentWristY) {
        boolean isComingFromReadyState = (prevGesture == Gesture.FIST || prevGesture == Gesture.UNDEF);

        switch (currentGesture) {
            case ONE_FINGER:
                if (isComingFromReadyState && prevGesture != Gesture.ONE_FINGER) {
                    isDeviceSelected = !isDeviceSelected;
                    if (isDeviceSelected) {
                        Log.d(TAG, "🎯 [EVENT] 디바이스가 선택(조준 완료) 되었습니다.");
                        isTrackingModeActive = false;
                    } else {
                        Log.d(TAG, "❌ [EVENT] 디바이스 선택이 해제되었습니다.");
                    }
                    if (actionListener != null) {
                        actionListener.onDeviceSelectionToggled(isDeviceSelected);
                    }
                }
                break;

            case PALM:
                if (prevGesture == Gesture.FIST) {
                    Log.d(TAG, "🔌 [EVENT] 디바이스 전원(On/Off) 토글 명령이 실행되었습니다.");
                    if (actionListener != null) {
                        actionListener.onDevicePowerToggled();
                    }
                }
                break;

            case V_SIGN:
                if (isComingFromReadyState && prevGesture != Gesture.V_SIGN) {
                    if (isDeviceSelected) {
                        isTrackingModeActive = !isTrackingModeActive;

                        if (isTrackingModeActive) {
                            baseWristY = currentWristY;
                            currentModeValue = 0.0f;
                            Log.d(TAG, "📏 [MODE] 모드 조절 활성화. 기준 Y: " + baseWristY + " (감도: " + sensitivity + ")");
                        } else {
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

        if (isTrackingModeActive) {
            float deltaY = baseWristY - currentWristY;
            currentModeValue = deltaY * sensitivity;
            Log.d(TAG, "🔄 [TRACKING] 실시간 높이 변화량 추적 중... deltaY(순수): " + deltaY + " -> 적용값(mode): " + currentModeValue);
        }

        prevGesture = currentGesture;
    }

    private void applyDeviceMode(float finalModeValue) {
        if (actionListener != null) {
            actionListener.onDeviceModeApplied(finalModeValue);
        }
        System.out.println("LUMOS Core Engine -> 디바이스에 적용 완료: " + finalModeValue);
    }
}
