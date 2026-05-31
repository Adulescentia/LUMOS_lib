package io.github.adulescentia.LUMOS_lib;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import org.joml.Vector3f;

/**
 * External camera frame -> MediaPipe Pose -> arm vector conversion pipeline.
 */
public class MediaPipeArmVectorEngine {

    public interface VectorResultListener {
        void onArmVectorReady(@NonNull Vector3f armVector, @Nullable PoseLandmarkerResult rawResult, long timestampMs);
    }

    private final PrecisionZ precisionZ = new PrecisionZ();
    private PoseLandmarker poseLandmarker;
    private VectorResultListener listener;

    public void initialize(@NonNull Context context, @NonNull String modelAssetPath) {
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelAssetPath)
                .build();

        PoseLandmarker.PoseLandmarkerOptions options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::onResult)
                .build();

        poseLandmarker = PoseLandmarker.createFromOptions(context, options);
    }

    public void setVectorResultListener(@Nullable VectorResultListener listener) {
        this.listener = listener;
    }

    public void processFrame(@NonNull MPImage frame, long timestampMs) {
        if (poseLandmarker == null) {
            throw new IllegalStateException("MediaPipeArmVectorEngine is not initialized");
        }
        poseLandmarker.detectAsync(frame, timestampMs);
    }

    public void close() {
        if (poseLandmarker != null) {
            poseLandmarker.close();
            poseLandmarker = null;
        }
        precisionZ.reset();
    }

    private void onResult(PoseLandmarkerResult result, MPImage inputImage) {
        Vector3f armVector = precisionZ.calculateFastArmVector(result);
        if (listener != null) {
            listener.onArmVectorReady(new Vector3f(armVector), result, result.timestampMs());
        }
    }
}
