package com.benji.oasiso.client.fountain;

import java.util.*;


public final class EntropyFountainLayout {
    public static final int MAX_SIZE = 5, MAX_GAP = 15;

    public record Cell(int x, int y, int z, int roof) {
    }

    public record Fountain(int x, int y, int z, int roof, int size) {
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
            return Objects.hash(x, y, z, roof, size) & 65535;
        }
    }

    public static List<Fountain> merge(Collection<Cell> cells) {
        Set<Cell> free = new HashSet<>();
        for (Cell cell : cells) if (cell.roof - cell.y - 1 >= 1 && cell.roof - cell.y - 1 <= MAX_GAP) free.add(cell);
        List<Cell> ordered = new ArrayList<>(free);
        ordered.sort(Comparator.comparingInt(Cell::y).thenComparingInt(Cell::roof).thenComparingInt(Cell::x).thenComparingInt(Cell::z));
        List<Fountain> result = new ArrayList<>();
        for (int size = MAX_SIZE; size >= 1; size--)
            for (Cell origin : ordered) {
                if (!free.contains(origin)) continue;
                boolean complete = true;
                for (int dx = 0; dx < size && complete; dx++)
                    for (int dz = 0; dz < size; dz++)
                        if (!free.contains(new Cell(origin.x + dx, origin.y, origin.z + dz, origin.roof))) {
                            complete = false;
                            break;
                        }
                if (!complete) continue;
                for (int dx = 0; dx < size; dx++)
                    for (int dz = 0; dz < size; dz++)
                        free.remove(new Cell(origin.x + dx, origin.y, origin.z + dz, origin.roof));
                result.add(new Fountain(origin.x, origin.y, origin.z, origin.roof, size));
            }
        return result;
    }

    private EntropyFountainLayout() {
    }
}
