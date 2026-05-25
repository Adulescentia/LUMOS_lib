# LUMOS_lib

LUMOS_lib는 **MediaPipe 기반 팔 방향(arm vector) 추정 + 디바이스 선택/제어 파이프라인**을 제공하는 안드로이드 라이브러리입니다.  
이 문서는 현재 코드 기준으로 라이브러리의 기능, 구조, 사용 방법, 제약사항을 자세히 설명합니다.

---

## 1) 라이브러리 개요

LUMOS_lib는 다음 문제를 해결하기 위한 라이브러리입니다.

- 카메라 프레임을 MediaPipe에 넣어 사용자 방향(arm vector)을 얻는다.
- 등록된 가상/실제 디바이스 중 사용자가 바라보는 디바이스를 계산한다.
- 제스처 상태 머신을 통해 선택/전원 토글/모드 적용 이벤트를 다룬다.
- 외부 앱에서 콜백으로 처리 결과를 수신한다.

핵심 구성요소:

- `Lumos`: 라이브러리 진입점(싱글톤), 라이프사이클/콜백/입력 연결 담당
- `MediaPipeArmVectorEngine`: MediaPipe PoseLandmarker 실행 및 arm vector 산출
- `Result`: 방향, 위치, 선택 디바이스를 담는 결과 모델
- `Device`: 디바이스 데이터 모델(id, name, position)
- `Detector`: 방향 벡터와 디바이스 위치 벡터 매칭
- `GestureStateManager`: 제스처 기반 상태 전이 및 액션 이벤트

---

## 2) 현재 제공 기능

### 2-1. 디바이스 등록

- `Lumos.registerDevice(x, y, z, deviceName, deviceType)` 호출 시 **호스트 앱이 제공한 좌표/이름/타입** 그대로 디바이스를 등록합니다.
- 라이브러리는 내부 식별용 ID(`DEV_01` 형태)를 자동 부여하지만, 위치/이름/타입은 호출자가 제공한 값을 사용합니다.

### 2-2. 디바이스 선택 알고리즘

- 사용자 방향 벡터(`direction`)와
- 사용자 위치에서 각 디바이스 위치로 향하는 목표 벡터(`Target = DevicePos - UserPos`)
- 두 벡터를 정규화 후 내적(dot product) 비교
- 내적이 가장 큰 디바이스를 선택 디바이스로 판단

### 2-3. MediaPipe 처리 파이프라인

- `initialize(context, modelAssetPath)`로 MediaPipe 엔진 초기화
- `ingestExternalCameraFrame(mpImage, timestamp)`로 프레임 입력
- 프레임마다 arm vector 계산 후 `Result` 업데이트 + 외부 콜백 전달

### 2-4. 제스처 상태 관리

- `GestureStateManager`에서 FIST/PALM/ONE_FINGER/V_SIGN/UNDEF 제스처 기반 상태 전이 수행
- 선택 토글, 전원 토글, 모드 적용 이벤트를 `ActionListener`로 외부에 제공

### 2-5. 결과 전달

- `registerExternalResultChannel(...)`로 결과 수신 콜백 등록
- `getLatestResultSnapshot()`으로 최신 결과를 안전하게 복사(clone) 조회

---

## 3) 빠른 시작 (호스트 앱 연동)

아래는 호스트 앱에서 LUMOS_lib를 사용하는 기본 흐름입니다.

```java
Lumos lumos = Lumos.getInstance();

// 1) 디바이스 등록 (호스트 앱이 좌표/이름/타입 제공)
lumos.registerDevice(0.0, 1.2, 4.0, "LivingRoom TV", "DISPLAY");
lumos.registerDevice(2.5, 1.0, 3.5, "Standing Lamp", "LIGHT");

// 2) 결과 콜백 등록
lumos.registerExternalResultChannel(result -> {
    Vector3f dir = result.getDirection();
    Vector3f pos = result.getCurrentPosition();
    Vector3f cam = result.getCameraPos();
    // UI/로그 처리
});

// 3) 초기화 (실사용 권장)
lumos.initialize(context, "pose_landmarker_full.task");

// 4) 시작
lumos.startIoTControlProcess();

// 5) 카메라 프레임 입력 (CameraX/Camera2 -> MPImage 변환 후)
lumos.ingestExternalCameraFrame(mpImage, System.currentTimeMillis());

// 6) 종료
lumos.shutdown();
```

