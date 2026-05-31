package io.github.adulescentia.LUMOS_lib;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;

/**
 * External camera frame -> MediaPipe Pose/Gesture -> arm vector and gesture conversion pipeline.
 */
public class MediaPipeArmVectorEngine {

    public interface VectorResultListener {
        void onArmVectorReady(@NonNull Vector3f armVector, @Nullable PoseLandmarkerResult rawResult, long timestampMs);
    }

    public interface GestureResultListener {
        void onGestureReady(@NonNull GestureStateManager.Gesture gesture,
                            float wristY,
                            @Nullable GestureRecognizerResult rawResult,
                            long timestampMs);
    }

    private final PrecisionZ precisionZ = new PrecisionZ();
    private PoseLandmarker poseLandmarker;
    private GestureRecognizer gestureRecognizer;
    private VectorResultListener vectorListener;
    private GestureResultListener gestureListener;

    public void initialize(@NonNull Context context, @NonNull String poseModelAssetPath) {
        initialize(context, poseModelAssetPath, null);
    }

    public void initialize(@NonNull Context context,
                           @NonNull String poseModelAssetPath,
                           @Nullable String gestureModelAssetPath) {
        initializePoseLandmarker(context, poseModelAssetPath);
        if (gestureModelAssetPath != null && !gestureModelAssetPath.trim().isEmpty()) {
            initializeGestureRecognizer(context, gestureModelAssetPath);
        }
    }

    public void setVectorResultListener(@Nullable VectorResultListener listener) {
        this.vectorListener = listener;
    }

    public void setGestureResultListener(@Nullable GestureResultListener listener) {
        this.gestureListener = listener;
    }

    public void processFrame(@NonNull MPImage frame, long timestampMs) {
        if (poseLandmarker == null) {
            throw new IllegalStateException("MediaPipeArmVectorEngine is not initialized");
        }
        poseLandmarker.detectAsync(frame, timestampMs);
        if (gestureRecognizer != null) {
            gestureRecognizer.recognizeAsync(frame, timestampMs);
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

    private void initializePoseLandmarker(@NonNull Context context, @NonNull String poseModelAssetPath) {
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath(poseModelAssetPath)
                .build();

        PoseLandmarker.PoseLandmarkerOptions options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::onPoseResult)
                .build();

        poseLandmarker = PoseLandmarker.createFromOptions(context, options);
    }

    private void initializeGestureRecognizer(@NonNull Context context, @NonNull String gestureModelAssetPath) {
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath(gestureModelAssetPath)
                .build();

        GestureRecognizer.GestureRecognizerOptions options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::onGestureResult)
                .build();

        gestureRecognizer = GestureRecognizer.createFromOptions(context, options);
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
            gestureListener.onGestureReady(gesture, wristY, result, result.timestampMs());
        }
    }

    @NonNull
    private static GestureStateManager.Gesture mapGesture(@Nullable GestureRecognizerResult result) {
        Category bestCategory = findBestGestureCategory(result);
        if (bestCategory == null || bestCategory.categoryName() == null) {
            return GestureStateManager.Gesture.UNDEF;
        }

        String name = bestCategory.categoryName().toLowerCase(Locale.US);
        if (name.contains("closed_fist") || name.contains("fist")) {
            return GestureStateManager.Gesture.FIST;
        }
        if (name.contains("open_palm") || name.contains("palm")) {
            return GestureStateManager.Gesture.PALM;
        }
        if (name.contains("pointing_up") || name.contains("pointing") || name.contains("one_finger")) {
            return GestureStateManager.Gesture.ONE_FINGER;
        }
        if (name.contains("victory") || name.contains("v_sign") || name.contains("peace")) {
            return GestureStateManager.Gesture.V_SIGN;
        }
        return GestureStateManager.Gesture.UNDEF;
    }

    @Nullable
    private static Category findBestGestureCategory(@Nullable GestureRecognizerResult result) {
        if (result == null || result.gestures() == null || result.gestures().isEmpty()) {
            return null;
        }

        Category bestCategory = null;
        float bestScore = -Float.MAX_VALUE;
        for (List<Category> handGestures : result.gestures()) {
            if (handGestures == null) continue;
            for (Category category : handGestures) {
                if (category != null && category.score() > bestScore) {
                    bestScore = category.score();
                    bestCategory = category;
                }
            }
        }
        return bestCategory;
    }

    private static float extractWristY(@Nullable GestureRecognizerResult result) {
        if (result == null || result.landmarks() == null || result.landmarks().isEmpty()) {
            return 0.5f;
        }
        List<NormalizedLandmark> firstHand = result.landmarks().get(0);
        if (firstHand == null || firstHand.isEmpty()) {
            return 0.5f;
        }
        return firstHand.get(0).y();
    }
}
