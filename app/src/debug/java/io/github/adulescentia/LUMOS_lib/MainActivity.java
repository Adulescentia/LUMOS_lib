package io.github.adulescentia.LUMOS_lib;

import android.os.Bundle;
import android.widget.Button;
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
        setContentView(R.layout.activity_main);

        stateText = findViewById(R.id.stateText);
        logText = findViewById(R.id.logText);
        logScroll = findViewById(R.id.logScroll);

        lumos = new Lumos();
        lumos.initialize();
        lumos.registerExternalResultChannel(result -> runOnUiThread(() -> {
            stateText.setText(
                    "Direction: " + result.getDirection() + "\n" +
                    "Current Position: " + result.getCurrentPosition() + "\n" +
                    "Camera Position: " + result.getCameraPos());
            appendLog("[RESULT] snapshot updated");
        }));

        bindButtons();
        appendLog("LUMOS test app started");
    }

    private void bindButtons() {
        Button addDevice = findViewById(R.id.btnAddDevice);
        Button startProcess = findViewById(R.id.btnStartProcess);
        Button showDevices = findViewById(R.id.btnShowDevices);
        Button showSelected = findViewById(R.id.btnShowSelected);

        addDevice.setOnClickListener(v -> {
            Device d = lumos.registerDevice();
            if (d != null) {
                appendLog("[DEVICE] added: " + d.getName() + " (#" + d.getId() + ")");
            } else {
                appendLog("[DEVICE] add failed");
            }
        });

        startProcess.setOnClickListener(v -> {
            lumos.startIoTControlProcess();
            appendLog("[PROCESS] startIoTControlProcess called");
        });

        showDevices.setOnClickListener(v -> {
            int size = lumos.getDeviceList().size();
            appendLog("[DEVICE] total registered: " + size);
        });

        showSelected.setOnClickListener(v -> {
            try {
                Result result = new Result();
                Device selected = result.getSelectedDevice();
                appendLog("[SELECT] selected device: " + selected.getName());
            } catch (Exception e) {
                appendLog("[SELECT] none (" + e.getClass().getSimpleName() + ")");
            }
        });
    }

    private void appendLog(String line) {
        logText.append(line + "\n");
        logScroll.post(() -> logScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
