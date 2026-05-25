package io.github.adulescentia.LUMOS_lib;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import org.joml.Vector3f;

public class Result implements Cloneable {

    private final Vector3f direction = new Vector3f(0, 0, 0);
    @Nullable
    private PoseLandmarkerResult rawResult;
    private long timestampMs;

    public void update(@NonNull Vector3f newDirection, @Nullable PoseLandmarkerResult rawResult, long timestampMs) {
        this.direction.set(newDirection);
        this.rawResult = rawResult;
        this.timestampMs = timestampMs;
    }

    @NonNull
    public Vector3f getDirection() {
        return new Vector3f(direction);
    }

    @Nullable
    public Device getSelectedDevice() {
        return Lumos.detector.getDevice(getDirection());
    }

    @NonNull
    public Vector3f getCurrentPosition() {
        return Lumos.user.getUserCoordinate();
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    @Nullable
    public PoseLandmarkerResult getRawResult() {
        return rawResult;
    }

    @NonNull
    @Override
    public Result clone() {
        Result copied = new Result();
        copied.update(getDirection(), rawResult, timestampMs);
        return copied;
    }
}
