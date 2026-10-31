package com.joshua.wasteland;

/** Pure geometry shared by the damage pass and the regression harness. */
public final class CombatGeometry {
    private CombatGeometry() {}
    public static boolean inArc(double dx, double dz, double facingX, double facingZ, double reach, double degrees) {
        double distance = dx * dx + dz * dz;
        if (distance > reach * reach) return false;
        if (distance < 1.0e-8 || degrees >= 360) return true;
        return (dx * facingX + dz * facingZ) / Math.sqrt(distance) >= Math.cos(Math.toRadians(degrees / 2)) - 1.0e-9;
    }
}
