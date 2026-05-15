package com.inkwell.auth.dto;

import lombok.Data;

@Data
public class UpdateUserBlockRequest {
    private Boolean isBlocked;
    private Boolean blocked;
    private Boolean active;
    private Boolean enabled;
    private String status;

    public Boolean resolveBlocked() {
        if (isBlocked != null) {
            return isBlocked;
        }
        if (blocked != null) {
            return blocked;
        }
        if (active != null) {
            return !active;
        }
        if (enabled != null) {
            return !enabled;
        }
        if (status != null && !status.isBlank()) {
            String normalizedStatus = status.trim().toUpperCase();
            if ("BLOCKED".equals(normalizedStatus)) {
                return true;
            }
            if ("ACTIVE".equals(normalizedStatus)) {
                return false;
            }
        }
        return null;
    }
}
