package io.github.adulescentia.LUMOS_lib;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Lumos lumos;
    private TextView stateText;
    private TextView logText;
    private ScrollView logScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            stateText = findViewById(R.id.stateText);
            logText = findViewById(R.id.logText);
            logScroll = findViewById(R.id.logScroll);

            lumos = Lumos.getInstance();
            lumos.initialize();
            lumos.registerExternalResultChannel(result -> runOnUiThread(() -> {
                String selected = "none";
                try { selected = result.getSelectedDevice().getName(); } catch (Exception ignore) {}
                stateText.setText(
                        "Direction: " + result.getDirection() + "\n" +
                                "Selected: " + selected + "\n" +
                                "Command: " + result.getCommandType() + " / " + result.getCommandDetail() + "\n" +
                                "Current Position: " + result.getCurrentPosition() + "\n" +
                                "Camera Position: " + result.getCameraPos());
                appendLog("[RESULT] updated");
            }));

            bindButtons();
            appendLog("LUMOS test app started");
        } catch (Throwable t) {
            if (stateText != null) {
                stateText.setText("Startup failed: " + t.getClass().getSimpleName() + "\n" + String.valueOf(t.getMessage()));
            }
            appendLog("[FATAL] startup error: " + t);
        }
    }

    private void bindButtons() {
        Button addDevice = findViewById(R.id.btnAddDevice);
        Button startProcess = findViewById(R.id.btnStartProcess);
        Button showDevices = findViewById(R.id.btnShowDevices);
        Button showSelected = findViewById(R.id.btnShowSelected);
        Button btnFist = findViewById(R.id.btnFist);
        Button btnOne = findViewById(R.id.btnOne);
        Button btnPalm = findViewById(R.id.btnPalm);
        Button btnV = findViewById(R.id.btnV);

        EditText inputName = findViewById(R.id.inputName);
        EditText inputType = findViewById(R.id.inputType);
        EditText inputX = findViewById(R.id.inputX);
        EditText inputY = findViewById(R.id.inputY);
        EditText inputZ = findViewById(R.id.inputZ);

        addDevice.setOnClickListener(v -> {
            if (lumos == null) { appendLog("[ERROR] lumos is null"); return; }
            try {
                String name = inputName.getText().toString().trim();
                String type = inputType.getText().toString().trim();
                double x = Double.parseDouble(inputX.getText().toString().trim());
                double y = Double.parseDouble(inputY.getText().toString().trim());
                double z = Double.parseDouble(inputZ.getText().toString().trim());

                if (name.isEmpty()) name = "Unnamed";
                if (type.isEmpty()) type = "UNKNOWN";

                Device d = lumos.registerDevice(x, y, z, name, type);
                if (d != null) {
                    appendLog("[DEVICE] added: " + d.getName() + " type=" + d.getType() + " pos=" + d.getPosition());
                } else {
                    appendLog("[DEVICE] add failed");
                }
            } catch (Exception e) {
                appendLog("[ERROR] invalid device input: " + e.getMessage());
            }
        });

        startProcess.setOnClickListener(v -> {
            if (lumos == null) { appendLog("[ERROR] lumos is null"); return; }
            lumos.startIoTControlProcess();
            appendLog("[PROCESS] started");
        });

        showDevices.setOnClickListener(v -> {
            if (lumos == null) { appendLog("[ERROR] lumos is null"); return; }
            appendLog("[DEVICE] total registered: " + lumos.getDeviceList().size());
        });

        showSelected.setOnClickListener(v -> {
            try {
                Result result = lumos.getLatestResultSnapshot();
                Device selected = result.getSelectedDevice();
                appendLog("[SELECT] selected: " + selected.getName() + " / type=" + selected.getType());
            } catch (Exception e) {
                appendLog("[SELECT] none (" + e.getClass().getSimpleName() + ")");
            }
        });

        btnFist.setOnClickListener(v -> { lumos.updateGesture(GestureStateManager.Gesture.FIST, 0.5f); appendLog("[GESTURE] FIST");});
        btnOne.setOnClickListener(v -> { lumos.updateGesture(GestureStateManager.Gesture.ONE_FINGER, 0.5f); appendLog("[GESTURE] ONE_FINGER");});
        btnPalm.setOnClickListener(v -> { lumos.updateGesture(GestureStateManager.Gesture.PALM, 0.5f); appendLog("[GESTURE] PALM");});
        btnV.setOnClickListener(v -> { lumos.updateGesture(GestureStateManager.Gesture.V_SIGN, 0.5f); appendLog("[GESTURE] V_SIGN");});
    }

    private void appendLog(String line) {
        if (logText == null) return;
        logText.append(line + "\n");
        if (logScroll != null) logScroll.post(() -> logScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
