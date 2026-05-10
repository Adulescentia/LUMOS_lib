package io.github.adulescentia.LUMOS_lib;

import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class BoundaryMap {
    float[] upperBoundary;
    float[] midBoundary;
    float[] lowerBoundary;
    float[] userCordiante;
    float armYAngle;
    Device[] devices;
    Device[] DevicesSortedByAngle;

    Vector3f yAxis = new Vector3f(0.0f, 1.0f, 0.0f);
    BoundaryMap(Device[] devices) {
        this.devices = devices;

        //make sorted list
        DevicesSortedByAngle = devices.clone();
        Arrays.sort(DevicesSortedByAngle, (d1, d2) -> {
            double a1 = Math.atan2(d1.coordinate.z, d1.coordinate.x);
            double a2 = Math.atan2(d2.coordinate.z, d2.coordinate.x);
            return Double.compare(a1, a2);
        });



        for (int i = 0; i < devices.length; i++) {

        }

        //todo boundary 계산 후 넣기
    }


    public void setupDeviceBoundaries(List<Device> sortedDevices) {
        int n = sortedDevices.size();
        if (n == 0) return;

        for (int i = 0; i < n; i++) {
            Device current = sortedDevices.get(i);

            Device prev = sortedDevices.get((i - 1 + n) % n);
            Device next = sortedDevices.get((i + 1) % n);

            float currentAngle = (float) Math.atan2(current.coordinate.z, current.coordinate.x);
            float prevAngle = (float) Math.atan2(prev.coordinate.z, prev.coordinate.x);
            float nextAngle = (float) Math.atan2(next.coordinate.z, next.coordinate.x);

            float start = getMidAngle(prevAngle, currentAngle);
            float end = getMidAngle(currentAngle, nextAngle);

            current.boundary = new Boundary(start, end);
        }
    }
    private float getMidAngle(float a, float b) {
        float diff = b - a;

        // circular link
        if (diff > Math.PI) diff -= 2 * Math.PI;
        if (diff < -Math.PI) diff += 2 * Math.PI;

        float mid = a + diff / 2;

        // -ㅠ~ㅠ
        if (mid > Math.PI) mid -= 2 * Math.PI;
        if (mid < -Math.PI) mid += 2 * Math.PI;

        return mid;
    }
    Device getDevice(Vector3f armVector) {

    }



}
