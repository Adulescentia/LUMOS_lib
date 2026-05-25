package io.github.adulescentia.LUMOS_lib;

import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.joml.Vector3f;

public class MainActivity extends AppCompatActivity {

    private Lumos lumos;
    private TextView statusBox;
    private float wristY = 0.5f;
    private String lastAction = "none";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusBox = findViewById(R.id.statusBox);
        lumos = new Lumos(this);

        lumos.registerDevice("Lamp", new Vector3f(0f, 0f, 5f));
        lumos.registerDevice("Fan", new Vector3f(2f, 0f, 5f));
        lumos.registerDevice("TV", new Vector3f(-2f, 0f, 5f));

        lumos.initialize();
        lumos.registerExternalResultChannel(result -> runOnUiThread(this::updateStatus));

        wireGestureButtons();
        wireWristSeek();
        updateStatus();
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
                + "Guide: 카메라 프레임을 LUMOS.ingestExternalCameraFrame(...)로 전달하면 MediaPipe 결과가 반영됩니다.";

        statusBox.setText(msg);
    }
}
