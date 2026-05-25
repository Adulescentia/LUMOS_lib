package io.github.adulescentia.LUMOS_lib;

import android.content.Context;
import com.google.mediapipe.framework.image.MPImage;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

/**more than one instance of it cannot be in the same application*/
public class Lumos {
    static User user = new User();
    static ArrayList<Device> devices = new ArrayList<>();
    static Detector detector = new Detector(devices, user.getUserCoordinate());

    private final MediaPipeArmVectorEngine armVectorEngine = new MediaPipeArmVectorEngine();
    private final GestureStateManager gestureStateManager = new GestureStateManager();

    private Consumer<Result> externalResultConsumer;

    private Device selectedDevice;
    private final Result latestResult = new Result();

    private final Context appContext;
    private String poseModelAssetPath = "pose_landmarker_full.task";

    public Lumos(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        wireInternalPipelines();
    }

    private void wireInternalPipelines() {
        armVectorEngine.setVectorResultListener((armVector, rawResult, timestampMs) -> {
            latestResult.update(armVector, rawResult, timestampMs);
            detector.updateAllDevices(user.getUserCoordinate());

            if (gestureStateManager.isDeviceSelected()) {
                selectedDevice = detector.getDevice(armVector);
            } else {
                selectedDevice = null;
            }

            if (externalResultConsumer != null) {
                externalResultConsumer.accept(latestResult.clone());
            }
        });

        gestureStateManager.setActionListener(new GestureStateManager.ActionListener() {
            @Override
            public void onDeviceSelectionToggled(boolean isSelected) {
                if (!isSelected) selectedDevice = null;
            }

            @Override
            public void onDevicePowerToggled() {
                if (selectedDevice != null) {
                    System.out.println("LUMOS Core Engine -> 전원 토글 대상: " + selectedDevice.getName());
                }
            }

            @Override
            public void onDeviceModeApplied(float modeValue) {
                if (selectedDevice != null) {
                    System.out.println("LUMOS Core Engine -> 모드 반영 대상: " + selectedDevice.getName() + " / value: " + modeValue);
                }
            }
        });
    }

    public void setPoseModelAssetPath(@NonNull String poseModelAssetPath) {
        this.poseModelAssetPath = poseModelAssetPath;
    }

    @Nullable
    public Device registerDevice(String name, Vector3f pos) {
        try {
            Device newDevice = new Device(name, pos);
            devices.add(newDevice);
            detector.updateAllDevices(user.getUserCoordinate());
            return newDevice;
        } catch (Exception e) {
            return null;
        }
    }

    public Collection<Device> getDeviceList() {
        return devices;
    }

    public void registerExternalResultChannel(Consumer<Result> resultConsumer) {
        this.externalResultConsumer = resultConsumer;
    }

    public void initialize() {
        armVectorEngine.initialize(appContext, poseModelAssetPath);
    }

    public void startIoTControlProcess() {
        // no-op: host app drives this library by calling ingestExternalCameraFrame(...).
    }

    /**
     * host app entry-point: pass external camera frame directly to LUMOS.
     */
    public void ingestExternalCameraFrame(@NonNull MPImage mpImage, long timestampMs) {
        armVectorEngine.processFrame(mpImage, timestampMs);
    }

    /**
     * compatibility API: wrapper object version.
     */
    public void ingestExternalCameraFrame(@NonNull CameraFrame frame) {
        ingestExternalCameraFrame(frame.getMpImage(), frame.getTimestampMs());
    }

    @NonNull
    public Result getLatestResultSnapshot() {
        return latestResult.clone();
    }

    public void shutdown() {
        armVectorEngine.close();
    }

    public void updateGesture(GestureStateManager.Gesture gesture, float wristY) {
        gestureStateManager.update(gesture, wristY);
    }

    public void setGestureSensitivity(float sensitivity) {
        gestureStateManager.setSensitivity(sensitivity);
    }

    @Nullable
    public Device getSelectedDevice() {
        return selectedDevice;
    }

    public static class CameraFrame {
        private final MPImage mpImage;
        private final long timestampMs;

        public CameraFrame(@NonNull MPImage mpImage, long timestampMs) {
            this.mpImage = mpImage;
            this.timestampMs = timestampMs;
        }

        @NonNull
        public MPImage getMpImage() {
            return mpImage;
        }

        public long getTimestampMs() {
            return timestampMs;
        }
    }
}
