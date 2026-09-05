package com.benji.oasiso.common.entity;

public enum ScarabUpgrade {

    NONE(0, "scarab.png", 1.0D, 1.0D, 1.0D, 1.0D, 1.0F, 1.0F, 1.0F),

    IRON(1, "scarab_iron.png", 1.30D, 0.80D, 0.90D, 1.0D, 0.78F, 0.70F, 0.65F),

    AMETHYST(2, "scarab_amethyst.png", 1.0D, 1.0D, 1.30D, 1.0D, 1.05F, 1.15F, 1.25F),

    COPPER(3, "scarab_copper.png", 0.60D, 0.90D, 1.0D, 2.0D, 1.15F, 1.0F, 1.35F),

    GOLD(4, "scarab_gold.png", 0.20D, 2.0D, 2.0D, 1.0D, 1.15F, 1.35F, 1.65F),

    DIAMOND(5, "scarab_diamond.png", 1.40D, 1.30D, 1.20D, 1.0D, 0.95F, 0.90F, 1.0F),

    EMERALD(6, "scarab_emerald.png", 1.0D, 1.20D, 1.0D, 1.0D, 1.0F, 1.05F, 0.85F),

    NETHERITE(7, "scarab_netherite.png", 3.0D, 0.50D, 0.80D, 1.0D, 0.70F, 0.55F, 0.50F);

    private final int id;
    private final String texture;

    private final double healthMultiplier;
    private final double walkSpeedMultiplier;
    private final double flightSpeedMultiplier;
    private final double jumpHeightMultiplier;

    private final float pitchMultiplier;
    private final float rollMultiplier;
    private final float tiltResponseMultiplier;

    ScarabUpgrade(int id, String texture, double healthMultiplier, double walkSpeedMultiplier, double flightSpeedMultiplier, double jumpHeightMultiplier, float pitchMultiplier, float rollMultiplier, float tiltResponseMultiplier) {
        this.id = id;
        this.texture = texture;
        this.healthMultiplier = healthMultiplier;
        this.walkSpeedMultiplier = walkSpeedMultiplier;
        this.flightSpeedMultiplier = flightSpeedMultiplier;
        this.jumpHeightMultiplier = jumpHeightMultiplier;
        this.pitchMultiplier = pitchMultiplier;
        this.rollMultiplier = rollMultiplier;
        this.tiltResponseMultiplier = tiltResponseMultiplier;
    }

    public int id() {
        return this.id;
    }

    public String texture() {
        return this.texture;
    }

    public double healthMultiplier() {
        return this.healthMultiplier;
    }

    public double walkSpeedMultiplier() {
        return this.walkSpeedMultiplier;
    }

    public double flightSpeedMultiplier() {
        return this.flightSpeedMultiplier;
    }

    public double jumpHeightMultiplier() {
        return this.jumpHeightMultiplier;
    }

    public float pitchMultiplier() {
        return this.pitchMultiplier;
    }

    public float rollMultiplier() {
        return this.rollMultiplier;
    }

    public float tiltResponseMultiplier() {
        return this.tiltResponseMultiplier;
    }

    public static ScarabUpgrade byId(int id) {
        for (ScarabUpgrade upgrade : values()) {

            if (upgrade.id == id) {
                return upgrade;
            }
        }

        return NONE;
    }
}