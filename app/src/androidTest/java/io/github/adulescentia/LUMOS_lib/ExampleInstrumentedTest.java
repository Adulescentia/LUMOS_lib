package io.github.adulescentia.LUMOS_lib;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private MediaPipeArmVectorEngine engine;
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        engine = new MediaPipeArmVectorEngine();

        // assets 폴더에 해당 파일들이 있어야 합니다.
        engine.initialize(context, "pose_landmarker_full.task", "gesture_recognizer.task");
    }

    @Test
    public void testProcessFrame_returnsCallbacks() throws InterruptedException {
        // 비동기 콜백을 기다리기 위한 Latch (포즈 1번, 제스처 1번)
        CountDownLatch poseLatch = new CountDownLatch(1);
        CountDownLatch gestureLatch = new CountDownLatch(1);

        engine.setVectorResultListener((armVector, rawResult, timestampMs) -> {
            assertNotNull("Arm vector should not be null", armVector);
            poseLatch.countDown();
        });

        engine.setGestureResultListener((gesture, wristY, timestampMs) -> {
            assertNotNull("Gesture should not be null", gesture);
            gestureLatch.countDown();
        });

        // 테스트용 이미지 로드 (res/drawable/test_image.jpg가 있다고 가정)
        // int resourceId = context.getResources().getIdentifier("test_image", "drawable", context.getPackageName());
        // Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);

        // (임시) 빈 비트맵으로 테스트할 경우 (실제 인식은 안되지만 파이프라인이 터지지 않는지 확인 가능)
        Bitmap bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
        MPImage mpImage = new BitmapImageBuilder(bitmap).build();

        // 프레임 처리 요청
        engine.processFrame(mpImage, System.currentTimeMillis());

        // 콜백이 올 때까지 최대 5초 대기
        boolean poseSuccess = poseLatch.await(5, TimeUnit.SECONDS);
        boolean gestureSuccess = gestureLatch.await(5, TimeUnit.SECONDS);

        assertTrue("Pose callback was not triggered", poseSuccess);
        assertTrue("Gesture callback was not triggered", gestureSuccess);
    }

    @After
    public void tearDown() {
        if (engine != null) {
            engine.close();
        }
    }
}