package com.benji.oasiso.common.entity;

public interface MiniBossHealthBar {
    // SHOWN true/false (maybe for cutscenes idk)
    default boolean showMiniBossHealthBar() {
        return true;
    }
    //POSition ABOVE Head
    default double getMiniBossHealthBarOffset() {
        return 0.65D;
    }
    default float getMiniBossHealthBarScale() {
        return 0.025F;
    }
}
