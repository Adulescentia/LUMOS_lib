package io.github.adulescentia.lumosapp;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.adulescentia.LUMOS_lib.Device;
import io.github.adulescentia.LUMOS_lib.GestureStateManager;
import io.github.adulescentia.LUMOS_lib.Lumos;
import io.github.adulescentia.LUMOS_lib.Result;

public class LumosDemoActivity extends Activity {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Device> devices = new ArrayList<>();
    private final Map<String, Boolean> powerStates = new HashMap<>();
    private final Map<String, Integer> modeLevels = new HashMap<>();

    private Lumos lumos;
    private String selectedDeviceId;
    private Result latestResult = new Result();

    private LinearLayout root;
    private LinearLayout deviceList;
    private TextView statusText;
    private TextView logText;
    private EditText nameInput;
    private EditText typeInput;
    private EditText xInput;
    private EditText yInput;
    private EditText zInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        safeStartup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (lumos != null) {
                lumos.shutdown();
            }
        } catch (Throwable ignored) {
            // Keep Activity teardown crash-free even if the library is already closed.
        }
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        scrollView.addView(root);

        TextView title = text("LUMOS 스마트홈 데모", 24, Typeface.BOLD);
        root.addView(title);
        root.addView(text("라이브러리 코드는 그대로 두고, 앱 레이어에서 LUMOS API를 호출해 디바이스 등록/제어를 테스트합니다.", 14, Typeface.NORMAL));

        statusText = text("상태: 시작 중", 16, Typeface.BOLD);
        root.addView(statusText);

        nameInput = input("Living Room TV");
        typeInput = input("DISPLAY");
        xInput = input("0.0");
        yInput = input("1.2");
        zInput = input("4.0");

        root.addView(section("디바이스 등록"));
        root.addView(label("이름"));
        root.addView(nameInput);
        root.addView(label("타입"));
        root.addView(typeInput);
        root.addView(label("좌표 X / Y / Z"));
        LinearLayout coordinates = row();
        coordinates.addView(xInput, weightParams());
        coordinates.addView(yInput, weightParams());
        coordinates.addView(zInput, weightParams());
        root.addView(coordinates);

        LinearLayout registrationButtons = row();
        registrationButtons.addView(button("등록", v -> addDeviceFromInput()), weightParams());
        registrationButtons.addView(button("샘플 3개", v -> seedDevices()), weightParams());
        root.addView(registrationButtons);

        root.addView(section("LUMOS API"));
        LinearLayout apiButtons1 = row();
        apiButtons1.addView(button("프로세스 시작", v -> withLumos(engine -> {
            engine.startIoTControlProcess();
            appendLog("startIoTControlProcess() 호출 성공");
        })), weightParams());
        apiButtons1.addView(button("스냅샷", v -> withLumos(engine -> {
            latestResult = engine.getLatestResultSnapshot();
            appendLog("스냅샷: " + summarize(latestResult));
            renderStatus();
        })), weightParams());
        root.addView(apiButtons1);

        LinearLayout apiButtons2 = row();
        apiButtons2.addView(button("직렬화", v -> withLumos(engine -> appendLog("직렬화 " + engine.serializeDevices().length + "건"))), weightParams());
        apiButtons2.addView(button("재적재", v -> reloadSerializedDevices()), weightParams());
        root.addView(apiButtons2);

        root.addView(section("스마트홈 제어"));
        deviceList = new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL);
        root.addView(deviceList);

        root.addView(section("실행 로그"));
        logText = text("", 13, Typeface.NORMAL);
        root.addView(logText);

        setContentView(scrollView);
        renderDevices();
    }

    private void safeStartup() {
        try {
            lumos = Lumos.getInstance();
            lumos.initialize();
            lumos.registerExternalResultChannel(result -> mainHandler.post(() -> {
                latestResult = result;
                appendLog("결과 콜백: " + summarize(result));
                renderStatus();
            }));
            appendLog("LUMOS 초기화 완료");
            renderStatus();
        } catch (Throwable t) {
            lumos = null;
            appendLog("LUMOS 초기화 실패: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            appendLog("앱은 계속 실행됩니다. 버튼을 눌러도 크래시 대신 로그로 표시됩니다.");
            renderStatus();
        }
    }

    private void addDeviceFromInput() {
        withLumos(engine -> {
            String name = clean(nameInput.getText().toString(), "Unnamed Device");
            String type = clean(typeInput.getText().toString(), "UNKNOWN");
            double x = parseCoordinate(xInput, "X");
            double y = parseCoordinate(yInput, "Y");
            double z = parseCoordinate(zInput, "Z");
            Device device = engine.registerDevice(x, y, z, name, type);
            if (device != null) {
                rememberDevice(device);
                appendLog("등록: " + summarize(device));
                renderStatus();
                renderDevices();
            }
        });
    }

    private void seedDevices() {
        withLumos(engine -> {
            addSample(engine, "Living Room TV", "DISPLAY", 0.0, 1.2, 4.0);
            addSample(engine, "Desk Lamp", "LIGHT", -1.4, 0.8, 2.1);
            addSample(engine, "Speaker", "AUDIO", 1.6, 1.0, 2.8);
            renderStatus();
            renderDevices();
        });
    }

    private void reloadSerializedDevices() {
        withLumos(engine -> {
            Device[] restored = engine.deserializeDevices(engine.serializeDevices());
            devices.clear();
            for (Device device : restored) {
                rememberDevice(device);
            }
            if (selectedDeviceId != null && !containsDevice(selectedDeviceId)) {
                selectedDeviceId = null;
            }
            appendLog("재적재 완료: " + restored.length + "건");
            renderStatus();
            renderDevices();
        });
    }

    private void addSample(Lumos engine, String name, String type, double x, double y, double z) {
        Device device = engine.registerDevice(x, y, z, name, type);
        if (device != null) {
            rememberDevice(device);
            appendLog("샘플 등록: " + summarize(device));
        }
    }

    private void rememberDevice(Device device) {
        devices.add(device);
        powerStates.put(device.getId(), false);
        modeLevels.put(device.getId(), 50);
    }

    private void renderStatus() {
        String selectedName = "없음";
        for (Device device : devices) {
            if (device.getId().equals(selectedDeviceId)) {
                selectedName = device.getName();
                break;
            }
        }
        statusText.setText("상태: 디바이스 " + devices.size() + "개 · 선택 " + selectedName + "\n최신 결과: " + summarize(latestResult));
    }

    private void renderDevices() {
        deviceList.removeAllViews();
        if (devices.isEmpty()) {
            deviceList.addView(text("등록된 디바이스가 없습니다. '샘플 3개'를 눌러 바로 테스트하세요.", 14, Typeface.NORMAL));
            return;
        }

        for (Device device : devices) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(12), dp(12), dp(12));
            card.setBackgroundColor(0xFFEFEAF7);

            boolean selected = device.getId().equals(selectedDeviceId);
            boolean powered = Boolean.TRUE.equals(powerStates.get(device.getId()));
            int mode = modeLevels.containsKey(device.getId()) ? modeLevels.get(device.getId()) : 50;

            card.addView(text(device.getName() + (selected ? "  · 선택됨" : ""), 18, Typeface.BOLD));
            card.addView(text(device.getType() + " · " + vector(device) + " · 전원 " + (powered ? "ON" : "OFF") + " · 모드 " + mode + "%", 13, Typeface.NORMAL));

            LinearLayout row1 = row();
            row1.addView(button("조준", v -> selectDevice(device)), weightParams());
            row1.addView(button("전원", v -> togglePower(device)), weightParams());
            card.addView(row1);

            LinearLayout row2 = row();
            row2.addView(button("모드 -", v -> changeMode(device, -10)), weightParams());
            row2.addView(button("모드 +", v -> changeMode(device, 10)), weightParams());
            card.addView(row2);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dp(10));
            deviceList.addView(card, params);
        }
    }

    private void selectDevice(Device device) {
        withLumos(engine -> {
            engine.updateGesture(GestureStateManager.Gesture.FIST, 0.5f);
            engine.updateGesture(GestureStateManager.Gesture.ONE_FINGER, 0.5f);
            selectedDeviceId = device.getId();
            appendLog("조준/선택: " + device.getName());
            renderStatus();
            renderDevices();
        });
    }

    private void togglePower(Device device) {
        withLumos(engine -> {
            engine.updateGesture(GestureStateManager.Gesture.FIST, 0.5f);
            engine.updateGesture(GestureStateManager.Gesture.PALM, 0.5f);
            boolean next = !Boolean.TRUE.equals(powerStates.get(device.getId()));
            powerStates.put(device.getId(), next);
            appendLog("전원 " + (next ? "ON" : "OFF") + ": " + device.getName());
            renderDevices();
        });
    }

    private void changeMode(Device device, int delta) {
        withLumos(engine -> {
            engine.updateGesture(GestureStateManager.Gesture.FIST, 0.5f);
            engine.updateGesture(GestureStateManager.Gesture.V_SIGN, 0.5f);
            engine.updateGesture(GestureStateManager.Gesture.FIST, 0.5f - (delta / 100f));
            engine.updateGesture(GestureStateManager.Gesture.V_SIGN, 0.5f - (delta / 100f));
            int current = modeLevels.containsKey(device.getId()) ? modeLevels.get(device.getId()) : 50;
            int next = Math.max(0, Math.min(100, current + delta));
            modeLevels.put(device.getId(), next);
            appendLog("모드 " + next + "%: " + device.getName());
            renderDevices();
        });
    }

    private void withLumos(LumosAction action) {
        if (lumos == null) {
            appendLog("LUMOS가 초기화되지 않았습니다. 앱은 종료하지 않습니다.");
            return;
        }
        try {
            action.run(lumos);
        } catch (Throwable t) {
            appendLog("오류: " + t.getClass().getSimpleName() + " - " + t.getMessage());
        }
    }

    private boolean containsDevice(String id) {
        for (Device device : devices) {
            if (device.getId().equals(id)) return true;
        }
        return false;
    }

    private double parseCoordinate(EditText input, String axis) {
        try {
            return Double.parseDouble(input.getText().toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(axis + " 좌표가 숫자가 아닙니다.");
        }
    }

    private String clean(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private void appendLog(String message) {
        logText.append("• " + message + "\n");
    }

    private String summarize(Device device) {
        return device.getId() + " / " + device.getName() + " / " + device.getType() + " / " + vector(device);
    }

    private String summarize(Result result) {
        String selected = "없음";
        try {
            selected = result.getSelectedDevice().getName();
        } catch (Throwable ignored) {
            // No selected device is a normal demo state.
        }
        return "direction=" + format(result.getDirection().x, result.getDirection().y, result.getDirection().z)
                + ", selected=" + selected
                + ", command=" + result.getCommandType() + "/" + result.getCommandDetail();
    }

    private String vector(Device device) {
        return format(device.getPosition().x, device.getPosition().y, device.getPosition().z);
    }

    private String format(float x, float y, float z) {
        return String.format(Locale.US, "(%.2f, %.2f, %.2f)", x, y, z);
    }

    private TextView section(String value) {
        TextView view = text(value, 18, Typeface.BOLD);
        view.setPadding(0, dp(18), 0, dp(6));
        return view;
    }

    private TextView label(String value) {
        TextView view = text(value, 12, Typeface.BOLD);
        view.setPadding(0, dp(8), 0, 0);
        return view;
    }

    private TextView text(String value, int sizeSp, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setTextColor(0xFF1D1B20);
        view.setPadding(0, dp(4), 0, dp(4));
        return view;
    }

    private EditText input(String value) {
        EditText editText = new EditText(this);
        editText.setText(value);
        editText.setSingleLine(true);
        return editText;
    }

    private Button button(String value, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private interface LumosAction {
        void run(Lumos engine);
    }
}
