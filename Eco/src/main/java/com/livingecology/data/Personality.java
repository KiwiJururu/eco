package com.livingecology.data;

import java.util.EnumMap;
import java.util.Map;

public enum Personality {
    NORMAL("Normal"),
    LAZY("Preguiçoso"),
    WORRIED("Preocupado"),
    PLAYFUL("Brincalhão"),
    AGGRESSIVE("Agressivo"),
    WEAK("Fraco"),
    CURIOUS("Curioso"),
    CAUTIOUS("Cauteloso"),
    BOLD("Ousado"),
    PROTECTIVE("Protetor"),
    INDEPENDENT("Independente"),
    STUBBORN("Teimoso"),
    SOCIABLE("Sociável");

    private final String displayName;

    Personality(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() { return displayName; }

    public Map<AttributeType, Integer> modifiers() {
        EnumMap<AttributeType, Integer> out = new EnumMap<>(AttributeType.class);
        switch (this) {
            case LAZY -> {
                out.put(AttributeType.ALERT, -10);
                out.put(AttributeType.INSTINCT, -5);
            }
            case WORRIED -> {
                out.put(AttributeType.ALERT, 15);
                out.put(AttributeType.INSTINCT, 5);
            }
            case PLAYFUL -> {
                out.put(AttributeType.SOCIABILITY, 10);
                out.put(AttributeType.PERCEPTION, 5);
            }
            case AGGRESSIVE -> {
                out.put(AttributeType.TERRITORY, 10);
                out.put(AttributeType.INSTINCT, 5);
                out.put(AttributeType.CONSTITUTION, 5);
            }
            case WEAK -> out.put(AttributeType.CONSTITUTION, -20);
            case CURIOUS -> {
                out.put(AttributeType.PERCEPTION, 10);
                out.put(AttributeType.MEMORY, 5);
            }
            case CAUTIOUS -> {
                out.put(AttributeType.ALERT, 10);
                out.put(AttributeType.MEMORY, 10);
                out.put(AttributeType.INSTINCT, 5);
            }
            case BOLD -> {
                out.put(AttributeType.CONSTITUTION, 10);
                out.put(AttributeType.INSTINCT, 5);
                out.put(AttributeType.ALERT, -5);
            }
            case PROTECTIVE -> {
                out.put(AttributeType.SOCIABILITY, 10);
                out.put(AttributeType.TERRITORY, 10);
            }
            case INDEPENDENT -> {
                out.put(AttributeType.SOCIABILITY, -15);
                out.put(AttributeType.INSTINCT, 10);
            }
            case STUBBORN -> {
                out.put(AttributeType.TERRITORY, 10);
                out.put(AttributeType.CONSTITUTION, 5);
            }
            case SOCIABLE -> out.put(AttributeType.SOCIABILITY, 15);
            case NORMAL -> { }
        }
        return out;
    }
}
