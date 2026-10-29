package com.benji.oasiso.common.shrine;

public interface ShrineSoul {
    int DISSOLVE_TICKS = 60;
    boolean isShrineSoul();
    int soulDissolveAge();
    void setSoulDissolveAge(int age);
    void beginSoulDissolve();
    void releaseShrineSoul();
    default boolean isSoulDissolving() { return isShrineSoul() && soulDissolveAge() >= 0; }
}
