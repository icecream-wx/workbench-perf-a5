package com.example.perfa5.domain;

/**
 * 员工在职状态，新增未填写时统一为 {@link #ACTIVE}。
 */
public enum EmployeeStatus {
    ACTIVE,
    INACTIVE;

    public static EmployeeStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ACTIVE;
        }
        for (EmployeeStatus status : values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
