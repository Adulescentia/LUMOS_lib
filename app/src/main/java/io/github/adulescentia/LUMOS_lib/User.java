package io.github.adulescentia.LUMOS_lib;

import org.joml.Vector3f;

public class User {
    Vector3f userCoordinate;

    void setUserCoordinate(float wallWidth, float wallLength, float shoulderWidth) {//camera's pos is 0,0, +z behind the camera, +x to the right
        //todo - 벽 너비와 어깨 너비 비교해 유저 위치 파악 알고리즘 짜기
    }

    Vector3f getUserCoordinate() {
        return userCoordinate;
    }
}
