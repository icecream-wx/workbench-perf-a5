package com.example.perfa5.service;

import com.example.perfa5.controller.EmployeeRequest;
import com.example.perfa5.domain.Employee;
import com.example.perfa5.domain.EmployeeStatus;
import com.example.perfa5.exception.BizException;
import com.example.perfa5.repository.EmployeeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class EmployeeService {

    public static final int MAX_PAGE_SIZE = 100;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public Page<Employee> list(String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        String condition = keyword == null ? "" : keyword.trim();
        if (condition.isEmpty()) {
            return employeeRepository.findAll(pageable);
        }
        return employeeRepository.searchByKeyword(condition, pageable);
    }

    @Transactional(readOnly = true)
    public Employee get(long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new BizException(40401, "员工不存在"));
    }

    @Transactional
    public Employee create(EmployeeRequest request) {
        if (employeeRepository.existsByEmployeeNoIgnoreCase(request.getEmployeeNo())) {
            throw new BizException(40901, "工号已存在");
        }
        Employee employee = new Employee();
        applyEditableFields(employee, request);
        return saveWithConflictGuard(employee);
    }

    @Transactional
    public Employee update(long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BizException(40401, "员工不存在"));
        if (employeeRepository.existsByEmployeeNoIgnoreCaseAndIdNot(request.getEmployeeNo(), id)) {
            throw new BizException(40901, "工号已存在");
        }
        applyEditableFields(employee, request);
        return saveWithConflictGuard(employee);
    }

    private Employee saveWithConflictGuard(Employee employee) {
        try {
            return employeeRepository.saveAndFlush(employee);
        } catch (DataIntegrityViolationException exception) {
            throw new BizException(40901, "工号已存在");
        }
    }

    private void applyEditableFields(Employee employee, EmployeeRequest request) {
        employee.setEmployeeNo(request.getEmployeeNo());
        employee.setName(request.getName());
        employee.setDepartment(request.getDepartment());
        employee.setPosition(request.getPosition());
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        employee.setHireDate(parseHireDate(request.getHireDate()));
        EmployeeStatus status = EmployeeStatus.fromCode(request.getStatus());
        if (status == null) {
            throw new BizException(400, "状态取值不合法");
        }
        employee.setStatus(status);
    }

    private LocalDate parseHireDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BizException(400, "入职日期格式不正确");
        }
    }
}
