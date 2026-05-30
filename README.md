# LUMOS_lib

LUMOS_lib는 **MediaPipe 기반 팔 방향(arm vector) 추정 + 디바이스 선택/제어 파이프라인**을 제공하는 안드로이드 라이브러리입니다.
이 문서는 현재 코드 기준으로 라이브러리의 기능, 구조, 사용 방법, **유저 세이프티(안전한 오류 처리) 정책**까지 자세히 설명합니다.

---

## 1) 라이브러리 개요

LUMOS_lib는 다음 문제를 해결하기 위한 라이브러리입니다.

- 카메라 프레임을 MediaPipe에 넣어 사용자 방향(arm vector)을 얻는다.
- 등록된 디바이스 중 사용자가 바라보는 디바이스를 계산한다.
- 제스처 상태 머신을 통해 선택/전원 토글/모드 적용 이벤트를 다룬다.
- 외부 앱에서 콜백으로 처리 결과를 수신한다.

핵심 구성요소:

- `Lumos`: 라이브러리 진입점(싱글톤), 라이프사이클/콜백/입력 연결 담당
- `MediaPipeArmVectorEngine`: MediaPipe PoseLandmarker 실행 및 arm vector 산출
- `Result`: 방향, 위치, 선택 디바이스, 명령 정보를 담는 결과 모델
- `Device`: 디바이스 데이터 모델(id, name, type, position)
- `Detector`: 방향 벡터와 디바이스 위치 벡터 매칭
- `GestureStateManager`: 제스처 기반 상태 전이 및 액션 이벤트
- `LumosException`, `NotInitializedErr`, `InvalidInputErr`: 안전한 오류 전달 계층

---

## 2) 현재 제공 기능

### 2-1. 디바이스 등록 (호스트 입력 기반)

- `Lumos.registerDevice(x, y, z, deviceName, deviceType)` 호출 시 **호스트 앱이 제공한 좌표/이름/타입** 그대로 디바이스를 등록합니다.
- 라이브러리는 내부 식별용 ID(`DEV_01` 형태)를 자동 부여합니다.
- 즉, 더 이상 랜덤/가상 디바이스 자동 생성에 의존하지 않고, **실사용 배치 좌표를 기준**으로 동작합니다.

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
- 이벤트는 `Result`의 command 정보와 함께 전달될 수 있어, 호스트 앱이 즉시 액션 라우팅 가능

### 2-5. 결과 전달

- `registerExternalResultChannel(...)`로 결과 수신 콜백 등록
- `getLatestResultSnapshot()`으로 최신 결과를 안전하게 복사(clone) 조회

### 2-6. 디바이스 직렬화/역직렬화

- `serializeDevices()`로 현재 등록된 디바이스 목록을 `String[]` 형태로 저장/전송할 수 있습니다.
- `deserializeDevices(String[] serializedDevices)`로 저장된 문자열 배열을 다시 `Device[]`로 복원하고, Lumos 내부 디바이스 목록에도 반영합니다.
- 디바이스 이름/타입/id에 구분자(`|`)나 줄바꿈이 들어가도 복원 가능하도록 내부 escape 포맷을 사용합니다.
- 역직렬화 입력이 잘못된 경우 `InvalidInputErr`를 던지며, 기존 디바이스 목록은 유지됩니다.

---

## 3) 유저 세이프티(안전한 오류 처리) 정책

LUMOS_lib는 런타임 실패 시 `NullPointerException` 같은 모호한 오류 대신, **의도가 명확한 예외 타입**을 우선 제공하도록 설계되어 있습니다.

### 3-1. 핵심 예외 타입

- `NotInitializedErr`
  초기화(`initialize`) 이전에 처리 함수 호출 시 발생
- `InvalidInputErr`
  파라미터가 null/blank/비정상 값(예: NaN 좌표)일 때 발생
- `LumosException`
  라이브러리 내부 처리 실패를 포괄하는 상위 예외

### 3-2. 어떤 경우에 발생하나

