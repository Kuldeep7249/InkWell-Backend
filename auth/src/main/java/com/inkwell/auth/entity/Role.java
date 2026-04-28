package com.inkwell.auth.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Role {
    READER,
    AUTHOR,
    ADMIN;

    @JsonCreator
    public static Role fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        String roleName = normalized;
        return Arrays.stream(values())
                .filter(role -> role.name().equals(roleName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + value));
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
