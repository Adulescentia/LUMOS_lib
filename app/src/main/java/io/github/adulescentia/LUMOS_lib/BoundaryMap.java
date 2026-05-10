package io.github.adulescentia.LUMOS_lib;

import org.joml.Vector3f;

public class BoundaryMap {
    float[] upperBoundary;
    float[] midBoundary;
    float[] lowerBoundary;
    float[] userCordiante;
    float armYAngle;

    Vector3f yAxis = new Vector3f(0.0f, 1.0f, 0.0f);
//    BoundaryMap(Device[] Devices) {
//        for (int i = 0; i < Devices.length; i++) {
//
//        }
//
//        //todo boundary 계산 후 넣기
//    }
//    setboundary
//    int getDeviceNum(Vector3f armVector) {
//        armYAngle = (float)Math.atan2(armVector.z, armVector.x);
//        //upper
//        if (armVector.angle(yAxis) > 13){
//            if (upperBoundary[upperBoundary.length-1] <= armYAngle && upperBoundary[0] >= armYAngle) {
//                return 0;
//            }
//            for (int i = 1; i< upperBoundary.length; i++) {
//                if (upperBoundary[i-1] <= armYAngle && upperBoundary[i] >= armYAngle) {
//                    return i;
//                }
//            }
//        //middle
//        } else if (13 >= armVector.angle(yAxis)&& armVector.angle(yAxis) >= -13) {
//            if (midBoundary[midBoundary.length-1] <= armYAngle && midBoundary[0] >= armYAngle) {
//                return 0;
//            }
//            for (int i = 1; i< midBoundary.length; i++) {
//                if (midBoundary[i-1] <= armYAngle && midBoundary[i] >= armYAngle) {
//                    return i;
//                }
//            }
//        //lower
//        } else if (armVector.angle(yAxis) < 13) {
//            if (lowerBoundary[lowerBoundary.length-1] <= armYAngle && lowerBoundary[0] >= armYAngle) {
//                return 0;
//            }
//            for (int i = 1; i< lowerBoundary.length; i++) {
//                if (lowerBoundary[i-1] <= armYAngle && lowerBoundary[i] >= armYAngle) {
//                    return i;
//                }
//            }
//        }
//        return 0;
//    }

}
