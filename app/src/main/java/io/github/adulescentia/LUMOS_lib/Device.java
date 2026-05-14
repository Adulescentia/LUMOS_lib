package io.github.adulescentia.LUMOS_lib;

import org.joml.Vector3f;

public class Device {
    static int currentId = 0;
    private Vector3f coordinate;
    private Vector3f relativeCoordinate;
    private final int id;
    private final String name;

    Device(String name, Vector3f coordinate ) {
        this.id = currentId;
        this.name = name;
        this.coordinate = coordinate;
        currentId ++;
    }

    public void updateRelativeCoordinate(Vector3f userPos) {
        this.relativeCoordinate = new Vector3f(this.coordinate).sub(userPos).normalize();
    }

    // --- Getters ---
    public Vector3f getCoordinate() { return coordinate; }
    public Vector3f getRelativeCoordinate() { return relativeCoordinate; }
    public String getName() { return name; }
    public int getId() { return id; }


    // --- Setters ---
    public void setCoordinate (Vector3f coordinate) { this.coordinate = coordinate; }

}