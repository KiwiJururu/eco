package com.livingecology.data;

public enum StateType {
    FEAR("fear", "Medo"),
    RAGE("rage", "Raiva"),
    STRESS("stress", "Estresse"),
    TRUST("trust", "Confiança"),
    CURIOSITY("curiosity", "Curiosidade"),
    FATIGUE("fatigue", "Fadiga"),
    PAIN("pain", "Dor");

    private final String key;
    private final String displayName;

    StateType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() { return key; }
    public String displayName() { return displayName; }
}
