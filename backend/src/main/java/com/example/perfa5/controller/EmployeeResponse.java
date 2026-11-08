package com.example.perfa5.controller;

import com.example.perfa5.domain.Employee;
import com.example.perfa5.domain.EmployeeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String employeeNo,
        String name,
        String department,
        String position,
        String phone,
        String email,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate hireDate,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updatedAt) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeNo(),
                employee.getName(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getPhone(),
                employee.getEmail(),
                employee.getHireDate(),
                (employee.getStatus() == null ? EmployeeStatus.ACTIVE : employee.getStatus()).name(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }
}
