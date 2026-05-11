package io.github.adulescentia.LUMOS_lib;

import org.joml.Vector3f;

public class Detector {
    private Device[] devices;
    private Vector3f userCoordinate;

    public Detector(Device[] devices, Vector3f userCoordinate) {
        this.devices = devices;
        this.userCoordinate = userCoordinate;
        updateAllDevices(userCoordinate);
    }

    public void updateAllDevices(Vector3f newUserPos) {
        this.userCoordinate = newUserPos;
        for (Device device : devices) {
            device.updateRelativeCoordinate(newUserPos);
        }
    }


    public Device getDevice(Vector3f armVector) {
        if (devices == null || devices.length == 0) return null;

        Vector3f normalizedArm = new Vector3f(armVector).normalize();

        Device bestMatch = null;
        float maxDot = -1.0f;

        for (Device device : devices) {
            Vector3f deviceDir = device.getRelativeCoordinate();
            if (deviceDir == null) continue;

            float currentDot = normalizedArm.dot(deviceDir);

            if (currentDot > maxDot) {
                maxDot = currentDot;
                bestMatch = device;
            }
        }
        //should point out at least this much. ~~cos45
        return (maxDot > 0.7f) ? bestMatch : null;
    }
}