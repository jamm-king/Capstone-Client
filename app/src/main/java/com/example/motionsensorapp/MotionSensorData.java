package com.example.motionsensorapp;

class MotionSensorData {
    public float timestamp;
    public float angleX;
    public float angleY;
    public float angleZ;
    public MotionSensorData(float timestamp, float angleX, float angleY, float angleZ) {
        this.timestamp = timestamp;
        this.angleX = angleX;
        this.angleY = angleY;
        this.angleZ = angleZ;
    }
    public void print() {
        System.out.println(String.format("x: %f, y: %f, z: %f (%f)",
                angleX, angleY, angleZ, timestamp));
    }
}
