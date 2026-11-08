package com.example.perfa5.controller;

import java.util.List;

public record EmployeePageResponse(List<EmployeeResponse> records, long total, int page, int size) {
}
