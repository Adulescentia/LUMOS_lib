package io.github.adulescentia.LUMOS_lib;

import android.content.Context;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** 실사용 라이브러리 형태의 LUMOS 코어 */
public class Lumos {
    private static final String TAG = "Lumos";
    private static volatile Lumos instance;

    private final List<Device> devices = new CopyOnWriteArrayList<>();
    private final Random random = new Random();
    private final GestureStateManager gestureStateManager = new GestureStateManager();
    private final MediaPipeArmVectorEngine armVectorEngine = new MediaPipeArmVectorEngine();

    private Consumer<Image> uiUpdater;
    private Consumer<Result> resultChannel;

    private final Vector3f currentPosition = new Vector3f(0, 0, 0);
    private final Vector3f cameraPos = new Vector3f(0, 0, 0);
    private final Result latestResult = new Result();
    private volatile Result.CommandType pendingCommandType = Result.CommandType.NONE;
    private volatile String pendingCommandDetail = "";

    private boolean initialized = false;
    private int sequence = 1;

    private Lumos() {
        gestureStateManager.setActionListener(new GestureStateManager.ActionListener() {
            @Override public void onDeviceSelectionToggled(boolean isSelected) {
                Log.d(TAG, "selection=" + isSelected);
                pendingCommandType = Result.CommandType.DEVICE_SELECTION_TOGGLED;
                pendingCommandDetail = "selected=" + isSelected;
            }
            @Override public void onDevicePowerToggled() {
                Log.d(TAG, "power toggled");
                pendingCommandType = Result.CommandType.DEVICE_POWER_TOGGLED;
                pendingCommandDetail = "power_toggle";
            }
            @Override public void onDeviceModeApplied(float modeValue) {
                Log.d(TAG, "mode=" + modeValue);
                pendingCommandType = Result.CommandType.DEVICE_MODE_APPLIED;
                pendingCommandDetail = "mode=" + modeValue;
            }
        });
    }

    public static Lumos getInstance() {
        if (instance == null) {
            synchronized (Lumos.class) {
                if (instance == null) instance = new Lumos();
            }
        }
        return instance;
    }

    @Nullable
    public synchronized Device registerDevice(double x, double y, double z,
                                              @NonNull String deviceName,
                                              @NonNull String deviceType) {
        try {
            if (!initialized) throw new NotInitializedErr("Call initialize() before registerDevice()");
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
                    || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
                throw new InvalidInputErr("Device coordinates must be finite numbers");
            }
            if (deviceName == null || deviceName.trim().isEmpty()) {
                throw new InvalidInputErr("deviceName must not be null/blank");
            }
            if (deviceType == null || deviceType.trim().isEmpty()) {
                throw new InvalidInputErr("deviceType must not be null/blank");
            }
            String id = String.format("DEV_%02d", sequence);
            Device d = new Device(id, deviceName, deviceType,
                    new Vector3f((float) x, (float) y, (float) z));
            d.updateRelativeCoordinate(currentPosition);
            devices.add(d);
            sequence++;
            Log.d(TAG, "registerDevice -> id=" + id + ", name=" + deviceName + ", type=" + deviceType
                    + ", pos=" + d.getPosition());
            return d;
        } catch (LumosException e) {
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "registerDevice failed", e);
            throw new LumosException("registerDevice failed", e);
        }
    }

    public Collection<Device> getDeviceList() {
        return new ArrayList<>(devices);
    }

    public void registerUIUpdater(Consumer<Image> uiUpdateCallback) {
        if (uiUpdateCallback == null) throw new InvalidInputErr("uiUpdateCallback must not be null");
        this.uiUpdater = uiUpdateCallback;
    }

    public void registerExternalResultChannel(Consumer<Result> resultConsumer) {
        if (resultConsumer == null) throw new InvalidInputErr("resultConsumer must not be null");
        this.resultChannel = resultConsumer;
    }

    /** 하위호환 초기화 */
    public void initialize() {
        Log.d(TAG, "initialize() called without context. MediaPipe disabled until initialize(context) called.");
        initialized = true;
    }

    /** 실사용 초기화 */
    public void initialize(@NonNull Context context, @NonNull String modelAssetPath) {
        armVectorEngine.setVectorResultListener(this::onArmVectorReady);
        armVectorEngine.initialize(context.getApplicationContext(), modelAssetPath);
        initialized = true;
        Log.d(TAG, "MediaPipe initialized");
    }

    public void startIoTControlProcess() {
        if (!initialized) throw new NotInitializedErr("Call initialize() first");
        // host-driven: ingestExternalCameraFrame() 호출로 실제 처리 시작
    }

    public void ingestExternalCameraFrame(@NonNull MPImage mpImage, long timestampMs) {
        if (!initialized) throw new NotInitializedErr("Call initialize() first");
        armVectorEngine.processFrame(mpImage, timestampMs);
    }

    public void updateGesture(@NonNull GestureStateManager.Gesture gesture, float wristY) {
        if (!initialized) throw new NotInitializedErr("Call initialize() first");
        if (gesture == null) throw new InvalidInputErr("gesture must not be null");
        gestureStateManager.update(gesture, wristY);
    }

    private void onArmVectorReady(@NonNull Vector3f armVector, @Nullable PoseLandmarkerResult raw, long ts) {
        for (Device d : devices) d.updateRelativeCoordinate(currentPosition);
        Device selected = findBestMatchedDevice(armVector);
        Result.CommandType cmd = pendingCommandType;
        String cmdDetail = pendingCommandDetail;
        latestResult.update(armVector, currentPosition, cameraPos, selected, cmd, cmdDetail);
        pendingCommandType = Result.CommandType.NONE;
        pendingCommandDetail = "";

        if (resultChannel != null) resultChannel.accept(latestResult.clone());
        if (uiUpdater != null) uiUpdater.accept(null);
    }

    @Nullable
    private Device findBestMatchedDevice(@NonNull Vector3f direction) {
        if (devices.isEmpty()) return null;
        Vector3f nd = new Vector3f(direction).normalize();
        float best = -Float.MAX_VALUE;
        Device match = null;
        for (Device d : devices) {
            Vector3f target = new Vector3f(d.getPosition()).sub(currentPosition);
            if (target.lengthSquared() == 0f) continue;
            float dot = nd.dot(target.normalize());
            if (dot > best) { best = dot; match = d; }
        }
        return match;
    }

    @NonNull
    public Result getLatestResultSnapshot() { return latestResult.clone(); }

    public void shutdown() {
        armVectorEngine.close();
        initialized = false;
    }
}
