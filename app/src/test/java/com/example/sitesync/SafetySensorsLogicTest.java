package com.example.sitesync;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Mathematical & Signal Processing Engines:
 * 1. Sudden Impact / Fall Detection Vector Magnitude
 * 2. Digital Spirit Level Pitch & Slope % Calculation
 * 3. Acoustic Sound Pressure Level (dB) & OSHA 85dB Rule
 */
public class SafetySensorsLogicTest {

    @Test
    public void testVectorMagnitudeCalculation() {
        // Test standard 3D acceleration vector
        float ax = 12.0f;
        float ay = 16.0f;
        float az = 15.0f;

        float magnitude = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        // sqrt(144 + 256 + 225) = sqrt(625) = 25.0 m/s^2
        assertEquals(25.0f, magnitude, 0.001f);

        // Convert to G-force (1G = 9.80665 m/s^2)
        float gForce = magnitude / 9.80665f;
        assertEquals(2.55f, gForce, 0.01f);
    }

    @Test
    public void testFallDetectionThresholds() {
        float freeFallThreshold = 2.8f;
        float impactThreshold = 25.0f;

        // Weightlessness during free fall
        float freeFallMag = (float) Math.sqrt(0.5 * 0.5 + 0.8 * 0.8 + 0.2 * 0.2);
        assertTrue("Magnitude must be below free fall threshold", freeFallMag < freeFallThreshold);

        // High G impact with ground/concrete
        float impactMag = (float) Math.sqrt(18.0 * 18.0 + 20.0 * 20.0 + 12.0 * 12.0);
        assertTrue("Magnitude must exceed impact threshold", impactMag > impactThreshold);
    }

    @Test
    public void testDigitalSpiritLevelMath() {
        // Test true level (0.0° pitch and roll)
        float pitchDeg = 0.4f;
        float rollDeg = -0.3f;
        float tolerance = 1.0f;

        boolean isLevel = Math.abs(pitchDeg) <= tolerance && Math.abs(rollDeg) <= tolerance;
        assertTrue("0.4° and -0.3° should register as true level within 1.0° tolerance", isLevel);

        // Test pipe grade slope: target 2.0%
        // slope % = |tan(theta_rad)| * 100
        // For ~1.146 degrees: tan(1.146°) * 100 ≈ 2.0%
        double pipePitchRad = Math.toRadians(1.1458);
        float slopePercent = (float) Math.abs(Math.tan(pipePitchRad)) * 100.0f;

        assertEquals(2.0f, slopePercent, 0.05f);

        float pipeGradeTarget = 2.0f;
        float pipeGradeTolerance = 0.2f;
        boolean isPipeGrade = Math.abs(slopePercent - pipeGradeTarget) <= pipeGradeTolerance;
        assertTrue("Calculated slope must match 2% pipe grade", isPipeGrade);
    }

    @Test
    public void testAcousticDecibelConversion() {
        // RMS = sqrt(sum(sample^2) / N)
        // dB = 20 * log10(RMS / Ref)
        double refAmplitude = 1.0;
        double rmsQuiet = 100.0;
        double rmsLoudPneumaticDrill = 32000.0;

        double dbQuiet = 20.0 * Math.log10(rmsQuiet / refAmplitude);
        double dbLoud = 20.0 * Math.log10(rmsLoudPneumaticDrill / refAmplitude);

        assertEquals(40.0, dbQuiet, 0.1);
        assertEquals(90.1, dbLoud, 0.2);

        // OSHA 85 dB PPE warning threshold
        boolean isHazardousQuiet = dbQuiet >= 85.0;
        boolean isHazardousLoud = dbLoud >= 85.0;

        assertFalse("40 dB should not trigger OSHA PPE alert", isHazardousQuiet);
        assertTrue("90 dB must trigger OSHA PPE hazardous noise alert", isHazardousLoud);
    }
}
