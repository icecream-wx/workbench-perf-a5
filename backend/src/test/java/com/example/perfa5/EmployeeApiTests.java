package com.example.perfa5;

import com.example.perfa5.domain.Employee;
import com.example.perfa5.domain.EmployeeStatus;
import com.example.perfa5.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeApiTests {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void cleanDatabase() {
        employeeRepository.deleteAll();
    }

    @Test
    void createEmployeeSuccess() throws Exception {
        Call call = post(validPayload("EMP001"));

        assertEquals(201, call.status());
        assertEquals(0, call.code());
        assertEquals("ok", call.message());
        JsonNode data = call.data();
        assertTrue(data.path("id").asLong() > 0);
        assertEquals("EMP001", data.path("employeeNo").asText());
        assertEquals("张三", data.path("name").asText());
        assertEquals("研发部", data.path("department").asText());
        assertEquals("工程师", data.path("position").asText());
        assertEquals("13800138000", data.path("phone").asText());
        assertEquals("zhangsan@example.com", data.path("email").asText());
        assertEquals("2024-01-31", data.path("hireDate").asText());
        assertEquals("ACTIVE", data.path("status").asText());
        assertTrue(data.path("createdAt").asText().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
        assertEquals(data.path("createdAt").asText(), data.path("updatedAt").asText());
        assertEquals(1, employeeRepository.count());

        Call list = get("/api/employees?page=1&size=10");
        assertEquals(1, list.data().path("total").asInt());
        assertEquals(1, list.data().path("records").size());
        assertEquals("EMP001", list.data().path("records").get(0).path("employeeNo").asText());
    }

    @Test
    void createEmployeeTrimsValuesAndDefaultsStatus() throws Exception {
        Map<String, Object> payload = validPayload("  EMP002  ");
        payload.put("name", "  李四  ");
        payload.remove("status");
        payload.put("phone", "   ");
        payload.put("email", "");
        payload.put("department", "   ");

        Call call = post(payload);

        assertEquals(201, call.status());
        assertEquals(0, call.code());
        assertEquals("EMP002", call.data().path("employeeNo").asText());
        assertEquals("李四", call.data().path("name").asText());
        assertEquals("ACTIVE", call.data().path("status").asText());
        assertTrue(call.data().path("phone").isNull());
        assertTrue(call.data().path("email").isNull());
        assertTrue(call.data().path("department").isNull());
    }

    @Test
    void createEmployeeRejectsDuplicateEmployeeNo() throws Exception {
        assertEquals(201, post(validPayload("EMP001")).status());

        Call duplicate = post(validPayload("  emp001  "));

        assertEquals(409, duplicate.status());
        assertEquals(40901, duplicate.code());
        assertTrue(duplicate.message().contains("工号已存在"));
        assertEquals(1, employeeRepository.count());
    }

    @Test
    void createEmployeeRejectsMissingEmployeeNo() throws Exception {
        Map<String, Object> payload = validPayload("EMP001");
        payload.put("employeeNo", "   ");

        Call call = post(payload);

        assertEquals(400, call.status());
        assertEquals(400, call.code());
        assertTrue(call.message().contains("工号不能为空"));
        assertEquals("工号不能为空", call.body().path("fieldErrors").path("employeeNo").asText());
        assertEquals(0, employeeRepository.count());
    }

    @Test
    void validationFailuresAreRejectedWithoutPersisting() throws Exception {
        assertRejected(validPayload("EMP001"), "employeeNo", "E".repeat(33), "工号长度不能超过32个字符");
        assertRejected(validPayload("EMP001"), "name", "名".repeat(65), "姓名长度不能超过64个字符");
        assertRejected(validPayload("EMP001"), "phone", "abc", "手机号格式不正确");
        assertRejected(validPayload("EMP001"), "email", "abc", "邮箱格式不正确");
        assertRejected(validPayload("EMP001"), "status", "OTHER", "状态取值不合法");
        assertRejected(validPayload("EMP001"), "hireDate", "2024-13-01", "入职日期格式不正确");
        assertRejected(validPayload("EMP001"), "department", "部".repeat(65), "部门长度不能超过64个字符");
        assertRejected(validPayload("EMP001"), "position", "职".repeat(65), "职位长度不能超过64个字符");
        assertEquals(0, employeeRepository.count());
    }

    @Test
    void updateEmployeeSuccess() throws Exception {
        Call created = post(validPayload("EMP001"));
        long id = created.data().path("id").asLong();
        String createdAt = created.data().path("createdAt").asText();

        Map<String, Object> payload = validPayload("EMP001");
        payload.put("name", "张三丰");
        payload.put("department", "平台部");
        payload.put("status", "INACTIVE");
        Call updated = put(id, payload);

        assertEquals(200, updated.status());
        assertEquals(0, updated.code());
        assertEquals(id, updated.data().path("id").asLong());
        assertEquals("张三丰", updated.data().path("name").asText());
        assertEquals("平台部", updated.data().path("department").asText());
        assertEquals("INACTIVE", updated.data().path("status").asText());
        assertEquals("EMP001", updated.data().path("employeeNo").asText());

        LocalDateTime createdTime = LocalDateTime.parse(createdAt, DATE_TIME);
        LocalDateTime updatedTime = LocalDateTime.parse(updated.data().path("updatedAt").asText(), DATE_TIME);
        assertTrue(updatedTime.isAfter(createdTime));
        assertEquals(createdAt, updated.data().path("createdAt").asText());

        Call detail = get("/api/employees/" + id);
        assertEquals("张三丰", detail.data().path("name").asText());
        assertEquals("平台部", detail.data().path("department").asText());
        assertEquals(updated.data().path("updatedAt").asText(), detail.data().path("updatedAt").asText());
        assertEquals(1, employeeRepository.count());
    }

    @Test
    void updateEmployeeKeepsOwnEmployeeNo() throws Exception {
        long id = post(validPayload("EMP001")).data().path("id").asLong();

        Map<String, Object> payload = validPayload("EMP001");
        payload.put("department", "平台部");
        Call updated = put(id, payload);

        assertEquals(0, updated.code());
        assertEquals("EMP001", updated.data().path("employeeNo").asText());
        assertEquals("平台部", updated.data().path("department").asText());
    }

    @Test
    void updateEmployeeRejectsOtherEmployeeNo() throws Exception {
        long first = post(validPayload("EMP001")).data().path("id").asLong();
        long second = post(validPayload("EMP002")).data().path("id").asLong();
        assertNotEquals(first, second);

        Call conflict = put(first, validPayload("EMP002"));

        assertEquals(409, conflict.status());
        assertEquals(40901, conflict.code());
        assertTrue(conflict.message().contains("工号已存在"));
        assertEquals("EMP001", get("/api/employees/" + first).data().path("employeeNo").asText());
        assertEquals(2, employeeRepository.count());
    }

    @Test
    void updateMissingEmployeeReturnsNotFound() throws Exception {
        Call call = put(999999L, validPayload("EMP999"));

        assertEquals(404, call.status());
        assertEquals(40401, call.code());
        assertTrue(call.message().contains("员工不存在"));
        assertEquals(0, employeeRepository.count());
    }

    @Test
    void updateValidationFailureDoesNotPartiallyUpdate() throws Exception {
        Call created = post(validPayload("EMP001"));
        long id = created.data().path("id").asLong();

        Map<String, Object> payload = validPayload("EMP001");
        payload.put("name", "新名字");
        payload.put("phone", "123");
        Call invalid = put(id, payload);

        assertEquals(400, invalid.status());
        assertEquals(400, invalid.code());
        assertTrue(invalid.message().contains("手机号格式不正确"));

        Call detail = get("/api/employees/" + id);
        assertEquals("张三", detail.data().path("name").asText());
        assertEquals("13800138000", detail.data().path("phone").asText());
    }

    @Test
    void detailReturnsEmployeeOrNotFound() throws Exception {
        Call created = post(validPayload("EMP001"));
        long id = created.data().path("id").asLong();

        Call detail = get("/api/employees/" + id);
        assertEquals(0, detail.code());
        assertEquals("EMP001", detail.data().path("employeeNo").asText());

        Call missing = get("/api/employees/123456");
        assertEquals(404, missing.status());
        assertEquals(40401, missing.code());
        assertTrue(missing.message().contains("员工不存在"));
    }

    @Test
    void listPaginatesWithStableDescendingOrder() throws Exception {
        saveEmployees(3);

        Call firstPage = get("/api/employees?page=1&size=2");
        assertEquals(0, firstPage.code());
        assertEquals(2, firstPage.data().path("records").size());
        assertEquals(3, firstPage.data().path("total").asInt());
        assertEquals(1, firstPage.data().path("page").asInt());
        assertEquals(2, firstPage.data().path("size").asInt());
        assertEquals("E003", firstPage.data().path("records").get(0).path("employeeNo").asText());
        assertEquals("E002", firstPage.data().path("records").get(1).path("employeeNo").asText());

        Call secondPage = get("/api/employees?page=2&size=2");
        assertEquals(1, secondPage.data().path("records").size());
        assertEquals(3, secondPage.data().path("total").asInt());
        assertEquals("E001", secondPage.data().path("records").get(0).path("employeeNo").asText());
        assertNotEquals(
                firstPage.data().path("records").get(0).path("id").asLong(),
                secondPage.data().path("records").get(0).path("id").asLong());
    }

    @Test
    void listFiltersByKeyword() throws Exception {
        saveEmployee("E1001", "张三丰");
        saveEmployee("E2002", "李四");

        Call byNo = get("/api/employees?keyword=e1001");
        assertEquals(1, byNo.data().path("total").asInt());
        assertEquals("E1001", byNo.data().path("records").get(0).path("employeeNo").asText());

        Call byName = get("/api/employees?keyword=李");
        assertEquals(1, byName.data().path("total").asInt());
        assertEquals("E2002", byName.data().path("records").get(0).path("employeeNo").asText());

        Call blankKeyword = perform(MockMvcRequestBuilders.get("/api/employees").param("keyword", "   "));
        assertEquals(2, blankKeyword.data().path("total").asInt());

        Call noMatch = get("/api/employees?keyword=不存在");
        assertEquals(0, noMatch.data().path("total").asInt());
        assertEquals(0, noMatch.data().path("records").size());
    }

    @Test
    void listReturnsEmptyPageWhenNoData() throws Exception {
        Call list = get("/api/employees");

        assertEquals(200, list.status());
        assertEquals(0, list.data().path("total").asInt());
        assertEquals(0, list.data().path("records").size());
        assertEquals(1, list.data().path("page").asInt());
        assertEquals(10, list.data().path("size").asInt());
    }

    @Test
    void negativeOrOversizedPagingParametersAreRejected() throws Exception {
        Call pageZero = get("/api/employees?page=0");
        assertEquals(400, pageZero.status());
        assertEquals(400, pageZero.code());

        Call sizeZero = get("/api/employees?size=0");
        assertEquals(400, sizeZero.status());
        assertEquals(400, sizeZero.code());

        Call sizeTooLarge = get("/api/employees?size=101");
        assertEquals(400, sizeTooLarge.status());
        assertEquals(400, sizeTooLarge.code());
    }

    @Test
    void repositoryManagesTimestampsAndEnforcesUniqueEmployeeNo() {
        Employee employee = newEmployee("E9001", "持久化测试");
        Employee saved = employeeRepository.saveAndFlush(employee);
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
        assertEquals(EmployeeStatus.ACTIVE, saved.getStatus());

        Employee reloaded = employeeRepository.findById(saved.getId()).orElseThrow();
        reloaded.setName("持久化测试改");
        Employee updated = employeeRepository.saveAndFlush(reloaded);
        assertTrue(updated.getUpdatedAt().isAfter(saved.getUpdatedAt()));
        assertEquals(saved.getCreatedAt(), updated.getCreatedAt());

        Employee duplicate = newEmployee("E9001", "重复工号");
        assertThrows(DataIntegrityViolationException.class, () -> employeeRepository.saveAndFlush(duplicate));
    }

    private void assertRejected(Map<String, Object> payload, String field, String value, String expectedMessage)
            throws Exception {
        payload.put(field, value);
        Call call = post(payload);
        assertEquals(400, call.status(), "field=" + field);
        assertEquals(400, call.code(), "field=" + field);
        assertTrue(call.message().contains(expectedMessage),
                "expected message to contain [" + expectedMessage + "] but was [" + call.message() + "]");
        assertEquals(0, employeeRepository.count(), "field=" + field);
    }

    private void saveEmployees(int total) {
        List<Employee> employees = new ArrayList<>();
        for (int index = 1; index <= total; index++) {
            employees.add(newEmployee(String.format("E%03d", index), "员工" + index));
        }
        employeeRepository.saveAll(employees);
    }

    private void saveEmployee(String employeeNo, String name) {
        employeeRepository.save(newEmployee(employeeNo, name));
    }

    private Employee newEmployee(String employeeNo, String name) {
        Employee employee = new Employee();
        employee.setEmployeeNo(employeeNo);
        employee.setName(name);
        employee.setDepartment("研发部");
        employee.setPosition("工程师");
        employee.setPhone("13800138000");
        employee.setEmail(employeeNo.toLowerCase() + "@example.com");
        employee.setHireDate(LocalDate.of(2024, 1, 31));
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }

    private Map<String, Object> validPayload(String employeeNo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeNo", employeeNo);
        payload.put("name", "张三");
        payload.put("department", "研发部");
        payload.put("position", "工程师");
        payload.put("phone", "13800138000");
        payload.put("email", "zhangsan@example.com");
        payload.put("hireDate", "2024-01-31");
        payload.put("status", "ACTIVE");
        return payload;
    }

    private Call post(Object payload) throws Exception {
        return perform(MockMvcRequestBuilders.post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(payload)));
    }

    private Call put(long id, Object payload) throws Exception {
        return perform(MockMvcRequestBuilders.put("/api/employees/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(payload)));
    }

    private Call get(String url) throws Exception {
        return perform(MockMvcRequestBuilders.get(url));
    }

    private Call perform(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder).andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode node = body.isEmpty() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
        return new Call(result.getResponse().getStatus(), node);
    }

    private String serialize(Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        assertNotNull(json);
        assertFalse(json.isBlank());
        return json;
    }

    private record Call(int status, JsonNode body) {

        int code() {
            return body.path("code").asInt(-1);
        }

        String message() {
            return body.path("message").asText();
        }

        JsonNode data() {
            return body.path("data");
        }
    }
}
