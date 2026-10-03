package com.benji.oasiso.common.entity;

public final class ApolArenaShape {
    public static final int SIZE = 7, DEPTH = 8;

    public static boolean contains(int x, int z) {
        if (x < 0 || z < 0 || x >= SIZE || z >= SIZE) return false;
        double dx = x - 3.0;
        double dz = z - 3.0;
        return dx * dx + dz * dz <= 3.5 * 3.5;
    }

    public static int first(int row) {
        for (int x = 0; x < SIZE; x++) if (contains(x, row)) return x;
        return SIZE;
    }

    public static int last(int row) {
        for (int x = SIZE - 1; x >= 0; x--) if (contains(x, row)) return x;
        return -1;
    }

    public static boolean rim(int x, int z) {
        return contains(x, z) && (!contains(x - 1, z) || !contains(x + 1, z) || !contains(x, z - 1) || !contains(x, z + 1));
    }

    public static boolean intersects(double minX, double minZ, double maxX, double maxZ) {
        for (int row = 0; row < SIZE; row++)
            if (maxZ > row && minZ < row + 1 && maxX > first(row) && minX < last(row) + 1) return true;
        return false;
    }

    private ApolArenaShape() {
    }
}
