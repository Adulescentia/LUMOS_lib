package io.github.adulescentia.LUMOS_lib;

import org.joml.Vector3f;

/**
 * 가상의 IoT 디바이스 모델.
 * 요구사항: id, name, position 필수 필드 보유.
 */
public class Device {
    private final String id;
    private final String name;
    private final String type;
    private final Vector3f position;

    public final static String DEVICE_CONTROL = "control";

    // Detector 연동용 상대 방향 캐시
    private Vector3f relativeCoordinate = new Vector3f(0, 0, 0);

    static int currentId = 0;

    // 기존 테스트/코드 호환용 생성자
    public Device(String name, Vector3f position) {
        this("LEGACY_" + name, name, "UNKNOWN", position);
    }

    public String getControlTopic(){
        return name+"/"+DEVICE_CONTROL;
    }

    public Device(String id, String name, Vector3f position) {
        this(id, name, "UNKNOWN", position);
    }

    public Device(String id, String name, String type, Vector3f position) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.position = new Vector3f(position);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    // 기존 코드 호환용
    public Vector3f getCoordinate() {
        return getPosition();
    }

    public Vector3f getRelativeCoordinate() {
        return new Vector3f(relativeCoordinate);
    }

    public void updateRelativeCoordinate(Vector3f userPos) {
        Vector3f direction = new Vector3f(position).sub(userPos);
        if (direction.lengthSquared() > 0f) {
            this.relativeCoordinate = direction.normalize();
        } else {
            this.relativeCoordinate = new Vector3f(0, 0, 0);
        }
    }
}