- `startIoTControlProcess()`를 초기화 없이 호출 → `NotInitializedErr`
- `ingestExternalCameraFrame(...)`를 초기화 없이 호출 → `NotInitializedErr`
- `updateGesture(null, ...)` 호출 → `InvalidInputErr`
- `registerDevice(...)`에 빈 이름/타입 또는 비정상 좌표 전달 → `InvalidInputErr`
- `deserializeDevices(...)`에 null/빈 항목/깨진 포맷/비정상 좌표 전달 → `InvalidInputErr`
- `deserializeDevices(...)`를 초기화 없이 호출 → `NotInitializedErr`
- 콜백 등록 함수에 null 콜백 전달 → `InvalidInputErr`

### 3-3. 호스트 앱 권장 처리 패턴

```java
try {
    Lumos lumos = Lumos.getInstance();
    lumos.initialize(context, "pose_landmarker_full.task");
    lumos.startIoTControlProcess();
} catch (NotInitializedErr e) {
    // 초기화 순서 문제: 사용자에게 재시도/재초기화 안내
} catch (InvalidInputErr e) {
    // 입력값 문제: 설정 화면 검증 메시지 노출
} catch (LumosException e) {
    // 라이브러리 내부 일반 오류: 로그 수집 + 안전 중단
}
```

### 3-4. 왜 중요한가

- 장애 원인 분류가 쉬워 복구 UX 설계가 쉬움
- 크래시를 “무작정 종료”가 아니라 “예상 가능한 실패”로 전환
- IoT 제어에서 오동작/오제어 리스크 감소

---

## 4) 빠른 시작 (호스트 앱 연동)

아래는 호스트 앱에서 LUMOS_lib를 사용하는 기본 흐름입니다.

```java
Lumos lumos = Lumos.getInstance();

// 1) 초기화
lumos.initialize(context, "pose_landmarker_full.task");

// 2) 디바이스 등록 (호스트 앱이 좌표/이름/타입 제공)
lumos.registerDevice(0.0, 1.2, 4.0, "LivingRoom TV", "DISPLAY");
lumos.registerDevice(2.5, 1.0, 3.5, "Standing Lamp", "LIGHT");

// 2-1) 디바이스 목록 저장/복원 예시
String[] savedDevices = lumos.serializeDevices();
Device[] restoredDevices = lumos.deserializeDevices(savedDevices);

// 3) 결과 콜백 등록
lumos.registerExternalResultChannel(result -> {
    Vector3f dir = result.getDirection();
    Vector3f pos = result.getCurrentPosition();
    Vector3f cam = result.getCameraPos();
    Device selected = result.getSelectedDevice();
    // UI/로그 처리
});

// 4) 파이프라인 시작
lumos.startIoTControlProcess();

// 5) 카메라 프레임 입력 (CameraX/Camera2 -> MPImage 변환 후)
lumos.ingestExternalCameraFrame(mpImage, System.currentTimeMillis());

// 6) 종료
lumos.shutdown();
```

---

## 5) 주요 API 설명

## `Lumos`

- `static Lumos getInstance()`
  싱글톤 인스턴스 획득

- `@Nullable Device registerDevice(double x, double y, double z, String deviceName, String deviceType)`
  호스트가 제공한 좌표/이름/타입으로 디바이스 등록 후 반환

- `Collection<Device> getDeviceList()`
  등록된 디바이스 목록 반환

- `String[] serializeDevices()`
  현재 등록된 디바이스 목록을 저장/전송 가능한 문자열 배열로 직렬화

- `Device[] deserializeDevices(String[] serializedDevices)`
  문자열 배열을 검증 후 Device 배열로 복원하고 내부 디바이스 목록을 교체

- `void registerUIUpdater(Consumer<Image> uiUpdateCallback)`
  UI 프레임 채널 콜백 등록

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
- `Result.CommandType getCommandType()`
- `String getCommandDetail()`
- `Result clone()` (Vector3f deep copy)

## `Device`

