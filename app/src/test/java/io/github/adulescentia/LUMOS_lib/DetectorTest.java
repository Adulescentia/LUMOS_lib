package io.github.adulescentia.LUMOS_lib;

import org.joml.Vector3f;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;

public class DetectorTest {

    @Test
    public void testDeviceDetectionBoundary() {
        ArrayList<Device> devices = new ArrayList<>();
        // Z축 정면(0, 0, 5)에 기기 배치
        Device target = new Device("Target", new Vector3f(0, 0, 5));
        devices.add(target);

        // 초기 사용자 위치 (0, 0, 0)
        Detector detector = new Detector(devices, new Vector3f(0, 0, 0));

        // Case 1: Success Boundary (Angle < 53.13°)
        // cos(50°) ≈ 0.64 (Should be detected)
        float rad50 = (float) Math.toRadians(50);
        Vector3f arm50 = new Vector3f(0, (float)Math.sin(rad50), (float)Math.cos(rad50));
        assertNotNull("Should detect device at 50 degrees (Dot > 0.6)",
                detector.getDevice(arm50));

        // Case 2: Failure Boundary (Angle > 53.13°)
        // cos(55°) ≈ 0.57 (Should NOT be detected)
        float rad55 = (float) Math.toRadians(55);
        Vector3f arm55 = new Vector3f(0, (float)Math.sin(rad55), (float)Math.cos(rad55));
        assertNull("Should return null at 55 degrees (Dot < 0.6)",
                detector.getDevice(arm55));
    }

    @Test
    public void testDeviceSelectionWithMultipleDevices() {
        ArrayList<Device> devices = new ArrayList<>();
        // 두 기기가 근처에 있음
        Device near = new Device("NearDevice", new Vector3f(0.1f, 0, 5));
        Device far = new Device("FarDevice", new Vector3f(2.0f, 0, 5));
        devices.add(near);
        devices.add(far);

        Detector detector = new Detector(devices, new Vector3f(0, 0, 0));

        // 정면을 가리킬 때 더 각도가 가까운 NearDevice가 선택되어야 함
        Vector3f straight = new Vector3f(0, 0, 1);
        Device result = detector.getDevice(straight);

        assertNotNull(result);
        assertEquals("NearDevice", result.getName());
    }
}