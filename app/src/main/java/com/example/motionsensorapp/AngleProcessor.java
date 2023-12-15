package com.example.motionsensorapp;

public class AngleProcessor {
    private static final float SAMPLE_RATE = 12.0f; // Replace with your actual sensor sample rate
    private static final float BETA = 0.041f; // Filter gain, adjust as needed
    private static final float RAD2DEG = 180.0f / (float) Math.PI;

    private float q0 = 1.0f;
    private float q1 = 0.0f;
    private float q2 = 0.0f;
    private float q3 = 0.0f;

    private float[] angle = new float[3];

    public AngleProcessor() {
    }

    public void update(float gx, float gy, float gz, float ax, float ay, float az) {
        // Normalize accelerometer readings
        float recipNorm = invSqrt(ax * ax + ay * ay + az * az);
        ax *= recipNorm;
        ay *= recipNorm;
        az *= recipNorm;

        // Estimated direction of gravity and magnetic field
        float halfvx = q1 * q3 - q0 * q2;
        float halfvy = q0 * q1 + q2 * q3;
        float halfvz = q0 * q0 - 0.5f + q3 * q3;

        // Error is cross product between estimated direction and measured direction of gravity
        float halfex = (ay * halfvz - az * halfvy);
        float halfey = (az * halfvx - ax * halfvz);
        float halfez = (ax * halfvy - ay * halfvx);

        // Compute and apply integral feedback
        gx += BETA * halfex;
        gy += BETA * halfey;
        gz += BETA * halfez;

        // Compute rate of change of quaternion
        float halfq0 = 0.5f * q0;
        float halfq1 = 0.5f * q1;
        float halfq2 = 0.5f * q2;
        float halfq3 = 0.5f * q3;

        float qa = halfq0 * gx + halfq2 * gy - halfq3 * gz;
        float qb = halfq1 * gx - halfq3 * gy + halfq2 * gz;
        float qc = halfq1 * gy + halfq2 * gx + halfq0 * gz;
        float qd = -halfq0 * gy - halfq1 * gz + halfq2 * gx;

        // Integrate rate of change of quaternion to yield quaternion
        q0 += (-halfq1 * gx - halfq2 * gy - halfq3 * gz) * (1.0f / SAMPLE_RATE);
        q1 += (qa * q0 + qb * q3 - qc * q2 - qd * q1) * (1.0f / SAMPLE_RATE);
        q2 += (qa * q1 - qb * q2 + qc * q3 - qd * q0) * (1.0f / SAMPLE_RATE);
        q3 += (qa * q2 + qb * q1 - qc * q0 + qd * q3) * (1.0f / SAMPLE_RATE);

        // Normalize quaternion
        recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        q0 *= recipNorm;
        q1 *= recipNorm;
        q2 *= recipNorm;
        q3 *= recipNorm;

        // Quaternion to angle
        // Roll (x-axis rotation)
        double sinr_cosp = 2 * (q0 * q1 + q2 * q3);
        double cosr_cosp = 1 - 2 * (q1 * q1 + q2 * q2);
        angle[0] = (float) Math.atan2(sinr_cosp, cosr_cosp) * RAD2DEG;

        // Pitch (y-axis rotation)
        double sinp = 2 * (q0 * q2 - q3 * q1);
        angle[1] = (float) Math.asin(sinp) * RAD2DEG;

        // Yaw (z-axis rotation)
        double siny_cosp = 2 * (q0 * q3 + q1 * q2);
        double cosy_cosp = 1 - 2 * (q2 * q2 + q3 * q3);
        angle[2] = (float) Math.atan2(siny_cosp, cosy_cosp) * RAD2DEG;
    }

    private float invSqrt(float x) {
        // Fast inverse square root approximation
        float halfx = 0.5f * x;
        float y = x;
        int i = Float.floatToRawIntBits(y);
        i = 0x5f3759df - (i >> 1);
        y = Float.intBitsToFloat(i);
        y = y * (1.5f - halfx * y * y);
        return y;
    }

    public float[] getQuaternion() {
        return new float[]{q0, q1, q2, q3};
    }
    public float[] getAngle(){ return new float[]{angle[0], angle[1], angle[2]}; }
}