---

## 4) 주요 API 설명

## `Lumos`

- `static Lumos getInstance()`  
  싱글톤 인스턴스 획득

- `@Nullable Device registerDevice(double x, double y, double z, String deviceName, String deviceType)`  
  호스트가 제공한 좌표/이름/타입으로 디바이스 등록 후 반환

- `Collection<Device> getDeviceList()`  
  등록된 디바이스 목록 반환

- `void registerUIUpdater(Consumer<Image> uiUpdateCallback)`  
  UI 프레임 채널 콜백 등록(현재 mock 이미지 전달 시뮬레이션 포함)

- `void registerExternalResultChannel(Consumer<Result> resultConsumer)`  
  결과 콜백 채널 등록

- `void initialize()`  
  호환용/시뮬레이션 초기화

- `void initialize(Context context, String modelAssetPath)`  
  실사용 MediaPipe 초기화

- `void startIoTControlProcess()`  
  파이프라인 시작(호스트 프레임 입력 전제)

- `void ingestExternalCameraFrame(MPImage mpImage, long timestampMs)`  
  외부 카메라 프레임 입력

- `void updateGesture(GestureStateManager.Gesture gesture, float wristY)`  
  제스처 상태 업데이트

- `Result getLatestResultSnapshot()`  
  최신 결과 복사본 획득

- `void shutdown()`  
  엔진 정리 및 종료

## `Result`

- `Vector3f getDirection()`
- `Device getSelectedDevice()`
- `Vector3f getCurrentPosition()`
- `Vector3f getCameraPos()`
- `Result clone()` (Vector3f deep copy)

## `Device`

- `String getId()`
- `String getName()`
- `Vector3f getPosition()`

---

## 5) Build Variant / 테스트 앱 안내

- `app/src/debug`에 테스트용 `MainActivity`가 있습니다.
- Android Studio에서 **debug variant**로 실행해야 테스트 앱이 포함됩니다.
- release/main variant에서는 debug activity가 포함되지 않습니다.

---

## 6) 현재 제약사항 및 주의점

1. 일부 동작은 시뮬레이션 성격이 포함됩니다.
   - 디바이스 등록 자체는 랜덤이 아니라 **호스트 제공 좌표/이름/타입 기반**입니다.
   - `registerUIUpdater` 채널은 현재 mock image(`null`) 경로가 포함될 수 있습니다.

2. MediaPipe 모델 파일이 앱 assets에 있어야 합니다.
   - 예: `pose_landmarker_full.task`

3. 호스트 앱에서 카메라 프레임을 `MPImage`로 변환해 입력해야 합니다.
   - CameraX/Camera2 연동은 호스트 앱 책임입니다.

4. 선택 디바이스가 없는 순간 `Result.getSelectedDevice()` 호출은 예외가 발생할 수 있으므로 호출부에서 보호 처리하는 것을 권장합니다.

---

## 7) 권장 운영 패턴

- 앱 시작 시 1회 `initialize(context, modelAssetPath)`
- 화면 진입/카메라 시작 시 `startIoTControlProcess()`
- 프레임마다 `ingestExternalCameraFrame(...)`
- 화면 종료/앱 종료 시 `shutdown()`

---

## 8) 향후 개선 권장 항목

- 디바이스 등록 파라미터 검증 강화(좌표 범위, 이름/타입 null/blank 정책)  
- Result에 신뢰도(score), timestamp, raw landmark 접근 API 추가  
- 오류 코드/상태 코드 표준화 (init 실패, 모델 누락, 프레임 형식 오류)  
- 샘플 앱 분리 모듈화 (library module + sample app module)

---

## 9) 라이선스/주의

이 저장소의 실제 라이선스 파일/정책을 반드시 확인 후 사용하세요.

