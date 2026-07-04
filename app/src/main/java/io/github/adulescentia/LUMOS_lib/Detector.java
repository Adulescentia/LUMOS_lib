package io.github.adulescentia.LUMOS_lib;

import org.joml.Vector3f;

import java.util.ArrayList;

public class Detector {
    private ArrayList<Device> devices;
    private Vector3f userCoordinate;

    public Detector(ArrayList<Device> devices, Vector3f userCoordinate) {
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

    public void addDevice(Device device) {
        devices.add(device);
    }


    public Device getDevice(Vector3f armVector) {
        if (devices == null || devices.isEmpty()) return null;
        if (armVector == null || armVector.lengthSquared() == 0.0f) return null;

        Vector3f normalizedArm = new Vector3f(armVector).normalize();

        Device bestMatch = null;
        float maxDot = -1.0f;

        for (Device device : devices) {
            Vector3f deviceDir = device.getRelativeCoordinate();
            if (deviceDir == null) continue;

            float currentDot = normalizedArm.dot(new Vector3f(deviceDir).normalize());

            if (currentDot > maxDot) {
                maxDot = currentDot;
                bestMatch = device;
            }
        }
        //should point out at least this much. ~~cos45 = 0.7 -> 0.6
        return (maxDot > 0.6f) ? bestMatch : null;
    }
}