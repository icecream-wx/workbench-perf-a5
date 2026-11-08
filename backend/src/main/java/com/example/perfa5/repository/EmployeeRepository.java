package com.example.perfa5.repository;

import com.example.perfa5.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeNoIgnoreCase(String employeeNo);

    boolean existsByEmployeeNoIgnoreCaseAndIdNot(String employeeNo, Long id);

    @Query("""
            select e from Employee e
            where lower(e.employeeNo) like lower(concat('%', :keyword, '%'))
               or lower(e.name) like lower(concat('%', :keyword, '%'))
            """)
    Page<Employee> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
