package com.almostreliable.almostgradle.dependency;

public enum LoadingMode {
    NONE,
    API,
    FULL;

    public static LoadingMode fromString(String string) {
        return switch (string.toLowerCase()) {
            case "api" -> API;
            case "full" -> FULL;
            case "none" -> NONE;
            default -> throw new IllegalStateException("Unexpected value: " + string);
        };
    }
}