- `String getId()`
- `String getName()`
- `String getType()`
- `Vector3f getPosition()`

---

## 6) Build Variant / 테스트 앱 안내

- `app/src/debug`에 테스트용 `MainActivity`가 있습니다.
- Android Studio에서 **debug variant**로 실행해야 테스트 앱이 포함됩니다.
- release/main variant에서는 debug activity가 포함되지 않습니다.

---

## 7) 현재 제약사항 및 주의점

1. MediaPipe 모델 파일이 앱 assets에 있어야 합니다.
   - 예: `pose_landmarker_full.task`

2. 호스트 앱에서 카메라 프레임을 `MPImage`로 변환해 입력해야 합니다.
   - CameraX/Camera2 연동은 호스트 앱 책임입니다.

3. 선택 디바이스가 없는 프레임이 존재할 수 있습니다.
   - 선택 상태 사용 전 null/예외 처리 정책을 앱에서 정의하세요.

4. 안전성 측면에서, 라이브러리는 명시적 예외를 던지지만
   - 호스트 앱도 반드시 `try-catch`로 사용자 메시지/복구 루틴을 구현해야 합니다.

---

## 8) 권장 운영 패턴

- 앱 시작 시 1회 `initialize(context, modelAssetPath)`
- 초기화 성공 후 디바이스 등록
- 화면 진입/카메라 시작 시 `startIoTControlProcess()`
- 프레임마다 `ingestExternalCameraFrame(...)`
- 제스처 추정 결과가 있으면 `updateGesture(...)`
- 화면 종료/앱 종료 시 `shutdown()`

---

## 9) 통합 체크리스트 (실사용 전)

- [ ] MediaPipe 모델 파일(asset) 포함 여부
- [ ] 카메라 권한/프레임 변환 경로 점검
- [ ] 디바이스 좌표계(월드 좌표 기준) 정의 완료
- [ ] 디바이스 저장/복원 시 `serializeDevices()` / `deserializeDevices(...)` 사용
- [ ] 예외 처리(NotInitializedErr/InvalidInputErr/LumosException) 구현
- [ ] 결과 콜백에서 UI 스레드 전환 처리
- [ ] 앱 종료 시 `shutdown()` 호출

---

## 10) 향후 개선 권장 항목

- Result에 confidence score / timestamp 표준화 필드 확장
- 디바이스 매칭 임계값/필터링(노이즈 억제) 옵션화
- 에러 코드(enum) + 다국어 메시지 매핑 제공
- 샘플 앱 분리 모듈화 (library module + sample app module)

---

## 11) 라이선스/주의

이 저장소의 실제 라이선스 파일/정책을 반드시 확인 후 사용하세요.

---

## 12) 테스트 앱 실행

`lumosapp` 모듈은 기존 `app` 모듈의 Java 소스 코드를 수정하지 않고, `lumoslib` 래퍼 라이브러리 모듈을 통해 LUMOS API를 참조하는 간단한 Compose 테스트 앱입니다.

```bash
./gradlew :lumosapp:installDebug
```

앱에서 다음 기능을 빠르게 확인할 수 있습니다.

- `initialize()` 기반 API 테스트 모드 시작
- 디바이스 수동 등록 및 샘플 디바이스 3개 등록
- 등록 디바이스 직렬화/역직렬화
- `startIoTControlProcess()` 및 `getLatestResultSnapshot()` 호출
- `ONE_FINGER`, `FIST`, `PALM`, `V_SIGN` 제스처 입력

`lumoslib`는 테스트 앱이 라이브러리를 의존성처럼 사용할 수 있도록 기존 `app/src/main/java`를 source set으로 참조합니다. 따라서 기존 라이브러리/앱 코드를 직접 변경하지 않고 테스트 앱만 교체해 동작을 확인할 수 있습니다. 실제 카메라 프레임 처리 검증은 호스트 앱에서 `initialize(context, modelAssetPath)`와 `ingestExternalCameraFrame(...)`을 연결해 진행하세요.
