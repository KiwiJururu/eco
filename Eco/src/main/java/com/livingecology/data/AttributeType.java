package com.livingecology.data;

public enum AttributeType {
    TERRITORY("territory", "Território"),
    MEMORY("memory", "Memória"),
    PERCEPTION("perception", "Percepção"),
    ALERT("alert", "Alerta"),
    SOCIABILITY("sociability", "Sociabilidade"),
    INSTINCT("instinct", "Instinto"),
    CONSTITUTION("constitution", "Constituição");

    private final String key;
    private final String displayName;

    AttributeType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() { return key; }
    public String displayName() { return displayName; }
}
