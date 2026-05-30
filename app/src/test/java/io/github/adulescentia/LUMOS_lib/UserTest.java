package io.github.adulescentia.LUMOS_lib;

import static org.junit.Assert.assertEquals;
import org.joml.Vector3f;
import org.junit.Test;

public class UserTest {

    @Test
    public void testSetUserCoordinate_Calculation() {
        User user = new User();

        // 가상의 환경 파라미터
        float wallWidth = 10.0f;
        float wallLength = 20.0f;
        float shoulderWidth = 0.5f; // 실제 어깨 너비 (m)

        // 카메라에서 감지된 어깨 좌표 (정중앙에 서 있는 경우)
        // 왼쪽 어깨와 오른쪽 어깨의 x 차이가 0.1이라고 가정
        Vector3f leftShoulder = new Vector3f(-0.05f, 0, 0);
        Vector3f rightShoulder = new Vector3f(0.05f, 0, 0);

        user.setUserCoordinate(wallWidth, wallLength, shoulderWidth, leftShoulder, rightShoulder);

        Vector3f result = user.getUserCoordinate();

        // 1. X축 검증: (-0.05 + 0.05) / 2 = 0 이 되어야 함
        assertEquals("X coordinate should be the center of shoulders", 0.0f, result.x, 0.001f);

        // 2. Z축 검증 수식: (0.5 * 20.0) / (10.0 * 0.1) = 10.0 / 1.0 = 10.0
        assertEquals("Z coordinate calculation should be accurate", 10.0f, result.z, 0.001f);
    }

    @Test
    public void testSetUserCoordinate_ShiftedPosition() {
        User user = new User();

        // 사용자가 오른쪽으로 치우쳐 있는 상황 테스트
        Vector3f leftShoulder = new Vector3f(0.4f, 0, 0);
        Vector3f rightShoulder = new Vector3f(0.6f, 0, 0);

        user.setUserCoordinate(10f, 20f, 0.5f, leftShoulder, rightShoulder);

        // X축: (0.4 + 0.6) / 2 = 0.5
        assertEquals("X coordinate should reflect shifted user position", 0.5f, user.getUserCoordinate().x, 0.001f);
    }
}