package com.benji.oasiso.client.fountain;

import java.util.*;

public final class EntropyFountainLayout {
    public static final int MAX_SIZE = 5, MAX_GAP = 15;

    public enum Axis {
        X, Y, Z;

        public Point world(double u, double along, double v) {
            return switch (this) {
                case X -> new Point(along, u, v);
                case Y -> new Point(u, along, v);
                case Z -> new Point(u, v, along);
            };
        }

        public Cell cell(int wx, int wy, int wz, int end) {
            return switch (this) {
                case X -> new Cell(wy, wx, wz, end, this);
                case Y -> new Cell(wx, wy, wz, end, this);
                case Z -> new Cell(wx, wz, wy, end, this);
            };
        }

        public int coordinate(int x, int y, int z) {
            return this == X ? x : (this == Y ? y : z);
        }
    }

    public record Point(double x, double y, double z) {
    }

    public record Cell(int x, int y, int z, int roof, Axis axis) {
        public Cell(int x, int y, int z, int roof) {
            this(x, y, z, roof, Axis.Y);
        }
    }

    public record Fountain(int x, int y, int z, int roof, int size, Axis axis) {
        public Fountain(int x, int y, int z, int roof, int size) {
            this(x, y, z, roof, size, Axis.Y);
        }

        public Point center() {
            return axis.world(centerX(), y + 1 + height() * .5, centerZ());
        }

        public Point start() {
            return axis.world(centerX(), y + 1.003, centerZ());
        }

        public Point min() {
            return axis.world(x, y + 1, z);
        }

        public Point max() {
            return axis.world(x + size, roof, z + size);
        }

        public Point direction() {
            return axis.world(0, 1, 0);
        }

        public double centerX() {
            return x + size * .5;
        }

        public double centerZ() {
            return z + size * .5;
        }

        public int height() {
            return roof - y - 1;
        }

        public int seed() {
            return (axis == Axis.Y ? Objects.hash(x, y, z, roof, size) : Objects.hash(x, y, z, roof, size, axis.ordinal())) & 65535;
        }
    }

    public static List<Fountain> merge(Collection<Cell> cells) {
        Set<Cell> free = new HashSet<>();
        for (Cell cell : cells) if (cell.roof - cell.y - 1 >= 1 && cell.roof - cell.y - 1 <= MAX_GAP) free.add(cell);
        List<Cell> ordered = new ArrayList<>(free);
        ordered.sort(Comparator.comparing(Cell::axis).thenComparingInt(Cell::y).thenComparingInt(Cell::roof).thenComparingInt(Cell::x).thenComparingInt(Cell::z));
        List<Fountain> result = new ArrayList<>();
        for (int size = MAX_SIZE; size >= 1; size--)
            for (Cell origin : ordered) {
                if (!free.contains(origin)) continue;
                boolean complete = true;
                for (int dx = 0; dx < size && complete; dx++)
                    for (int dz = 0; dz < size; dz++)
                        if (!free.contains(new Cell(origin.x + dx, origin.y, origin.z + dz, origin.roof, origin.axis))) {
                            complete = false;
                            break;
                        }
                if (!complete) continue;
                for (int dx = 0; dx < size; dx++)
                    for (int dz = 0; dz < size; dz++)
                        free.remove(new Cell(origin.x + dx, origin.y, origin.z + dz, origin.roof, origin.axis));
                result.add(new Fountain(origin.x, origin.y, origin.z, origin.roof, size, origin.axis));
            }
        return result;
    }

    private EntropyFountainLayout() {
    }
}
