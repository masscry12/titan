package com.titanmod.entity;

/**
 * Modo de comportamento do Titã quando não há piloto dentro dele.
 * Os comportamentos em si entram no marco M5; por ora o modo só é armazenado e sincronizado.
 */
public enum TitanAIMode {
    FOLLOW,
    GUARD;

    public static TitanAIMode byOrdinal(int ordinal) {
        TitanAIMode[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
