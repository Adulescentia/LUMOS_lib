package io.github.adulescentia.LUMOS_lib;

import org.joml.Vector3f;

public class Device {
    private Vector3f coordinate;
    private Vector3f relativeCoordinate;
    private String name;

    Device(String name, Vector3f coordinate) {
        this.name = name;
        this.coordinate = coordinate;
    }

    public void updateRelativeCoordinate(Vector3f userPos) {
        this.relativeCoordinate = new Vector3f(this.coordinate).sub(userPos).normalize();
    }

    // --- Getters ---
    public Vector3f getCoordinate() { return coordinate; }
    public Vector3f getRelativeCoordinate() { return relativeCoordinate; }
    public String getName() { return name; }

    // --- Setters ---
    public void setCoordinate(Vector3f coordinate) { this.coordinate = coordinate; }
    public void setName(String name) { this.name = name; }
}