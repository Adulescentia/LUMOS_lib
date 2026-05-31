package io.github.adulescentia.LUMOS_lib;

import android.content.Context;
import android.media.Image;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.lifecycle.LifecycleOwner;

import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** 실사용 라이브러리 형태의 LUMOS 코어 */
public class Lumos {
    private static final String TAG = "Lumos";
    private static volatile Lumos instance;
    public static final String DEFAULT_POSE_MODEL_ASSET_PATH = "pose_landmarker_full.task";
    public static final String DEFAULT_GESTURE_MODEL_ASSET_PATH = "gesture_recognizer.task";

    private static final String DEVICE_SERIALIZATION_VERSION = "LUMOS_DEVICE_V1";
    private static final char DEVICE_FIELD_SEPARATOR = '|';
    private static final int DEVICE_FIELD_COUNT = 7;

    private final List<Device> devices = new CopyOnWriteArrayList<>();
    private final GestureStateManager gestureStateManager = new GestureStateManager();
    private final MediaPipeArmVectorEngine armVectorEngine = new MediaPipeArmVectorEngine();
    private final MediaPipeCameraController cameraController = new MediaPipeCameraController(this);

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
                LumosLog.d(TAG, "selection=" + isSelected);
                pendingCommandType = Result.CommandType.DEVICE_SELECTION_TOGGLED;
                pendingCommandDetail = "selected=" + isSelected;
            }
            @Override public void onDevicePowerToggled() {
                LumosLog.d(TAG, "power toggled");
                pendingCommandType = Result.CommandType.DEVICE_POWER_TOGGLED;
                pendingCommandDetail = "power_toggle";
            }
            @Override public void onDeviceModeApplied(float modeValue) {
                LumosLog.d(TAG, "mode=" + modeValue);
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
            ensureInitialized("registerDevice");
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
            LumosLog.d(TAG, "registerDevice -> id=" + id + ", name=" + deviceName + ", type=" + deviceType
                    + ", pos=" + d.getPosition());
            return d;
        } catch (LumosException e) {
            throw e;
        } catch (Exception e) {
            LumosLog.e(TAG, "registerDevice failed", e);
            throw new LumosException("registerDevice failed", e);
        }
    }

    public Collection<Device> getDeviceList() {
        return new ArrayList<>(devices);
    }


    /**
     * 현재 Lumos에 등록된 디바이스 목록을 문자열 배열로 직렬화합니다.
     *
     * <p>각 문자열은 내부 버전, id, name, type, x, y, z를 포함합니다.
     * name/type/id에 구분자나 줄바꿈이 들어와도 복원할 수 있도록 자체 escape를 적용합니다.</p>
     *
     * @return 등록된 디바이스를 저장/전송 가능한 문자열 배열로 변환한 값
     */
    @NonNull
    public String[] serializeDevices() {
        List<String> serialized = new ArrayList<>();
        for (Device device : devices) {
            Vector3f position = device.getPosition();
            serialized.add(DEVICE_SERIALIZATION_VERSION
                    + DEVICE_FIELD_SEPARATOR + escapeDeviceField(device.getId())
                    + DEVICE_FIELD_SEPARATOR + escapeDeviceField(device.getName())
                    + DEVICE_FIELD_SEPARATOR + escapeDeviceField(device.getType())
                    + DEVICE_FIELD_SEPARATOR + position.x
                    + DEVICE_FIELD_SEPARATOR + position.y
                    + DEVICE_FIELD_SEPARATOR + position.z);
        }
        return serialized.toArray(new String[0]);
    }

    /**
     * 문자열 배열로 저장된 디바이스 목록을 Device 배열로 역직렬화하고 현재 Lumos 디바이스 목록에 반영합니다.
     *
     * <p>모든 문자열을 먼저 검증/파싱한 뒤 한 번에 교체하므로, 중간에 오류가 나도 기존 목록은 유지됩니다.</p>
     *
     * @param serializedDevices {@link #serializeDevices()}가 반환한 문자열 배열
     * @return 복원된 Device 배열
     */
    @NonNull
    public synchronized Device[] deserializeDevices(@NonNull String[] serializedDevices) {
        ensureInitialized("deserializeDevices");
        if (serializedDevices == null) {
            throw new InvalidInputErr("serializedDevices must not be null");
        }

        List<Device> parsedDevices = new ArrayList<>();
        int nextSequence = sequence;
        for (int i = 0; i < serializedDevices.length; i++) {
            String serializedDevice = serializedDevices[i];
            if (serializedDevice == null || serializedDevice.trim().isEmpty()) {
                throw new InvalidInputErr("serializedDevices[" + i + "] must not be null/blank");
            }

            String[] fields = splitEscapedDeviceFields(serializedDevice);
            if (fields.length != DEVICE_FIELD_COUNT) {
                throw new InvalidInputErr("serializedDevices[" + i + "] has invalid field count");
            }
            if (!DEVICE_SERIALIZATION_VERSION.equals(fields[0])) {
                throw new InvalidInputErr("serializedDevices[" + i + "] has unsupported version: " + fields[0]);
            }

            String id = unescapeDeviceField(fields[1]);
            String name = unescapeDeviceField(fields[2]);
            String type = unescapeDeviceField(fields[3]);
            if (id.trim().isEmpty()) throw new InvalidInputErr("Device id must not be blank");
            if (name.trim().isEmpty()) throw new InvalidInputErr("Device name must not be blank");
            if (type.trim().isEmpty()) throw new InvalidInputErr("Device type must not be blank");

            float x = parseFiniteFloat(fields[4], "x", i);
            float y = parseFiniteFloat(fields[5], "y", i);
            float z = parseFiniteFloat(fields[6], "z", i);

            Device device = new Device(id, name, type, new Vector3f(x, y, z));
            device.updateRelativeCoordinate(currentPosition);
            parsedDevices.add(device);
            nextSequence = Math.max(nextSequence, getNextSequenceAfter(id));
        }

        devices.clear();
        devices.addAll(parsedDevices);
        sequence = Math.max(sequence, nextSequence);
        return parsedDevices.toArray(new Device[0]);
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
        LumosLog.d(TAG, "initialize() called without context. MediaPipe disabled until initialize(context) called.");
        initialized = true;
    }

    /** assets 기본 경로의 MediaPipe 모델 2개로 실사용 초기화 */
    public void initialize(@NonNull Context context) {
        initialize(context, DEFAULT_POSE_MODEL_ASSET_PATH, DEFAULT_GESTURE_MODEL_ASSET_PATH);
    }

    /** 팔 방향만 사용하는 기존 호환 초기화 */
    public void initialize(@NonNull Context context, @NonNull String poseModelAssetPath) {
        initialize(context, poseModelAssetPath, null);
    }

    /** 실사용 초기화 */
    public void initialize(@NonNull Context context,
                           @NonNull String poseModelAssetPath,
                           @Nullable String gestureModelAssetPath) {
        armVectorEngine.setVectorResultListener(this::onArmVectorReady);
        armVectorEngine.setGestureResultListener(this::onGestureReady);
        armVectorEngine.initialize(context.getApplicationContext(), poseModelAssetPath, gestureModelAssetPath);
        initialized = true;
        LumosLog.d(TAG, "MediaPipe initialized");
    }

    public void startIoTControlProcess() {
        ensureInitialized("startIoTControlProcess");
        // host-driven: ingestExternalCameraFrame() 또는 startCameraControlProcess() 호출로 실제 처리 시작
    }

    public void startCameraControlProcess(@NonNull Context context, @NonNull LifecycleOwner lifecycleOwner) {
        startCameraControlProcess(context, lifecycleOwner, CameraSelector.LENS_FACING_BACK);
    }

    public void startCameraControlProcess(@NonNull Context context,
                                          @NonNull LifecycleOwner lifecycleOwner,
                                          int lensFacing) {
        ensureInitialized("startCameraControlProcess");
        if (context == null) throw new InvalidInputErr("context must not be null");
        if (lifecycleOwner == null) throw new InvalidInputErr("lifecycleOwner must not be null");
        cameraController.start(context, lifecycleOwner, lensFacing);
    }

    public void stopCameraControlProcess() {
        cameraController.stop();
    }

    public boolean isCameraControlProcessRunning() {
        return cameraController.isRunning();
    }

    public void ingestExternalCameraFrame(@NonNull MPImage mpImage, long timestampMs) {
        ingestExternalCameraFrame(mpImage, timestampMs, 0);
    }

    public void ingestExternalCameraFrame(@NonNull MPImage mpImage, long timestampMs, int rotationDegrees) {
        ensureInitialized("ingestExternalCameraFrame");
        if (mpImage == null) throw new InvalidInputErr("mpImage must not be null");
        armVectorEngine.processFrame(mpImage, timestampMs, rotationDegrees);
    }

    public void updateGesture(@NonNull GestureStateManager.Gesture gesture, float wristY) {
        ensureInitialized("updateGesture");
        if (gesture == null) throw new InvalidInputErr("gesture must not be null");
        gestureStateManager.update(gesture, wristY);
    }

    private void ensureInitialized(@NonNull String apiName) {
        if (!initialized) {
            throw new NotInitializedErr(apiName + " requires initialize() before use");
        }
    }

    private void onGestureReady(@NonNull GestureStateManager.Gesture gesture, float wristY, long ts) {
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


    private static String escapeDeviceField(String field) {
        if (field == null) throw new InvalidInputErr("Device field must not be null");
        StringBuilder escaped = new StringBuilder(field.length());
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            switch (c) {
                case '\\': escaped.append("\\\\"); break;
                case '|': escaped.append("\\|"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                default: escaped.append(c); break;
            }
        }
        return escaped.toString();
    }

    private static String unescapeDeviceField(String field) {
        StringBuilder unescaped = new StringBuilder(field.length());
        boolean escaping = false;
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (escaping) {
                switch (c) {
                    case 'n': unescaped.append('\n'); break;
                    case 'r': unescaped.append('\r'); break;
                    case '|': unescaped.append('|'); break;
                    case '\\': unescaped.append('\\'); break;
                    default: unescaped.append(c); break;
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                unescaped.append(c);
            }
        }
        if (escaping) unescaped.append('\\');
        return unescaped.toString();
    }

    private static String[] splitEscapedDeviceFields(String serializedDevice) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < serializedDevice.length(); i++) {
            char c = serializedDevice.charAt(i);
            if (escaping) {
                current.append('\\').append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == DEVICE_FIELD_SEPARATOR) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (escaping) current.append('\\');
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private static float parseFiniteFloat(String value, String axis, int index) {
        try {
            float parsed = Float.parseFloat(value);
            if (Float.isNaN(parsed) || Float.isInfinite(parsed)) {
                throw new InvalidInputErr("serializedDevices[" + index + "] " + axis + " must be finite");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new InvalidInputErr("serializedDevices[" + index + "] " + axis + " is not a valid number", e);
        }
    }

    private static int getNextSequenceAfter(String id) {
        if (id == null || !id.startsWith("DEV_")) return 1;
        try {
            return Integer.parseInt(id.substring(4)) + 1;
        } catch (NumberFormatException e) {
            return 1;
        }
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
        cameraController.stop();
        armVectorEngine.close();
        initialized = false;
    }
}
