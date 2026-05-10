package io.github.adulescentia.LUMOS_lib;

public class Boundary {
    float start; //radian
    float end;

    public Boundary(float start, float end) {
        this.start = start;
        this.end = end;
    }

    public boolean isInBoundary(float x, float z) {
        float angle = (float) Math.atan2(z, x);

        if (start <= end) {
            return angle >= start && angle <= end;
        }
        return angle >= start || angle <= end;
    }
}