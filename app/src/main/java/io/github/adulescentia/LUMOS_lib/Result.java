package io.github.adulescentia.LUMOS_lib;

import androidx.annotation.NonNull;

import org.joml.Vector3f;

public class Result implements Cloneable {
    private final Vector3f direction = new Vector3f(0, 0, 1);
    private final Vector3f cameraPos = new Vector3f(0, 0, 0);

    /**get user's current normalized direction
     * @return Vector*/
    @NonNull
    public Vector3f getDirection() {
        return new Vector3f(direction);
    }

    /**get user's currently selected Device
     * @return Device*/
    @NonNull
    public Device getSelectedDevice() {
        Device selected = Lumos.getDetector().getDevice(direction);
        if (selected != null) return selected;

        Device fallback = Lumos.getDetector().getDevice(new Vector3f(0, 0, 1));
        if (fallback != null) return fallback;

        Device created = new Lumos().registerDevice();
        if (created != null) return created;

        throw new IllegalStateException("No device available");
    }

    /**get user's current position ( it can be relative pos at fixed point but it should be consistent enough )*/
    @NonNull
    public Vector3f getCurrentPosition() {
        return new Vector3f(Lumos.getUser().getUserCoordinate());
    }

    /**get camera's position*/
    @NonNull
    public Vector3f getCameraPos() {
        return new Vector3f(cameraPos);
    }

    @NonNull
    @Override
    public Result clone() {
        return new Result();
    }
}
