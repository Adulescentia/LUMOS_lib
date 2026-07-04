package io.github.adulescentia.LUMOS_lib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import org.joml.Vector3f;

public class VectorOverlayView extends View {
    private final Paint paint = new Paint();
    private Vector3f currentVector = null;
    private float startX, startY;

    public VectorOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.RED); // 화살표 색상
        paint.setStrokeWidth(15f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    // MediaPipeArmVectorEngine의 콜백에서 이 메서드를 호출하여 벡터를 업데이트합니다.
    public void updateVector(Vector3f armVector, float wristX, float wristY) {
        this.currentVector = armVector;
        // 화면 해상도에 맞게 좌표 스케일링 필요 (예제에서는 그대로 사용)
        this.startX = wristX;
        this.startY = wristY;
        invalidate(); // UI 갱신 요청
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (currentVector == null) return;

        // 화면에 그릴 벡터의 길이 (시각적 확장을 위해 곱해줌)
        float lineLength = 300f;

        // 3D 벡터를 2D 화면에 투영 (단순화를 위해 X, Y만 사용)
        float endX = startX + (currentVector.x * lineLength);
        float endY = startY + (currentVector.y * lineLength);

        // 손목 위치(startX, startY)에서 벡터 방향으로 선 그리기
        canvas.drawLine(startX, startY, endX, endY, paint);

        // 화살표 머리 그리기 (생략 가능)
        canvas.drawCircle(endX, endY, 20f, paint);
    }
}