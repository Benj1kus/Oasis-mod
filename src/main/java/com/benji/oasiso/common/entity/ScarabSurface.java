package com.benji.oasiso.common.entity;

import net.minecraft.core.Direction;

public enum ScarabSurface {

    FLOOR(0, Direction.UP, 0.0F, 0.0F),

    WALL_NORTH(1, Direction.NORTH, 90.0F, 0.0F),
    WALL_SOUTH(2, Direction.SOUTH, 90.0F, 0.0F),
    WALL_WEST(3, Direction.WEST, 90.0F, 0.0F),
    WALL_EAST(4, Direction.EAST, 90.0F, 0.0F),

    CEILING(5, Direction.DOWN, 0.0F, 180.0F);

    private final int id;
    private final Direction normal;

    private final float pitch;
    private final float roll;

    ScarabSurface(int id, Direction normal, float pitch, float roll) {
        this.id = id;
        this.normal = normal;
        this.pitch = pitch;
        this.roll = roll;
    }

    public int id() {
        return this.id;
    }

    public Direction normal() {
        return this.normal;
    }

    public float pitch() {
        return this.pitch;
    }

    public float roll() {
        return this.roll;
    }

    public boolean isWall() {
        return this == WALL_NORTH || this == WALL_SOUTH || this == WALL_WEST || this == WALL_EAST;
    }

    public static ScarabSurface fromWallNormal(Direction normal) {
        return switch (normal) {
            case NORTH -> WALL_NORTH;
            case SOUTH -> WALL_SOUTH;
            case WEST -> WALL_WEST;
            case EAST -> WALL_EAST;

            default -> FLOOR;
        };
    }

    public static ScarabSurface byId(int id) {
        for (ScarabSurface surface : values()) {

            if (surface.id == id) {
                return surface;
            }
        }

        return FLOOR;
    }
}