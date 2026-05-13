package io.github.adulescentia.LUMOS_lib;

import org.joml.Vector3f;

public class User {
    Vector3f userCoordinate;

    User () {
        this.userCoordinate = new Vector3f(0,0,0);
    }

    /**/
    void setUserCoordinate(float wallWidth, float wallLength, float shoulderWidth, Vector3f leftShoulder, Vector3f rightShoulder) {//camera's pos is 0,0, +z behind the camera, +x to the right
        this.userCoordinate.z = (shoulderWidth*wallLength)/(wallWidth*Math.abs(leftShoulder.sub(rightShoulder).length()));
        this.userCoordinate.x = (leftShoulder.x + rightShoulder.x)/2;
    }

    Vector3f getUserCoordinate() {
        return userCoordinate;
    }
}
