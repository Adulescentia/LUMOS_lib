package io.github.adulescentia.LUMOS_lib;

import java.util.List;
// MediaPipe 공식 안드로이드 Tasks 구조 임포트
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.components.containers.Landmark; // World Landmark 대응용 기본 구조체

// 3D 수학 연산 (LUMOS 라이브러리 검증 라이브러리)
import org.joml.Vector3f;

public class PrecisionZ {

    // 알고리즘 상수 (v8.7 반영 세팅)
    private static final float ZERO_CUTOFF = 0.25f;
    private static final float ZERO_CUTOFF_SQ = ZERO_CUTOFF * ZERO_CUTOFF; // sqrt 연산 방지용 제곱 비교
    private static final float FUSION_BLEND = 0.40f;
    private static final float ALPHA_Z = 0.35f;

    // 상태 저장 필드 (원시 타입으로 메모리 오버헤드 최소화)
    private float calibratedArmLengthSq = -1.0f;
    private float prevSmoothZ = 0.0f;
    private int currentSign = 1;
    private float prevRawWorldZ = 0.0f;

    // 가비지 컬렉션(GC) 방지용 싱글 버퍼 오브젝트 생성
    private final Vector3f resultVector = new Vector3f();

    /**
     * @param result MediaPipe PoseLandmarkerResult 결과물 전체
     * @return LUMOS 라이브러리 제어용 정밀 3D Vector3f
     */
    public Vector3f calculateFastArmVector(PoseLandmarkerResult result) {
        // 결과 안전성 검사
        if (result == null || result.landmarks() == null || result.landmarks().isEmpty()
                || result.worldLandmarks() == null || result.worldLandmarks().isEmpty()) {
            resultVector.set(0.0f, 0.0f, 0.0f);
            return resultVector;
        }

        // 첫 번째 감지된 사람의 랜드마크 리스트 추출 (Java 7/8 호환을 위해 List 타입 명시)
        List<NormalizedLandmark> landmarks = result.landmarks().get(0);
        List<Landmark> worldLandmarks = result.worldLandmarks().get(0);

        // 안전 장치: 최소 요구 랜드마크 개수(16번 손목까지) 확보되었는지 검사
        if (landmarks.size() < 17 || worldLandmarks.size() < 17) {
            resultVector.set(0.0f, 0.0f, 0.0f);
            return resultVector;
        }

        // 12번(오른쪽 어깨), 16번(오른쪽 손목) 추출
        NormalizedLandmark shoulder = landmarks.get(12);
        NormalizedLandmark wrist = landmarks.get(16);

        // 월드(미터 단위) 데이터도 명확하게 Landmark 클래스로 매핑하여 var 키워드 에러 해결
        Landmark worldShoulder = worldLandmarks.get(12);
        Landmark worldWrist = worldLandmarks.get(16);

        // 1. 영상 평면 변위 및 거리를 제곱 단위로 계산하여 CPU 연산 절약
        float dx = wrist.x() - shoulder.x();
        float dy = wrist.y() - shoulder.y();
        float distance2DSq = dx * dx + dy * dy;

        // 2. 월드 물리 단위 Z축 가속도/변화량 추출
        float rawZDiff = worldWrist.z() - worldShoulder.z();
        float deltaRawZ = (prevRawWorldZ == 0.0f) ? 0.0f : rawZDiff - prevRawWorldZ;
        prevRawWorldZ = rawZDiff;

        // 3. Elastic L (캘리브레이션 동적 임계치 갱신)
        if (calibratedArmLengthSq < 0 || distance2DSq > calibratedArmLengthSq) {
            calibratedArmLengthSq = distance2DSq;
        }

        // 4. 역투영 기하 연산 부활
        float targetZ = 0.0f;
        float zSq = calibratedArmLengthSq - distance2DSq;
        if (zSq > 0.0f) {
            targetZ = (float) Math.sqrt(zSq);
        }

        // 5. 부호 유연화 판정 및 최적 복구
        if (rawZDiff > 0.05f) currentSign = 1;
        else if (rawZDiff < -0.05f) currentSign = -1;

        targetZ *= currentSign;

        // 6. 하이브리드 인젝션 융합 (0.3m 이내 마진)
        if (targetZ * targetZ < 0.09f) {
            targetZ += deltaRawZ * FUSION_BLEND;
        }

        // 7. 흔들림 보정 필터링 (Low-Pass Filter)
        float smoothZ = prevSmoothZ + ALPHA_Z * (targetZ - prevSmoothZ);
        prevSmoothZ = smoothZ;

        // 8. 최종 통제: 0.25m 이하 차단 및 사용자 요청 라벨 최종 반전
        float finalZ = 0.0f;
        if ((smoothZ * smoothZ) >= ZERO_CUTOFF_SQ) {
            finalZ = smoothZ * -1.0f; // 요청한 FRONT <-> BEHIND 뒤집기 싱크
        }

        // 9. 기존 생성해둔 인스턴스에 값만 덮어씌워 런타임 최적화 및 가비지 프리(Garbage-Free) 반환
        resultVector.set(dx, dy, finalZ);
        resultVector.normalize();

        return resultVector;
    }

    public void reset() {
        this.calibratedArmLengthSq = -1.0f;
        this.prevSmoothZ = 0.0f;
        this.prevRawWorldZ = 0.0f;
    }
}