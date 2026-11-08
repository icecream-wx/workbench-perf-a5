package com.example.perfa5.controller;

import com.example.perfa5.domain.Employee;
import com.example.perfa5.exception.ApiResponse;
import com.example.perfa5.exception.BizException;
import com.example.perfa5.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ApiResponse<EmployeePageResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        if (page < 1) {
            throw new BizException(400, "页码不能小于1");
        }
        if (size < 1) {
            throw new BizException(400, "每页条数不能小于1");
        }
        if (size > EmployeeService.MAX_PAGE_SIZE) {
            throw new BizException(400, "每页条数不能超过" + EmployeeService.MAX_PAGE_SIZE);
        }
        Page<Employee> result = employeeService.list(keyword, page, size);
        List<EmployeeResponse> records = result.getContent().stream()
                .map(EmployeeResponse::from)
                .toList();
        return ApiResponse.ok(new EmployeePageResponse(
                records, result.getTotalElements(), result.getNumber() + 1, result.getSize()));
    }

    @GetMapping("/{id}")
    public ApiResponse<EmployeeResponse> detail(@PathVariable long id) {
        return ApiResponse.ok(EmployeeResponse.from(employeeService.get(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.ok(EmployeeResponse.from(employeeService.create(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<EmployeeResponse> update(@PathVariable long id, @Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.ok(EmployeeResponse.from(employeeService.update(id, request)));
    }
}
