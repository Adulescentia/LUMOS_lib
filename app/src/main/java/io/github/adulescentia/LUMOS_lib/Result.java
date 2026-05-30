package io.github.adulescentia.LUMOS_lib;

import androidx.annotation.NonNull;

import org.joml.Vector3f;

public class Result implements Cloneable {
    public enum CommandType {
        NONE,
        DEVICE_SELECTION_TOGGLED,
        DEVICE_POWER_TOGGLED,
        DEVICE_MODE_APPLIED
    }
    // 요구사항 데이터 필드
    private Vector3f direction;
    private Vector3f currentPosition;
    private Vector3f cameraPos;
    private Device selectedDevice;
    private CommandType commandType;
    private String commandDetail;

    public Result() {
        this(new Vector3f(0, 0, 1), new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), null, CommandType.NONE, "");
    }

    public Result(@NonNull Vector3f direction,
                  @NonNull Vector3f currentPosition,
                  @NonNull Vector3f cameraPos,
                  Device selectedDevice,
                  @NonNull CommandType commandType,
                  @NonNull String commandDetail) {
        this.direction = new Vector3f(direction);
        this.currentPosition = new Vector3f(currentPosition);
        this.cameraPos = new Vector3f(cameraPos);
        this.selectedDevice = selectedDevice;
        this.commandType = commandType;
        this.commandDetail = commandDetail;
    }

    // Lumos 내부 루프 갱신용 (외부 공개 최소화)
    void update(@NonNull Vector3f direction,
                @NonNull Vector3f currentPosition,
                @NonNull Vector3f cameraPos,
                Device selectedDevice,
                @NonNull CommandType commandType,
                @NonNull String commandDetail) {
        this.direction = new Vector3f(direction);
        this.currentPosition = new Vector3f(currentPosition);
        this.cameraPos = new Vector3f(cameraPos);
        this.selectedDevice = selectedDevice;
        this.commandType = commandType;
        this.commandDetail = commandDetail;
    }

    @NonNull
    public Vector3f getDirection() {
        return new Vector3f(direction);
    }

    @NonNull
    public Device getSelectedDevice() {
        if (selectedDevice != null) {
            return selectedDevice;
        }
        throw new IllegalStateException("No selected device in current Result");
    }

    @NonNull
    public Vector3f getCurrentPosition() {
        return new Vector3f(currentPosition);
    }

    @NonNull
    public Vector3f getCameraPos() {
        return new Vector3f(cameraPos);
    }

    @NonNull
    public CommandType getCommandType() {
        return commandType;
    }

    @NonNull
    public String getCommandDetail() {
        return commandDetail;
    }

    @NonNull
    @Override
    public Result clone() {
        // Vector3f는 가변 객체이므로 반드시 깊은 복사
        return new Result(
                new Vector3f(this.direction),
                new Vector3f(this.currentPosition),
                new Vector3f(this.cameraPos),
                this.selectedDevice,
                this.commandType,
                this.commandDetail
        );
    }
}
