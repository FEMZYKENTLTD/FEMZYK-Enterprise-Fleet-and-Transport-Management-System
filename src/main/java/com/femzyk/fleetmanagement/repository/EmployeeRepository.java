package com.femzyk.fleetmanagement.repository;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.database.JdbcSupport;
import com.femzyk.fleetmanagement.model.Employee;
import com.femzyk.fleetmanagement.model.EmploymentStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashMap;

public class EmployeeRepository extends JdbcSupport {

    private static final String BASE = "SELECT e.*, (SELECT COUNT(*) FROM drivers d WHERE d.employee_id = e.id AND d.deleted_at IS NULL) AS is_driver "
            + "FROM employees e ";

    public EmployeeRepository(DatabaseManager db) { super(db); }

    static Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setId(rs.getLong("id"));
        e.setEmployeeCode(rs.getString("employee_code"));
        e.setFirstName(rs.getString("first_name"));
        e.setLastName(rs.getString("last_name"));
        e.setPhone(rs.getString("phone"));
        e.setEmail(rs.getString("email"));
        e.setAddress(rs.getString("address"));
        e.setDepartment(rs.getString("department"));
        e.setPosition(rs.getString("position"));
        e.setDateOfBirth(date(rs, "date_of_birth"));
        e.setHireDate(date(rs, "hire_date"));
        e.setSalary(doubleOrNull(rs, "salary"));
        e.setStatus(EmploymentStatus.valueOf(rs.getString("status")));
        e.setEmergencyContactName(rs.getString("emergency_contact_name"));
        e.setEmergencyContactPhone(rs.getString("emergency_contact_phone"));
        e.setNotes(rs.getString("notes"));
        e.setCreatedAt(dateTime(rs, "created_at"));
        e.setUpdatedAt(dateTime(rs, "updated_at"));
        e.setDeletedAt(dateTime(rs, "deleted_at"));
        try { e.setDriver(rs.getInt("is_driver") > 0); } catch (SQLException ignored) { /* column absent */ }
        return e;
    }

    public List<Employee> findAll(boolean includeDeleted) {
        String sql = BASE + (includeDeleted ? "" : "WHERE e.deleted_at IS NULL ") + "ORDER BY e.last_name, e.first_name";
        return query(sql, EmployeeRepository::map);
    }

    public List<Employee> findDeleted() {
        return query(BASE + "WHERE e.deleted_at IS NOT NULL ORDER BY e.deleted_at DESC", EmployeeRepository::map);
    }

    public Optional<Employee> findById(long id) {
        return queryOne(BASE + "WHERE e.id = ?", EmployeeRepository::map, id);
    }

    public Optional<Employee> findByCode(String code) {
        return queryOne(BASE + "WHERE e.employee_code = ?", EmployeeRepository::map, code);
    }

    public Optional<Employee> findByEmail(String email) {
        return queryOne(BASE + "WHERE e.email = ? COLLATE NOCASE AND e.deleted_at IS NULL", EmployeeRepository::map, email);
    }

    /** Free-text search across code, name, phone, email, department, position plus optional filters. */
    public List<Employee> search(String text, String department, EmploymentStatus status) {
        StringBuilder sql = new StringBuilder(BASE + "WHERE e.deleted_at IS NULL ");
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase() + "%";
            sql.append("AND (lower(e.employee_code) LIKE ? OR lower(e.first_name || ' ' || e.last_name) LIKE ? "
                    + "OR lower(coalesce(e.phone,'')) LIKE ? OR lower(coalesce(e.email,'')) LIKE ? "
                    + "OR lower(e.department) LIKE ? OR lower(coalesce(e.position,'')) LIKE ?) ");
            for (int i = 0; i < 6; i++) params.add(like);
        }
        if (department != null && !department.isBlank()) {
            sql.append("AND e.department = ? ");
            params.add(department);
        }
        if (status != null) {
            sql.append("AND e.status = ? ");
            params.add(status);
        }
        sql.append("ORDER BY e.last_name, e.first_name");
        return query(sql.toString(), EmployeeRepository::map, params.toArray());
    }

    public List<String> distinctDepartments() {
        return query("SELECT DISTINCT department FROM employees WHERE deleted_at IS NULL ORDER BY department", rs -> rs.getString(1));
    }

    public long count(EmploymentStatus status) {
        if (status == null) return queryLong("SELECT COUNT(*) FROM employees WHERE deleted_at IS NULL");
        return queryLong("SELECT COUNT(*) FROM employees WHERE deleted_at IS NULL AND status = ?", status);
    }

    public Map<String, Long> countByDepartment() {
        Map<String, Long> out = new LinkedHashMap<>();
        query("SELECT department, COUNT(*) FROM employees WHERE deleted_at IS NULL GROUP BY department ORDER BY department",
                rs -> out.put(rs.getString(1), rs.getLong(2)));
        return out;
    }

    public Employee save(Employee e) {
        String now = now();
        if (e.isNew()) {
            long id = insert("INSERT INTO employees(employee_code, first_name, last_name, phone, email, address, department, position, "
                    + "date_of_birth, hire_date, salary, status, emergency_contact_name, emergency_contact_phone, notes, created_at, updated_at) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    e.getEmployeeCode(), e.getFirstName(), e.getLastName(), e.getPhone(), blankToNull(e.getEmail()), e.getAddress(),
                    e.getDepartment(), e.getPosition(), e.getDateOfBirth(), e.getHireDate(), e.getSalary(), e.getStatus(),
                    e.getEmergencyContactName(), e.getEmergencyContactPhone(), e.getNotes(), now, now);
            e.setId(id);
        } else {
            update("UPDATE employees SET employee_code=?, first_name=?, last_name=?, phone=?, email=?, address=?, department=?, position=?, "
                    + "date_of_birth=?, hire_date=?, salary=?, status=?, emergency_contact_name=?, emergency_contact_phone=?, notes=?, updated_at=? "
                    + "WHERE id=?",
                    e.getEmployeeCode(), e.getFirstName(), e.getLastName(), e.getPhone(), blankToNull(e.getEmail()), e.getAddress(),
                    e.getDepartment(), e.getPosition(), e.getDateOfBirth(), e.getHireDate(), e.getSalary(), e.getStatus(),
                    e.getEmergencyContactName(), e.getEmergencyContactPhone(), e.getNotes(), now, e.getId());
        }
        return e;
    }

    public void softDelete(long id) {
        update("UPDATE employees SET deleted_at = ?, updated_at = ? WHERE id = ?", now(), now(), id);
    }

    public void restore(long id) {
        update("UPDATE employees SET deleted_at = NULL, updated_at = ? WHERE id = ?", now(), id);
    }

    public void hardDelete(long id) {
        update("DELETE FROM employees WHERE id = ?", id);
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
