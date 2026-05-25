package io.github.adulescentia.LUMOS_lib;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;

import org.joml.Vector3f;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Lumos lumos;
    private TextView statusBox;
    private PreviewView previewView;
    private float wristY = 0.5f;
    private String lastAction = "none";
    private ExecutorService cameraExecutor;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else {
                    lastAction = "camera permission denied";
                    updateStatus();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        statusBox = findViewById(R.id.statusBox);
        cameraExecutor = Executors.newSingleThreadExecutor();

        lumos = new Lumos(this);
        lumos.registerDevice("Lamp", new Vector3f(0f, 0f, 5f));
        lumos.registerDevice("Fan", new Vector3f(2f, 0f, 5f));
        lumos.registerDevice("TV", new Vector3f(-2f, 0f, 5f));

        lumos.initialize();
        lumos.registerExternalResultChannel(result -> runOnUiThread(this::updateStatus));

        wireGestureButtons();
        wireWristSeek();
        checkCameraPermissionAndStart();
        updateStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        lumos.shutdown();
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    try {
                        MPImage mpImage = toMpImage(image);
                        lumos.ingestExternalCameraFrame(mpImage, System.currentTimeMillis());
                    } catch (Exception e) {
                        lastAction = "frame ingest error: " + e.getClass().getSimpleName();
                    } finally {
                        image.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                lastAction = "camera started";
                runOnUiThread(this::updateStatus);
            } catch (Exception e) {
                lastAction = "camera start failed: " + e.getClass().getSimpleName();
                runOnUiThread(this::updateStatus);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @NonNull
    private MPImage toMpImage(@NonNull ImageProxy image) {
        Bitmap bitmap = previewView.getBitmap();
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(image.getImageInfo().getRotationDegrees());
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return new BitmapImageBuilder(rotated).build();
    }

    private void wireGestureButtons() {
        bindGesture(R.id.btnFist, GestureStateManager.Gesture.FIST, "gesture:FIST");
        bindGesture(R.id.btnPalm, GestureStateManager.Gesture.PALM, "gesture:PALM(power toggle)");
        bindGesture(R.id.btnOneFinger, GestureStateManager.Gesture.ONE_FINGER, "gesture:ONE_FINGER(select toggle)");
        bindGesture(R.id.btnVSign, GestureStateManager.Gesture.V_SIGN, "gesture:V_SIGN(mode toggle)");
        bindGesture(R.id.btnUndef, GestureStateManager.Gesture.UNDEF, "gesture:UNDEF");
    }

    private void bindGesture(int btnId, GestureStateManager.Gesture gesture, String actionLabel) {
        Button btn = findViewById(btnId);
        btn.setOnClickListener(v -> {
            lumos.updateGesture(gesture, wristY);
            lastAction = actionLabel;
            updateStatus();
        });
    }

    private void wireWristSeek() {
        SeekBar seekBar = findViewById(R.id.wristSeekBar);
        seekBar.setMax(100);
        seekBar.setProgress(50);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                wristY = progress / 100f;
                updateStatus();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    private void updateStatus() {
        Device selected = lumos.getSelectedDevice();
        Result result = lumos.getLatestResultSnapshot();
        String selectedName = (selected == null) ? "none" : selected.getName();

        String msg = "Selected Device: " + selectedName + "\n"
                + "Direction: " + result.getDirection() + "\n"
                + "WristY: " + wristY + "\n"
                + "Last Action: " + lastAction + "\n"
                + "MediaPipe RawResult: " + (result.getRawResult() == null ? "null" : "ok") + "\n"
                + "Guide: 손 제스처 + 카메라 입력으로 테스트하세요.";

        statusBox.setText(msg);
    }
}
