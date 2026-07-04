package io.github.adulescentia.LUMOS_lib;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import org.joml.Vector3f;

/**
 * Camera frame -> MediaPipe Pose/Gesture -> LUMOS arm vector and gesture pipeline.
 */
public class MediaPipeArmVectorEngine {

    public interface VectorResultListener {
        void onArmVectorReady(@NonNull Vector3f armVector, @Nullable PoseLandmarkerResult rawResult, long timestampMs);
    }

    public interface GestureResultListener {
        void onGestureReady(@NonNull GestureStateManager.Gesture gesture, float wristY, long timestampMs);
    }

    private static final float MIN_GESTURE_SCORE = 0.50f;

    private final PrecisionZ precisionZ = new PrecisionZ();
    private PoseLandmarker poseLandmarker;
    private GestureRecognizer gestureRecognizer;
    private VectorResultListener vectorListener;
    private GestureResultListener gestureListener;

    public void initialize(@NonNull Context context,
                           @NonNull String poseModelAssetPath,
                           @Nullable String gestureModelAssetPath) {
        BaseOptions poseBaseOptions = BaseOptions.builder()
                .setModelAssetPath(poseModelAssetPath)
                .build();

        PoseLandmarker.PoseLandmarkerOptions poseOptions = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(poseBaseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setResultListener(this::onPoseResult)
                .build();

        poseLandmarker = PoseLandmarker.createFromOptions(context, poseOptions);

        if (gestureModelAssetPath != null && !gestureModelAssetPath.trim().isEmpty()) {
            BaseOptions gestureBaseOptions = BaseOptions.builder()
                    .setModelAssetPath(gestureModelAssetPath)
                    .build();

            GestureRecognizer.GestureRecognizerOptions gestureOptions = GestureRecognizer.GestureRecognizerOptions.builder()
                    .setBaseOptions(gestureBaseOptions)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setNumHands(1)
                    .setResultListener(this::onGestureResult)
                    .build();

            gestureRecognizer = GestureRecognizer.createFromOptions(context, gestureOptions);
        }
    }

    public void setVectorResultListener(@Nullable VectorResultListener listener) {
        this.vectorListener = listener;
    }

    public void setGestureResultListener(@Nullable GestureResultListener listener) {
        this.gestureListener = listener;
    }

    public void processFrame(@NonNull MPImage frame, long timestampMs) {
        processFrame(frame, timestampMs, 0);
    }

    public void processFrame(@NonNull MPImage frame, long timestampMs, int rotationDegrees) {
        if (poseLandmarker == null) {
            throw new IllegalStateException("MediaPipeArmVectorEngine is not initialized");
        }
        ImageProcessingOptions imageOptions = ImageProcessingOptions.builder()
                .setRotationDegrees(normalizeRotation(rotationDegrees))
                .build();
        poseLandmarker.detectAsync(frame, imageOptions, timestampMs);
        if (gestureRecognizer != null) {
            gestureRecognizer.recognizeAsync(frame, imageOptions, timestampMs);
        }
    }

    public void close() {
        if (poseLandmarker != null) {
            poseLandmarker.close();
            poseLandmarker = null;
        }
        if (gestureRecognizer != null) {
            gestureRecognizer.close();
            gestureRecognizer = null;
        }
        precisionZ.reset();
    }

    private void onPoseResult(PoseLandmarkerResult result, MPImage inputImage) {
        Vector3f armVector = precisionZ.calculateFastArmVector(result);
        if (vectorListener != null) {
            vectorListener.onArmVectorReady(new Vector3f(armVector), result, result.timestampMs());
        }
    }

    private void onGestureResult(GestureRecognizerResult result, MPImage inputImage) {
        GestureStateManager.Gesture gesture = mapGesture(result);
        float wristY = extractWristY(result);
        if (gestureListener != null) {
            gestureListener.onGestureReady(gesture, wristY, result.timestampMs());
        }
    }

    @NonNull
    private static GestureStateManager.Gesture mapGesture(@Nullable GestureRecognizerResult result) {
        if (result == null || result.gestures().isEmpty() || result.gestures().get(0).isEmpty()) {
            return GestureStateManager.Gesture.UNDEF;
        }

        Category category = result.gestures().get(0).get(0);
        if (category.score() < MIN_GESTURE_SCORE) {
            return GestureStateManager.Gesture.UNDEF;
        }

        String name = category.categoryName();
        if ("Closed_Fist".equals(name)) return GestureStateManager.Gesture.FIST;
        if ("Open_Palm".equals(name)) return GestureStateManager.Gesture.PALM;
        if ("Pointing_Up".equals(name)) return GestureStateManager.Gesture.ONE_FINGER;
        if ("Victory".equals(name)) return GestureStateManager.Gesture.V_SIGN;
        return GestureStateManager.Gesture.UNDEF;
    }

    private static float extractWristY(@Nullable GestureRecognizerResult result) {
        if (result == null || result.landmarks().isEmpty() || result.landmarks().get(0).isEmpty()) {
            return 0.0f;
        }
        return result.landmarks().get(0).get(0).y();
    }

    private static int normalizeRotation(int rotationDegrees) {
        int normalized = rotationDegrees % 360;
        if (normalized < 0) normalized += 360;
        return normalized;
    }
}
