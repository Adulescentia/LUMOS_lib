package io.github.adulescentia.LUMOS_lib;

import android.content.Context;
import android.media.Image;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.framework.image.MediaImageBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CameraX bridge that feeds live camera frames into the MediaPipe-based LUMOS engine.
 */
class MediaPipeCameraController {
    private static final String TAG = "MediaPipeCameraController";

    private final LumosImpl lumos;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean running = false;

    MediaPipeCameraController(@NonNull LumosImpl lumos) {
        this.lumos = lumos;
    }

    synchronized void start(@NonNull Context context, @NonNull LifecycleOwner lifecycleOwner) {
        start(context, lifecycleOwner, CameraSelector.LENS_FACING_BACK);
    }

    synchronized void start(@NonNull Context context,
                            @NonNull LifecycleOwner lifecycleOwner,
                            int lensFacing) {
        stop();
        this.lensFacing = lensFacing;
        this.running = true;
        this.cameraExecutor = Executors.newSingleThreadExecutor();

        Context appContext = context.getApplicationContext();
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(appContext);
        providerFuture.addListener(() -> {
            try {
                cameraProvider = providerFuture.get();
                bindUseCases(lifecycleOwner);
            } catch (Exception e) {
                running = false;
                LumosLog.e(TAG, "Failed to start CameraX pipeline", e);
            }
        }, ContextCompat.getMainExecutor(appContext));
    }

    synchronized void stop() {
        running = false;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdownNow();
            cameraExecutor = null;
        }
    }

    synchronized boolean isRunning() {
        return running;
    }

    private void bindUseCases(@NonNull LifecycleOwner lifecycleOwner) {
        if (!running || cameraProvider == null || cameraExecutor == null) return;

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis);
    }

    @ExperimentalGetImage
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        try {
            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) return;

            MPImage mpImage = new MediaImageBuilder(mediaImage).build();
            long timestampMs = SystemClock.uptimeMillis();
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            lumos.ingestExternalCameraFrame(mpImage, timestampMs, rotationDegrees);
        } catch (Exception e) {
            LumosLog.e(TAG, "Failed to analyze camera frame", e);
        } finally {
            imageProxy.close();
        }
    }
}
