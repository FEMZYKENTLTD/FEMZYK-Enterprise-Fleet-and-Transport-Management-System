package com.femzyk.fleetmanagement.ui.modules;

import com.femzyk.fleetmanagement.model.Employee;
import com.femzyk.fleetmanagement.model.EmploymentStatus;
import com.femzyk.fleetmanagement.model.Permission;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.EmployeeService;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.ModulePanel;
import com.femzyk.fleetmanagement.ui.UiUtils;
import com.femzyk.fleetmanagement.ui.components.*;
import com.femzyk.fleetmanagement.util.DateUtil;
import com.femzyk.fleetmanagement.util.MoneyUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeesPanel implements ModulePanel {

    private final ServiceRegistry s;
    private final CrudTablePanel<Employee> table;
    private final JComboBox<String> department;
    private final JComboBox<Object> status;

    public EmployeesPanel(ServiceRegistry services) {
        this.s = services;
        List<EntityTableModel.Column<Employee>> cols = List.of(
                EntityTableModel.Column.of("Code", Employee::getEmployeeCode, 80),
                EntityTableModel.Column.of("Name", Employee::getFullName, 160),
                EntityTableModel.Column.of("Department", Employee::getDepartment, 110),
                EntityTableModel.Column.of("Position", Employee::getPosition, 140),
                EntityTableModel.Column.of("Phone", Employee::getPhone, 100),
                EntityTableModel.Column.of("Email", Employee::getEmail, 170),
                EntityTableModel.Column.of("Hired", e -> DateUtil.display(e.getHireDate()), 90),
                EntityTableModel.Column.of("Salary", e -> e.getSalary() == null ? "" : MoneyUtil.format(e.getSalary()), 100),
                EntityTableModel.Column.of("Status", Employee::getStatus, 90),
                EntityTableModel.Column.of("Driver", e -> e.isDriver() ? "Yes" : "", 50));
        table = new CrudTablePanel<>("Employees", "Staff records, departments and employment status", cols);
        table.setRenderer(8, new StatusCellRenderer());
        department = table.addFilter("Department:", new JComboBox<>());
        status = table.addFilter("Status:", new JComboBox<>());
        status.addItem("All");
        for (EmploymentStatus st : EmploymentStatus.values()) status.addItem(st);
        table.loader(() -> s.employees.search(table.searchText(),
                department.getSelectedIndex() <= 0 ? null : (String) department.getSelectedItem(),
                status.getSelectedItem() instanceof EmploymentStatus st ? st : null));
        boolean canEdit = SessionContext.has(Permission.MANAGE_EMPLOYEES);
        if (canEdit) {
            table.addAction(UiUtils.primary("Add employee", () -> edit(null)));
            table.addAction(UiUtils.neutral("Edit", () -> { Employee e = table.requireSelected("employee"); if (e != null) edit(e); }));
            table.addAction(UiUtils.danger("Delete", this::delete));
            table.addAction(UiUtils.neutral("Recycle bin", this::recycleBin));
            table.onDoubleClick(this::edit);
        }
        table.addAction(UiUtils.neutral("Statistics", this::statistics));
    }

    @Override public String id() { return "employees"; }
    @Override public String title() { return "Employees"; }
    @Override public JComponent component() { return table; }

    @Override
    public void refresh() {
        Object sel = department.getSelectedItem();
        department.removeAllItems();
        department.addItem("All departments");
        for (String d : s.employees.departments()) department.addItem(d);
        if (sel != null) department.setSelectedItem(sel);
        table.refresh();
    }

    private void edit(Employee existing) {
        Employee e = existing == null ? new Employee() : s.employees.getById(existing.getId());
        FormBuilder f = new FormBuilder();
        f.section("Personal details");
        JTextField first = f.text("First name *", e.getFirstName());
        JTextField last = f.text("Last name *", e.getLastName());
        JTextField dob = f.date("Date of birth", e.getDateOfBirth());
        JTextField phone = f.text("Phone *", e.getPhone());
        JTextField email = f.text("Email *", e.getEmail());
        JTextArea address = f.area("Address", e.getAddress());
        f.section("Employment");
        JComboBox<String> dept = new JComboBox<>(s.employees.departments().toArray(new String[0]));
        dept.setEditable(true);
        if (e.getDepartment() != null) dept.setSelectedItem(e.getDepartment());
        f.add("Department *", dept);
        JTextField position = f.text("Position *", e.getPosition());
        JTextField hired = f.date("Hire date *", e.getHireDate() == null && existing == null ? java.time.LocalDate.now() : e.getHireDate());
        JTextField salary = f.number("Monthly salary (\u20A6)", e.getSalary());
        JComboBox<EmploymentStatus> st = f.combo("Status", EmploymentStatus.values(), e.getStatus());
        f.section("Emergency contact");
        JTextField ecName = f.text("Contact name", e.getEmergencyContactName());
        JTextField ecPhone = f.text("Contact phone", e.getEmergencyContactPhone());
        JTextArea notes = f.area("Notes", e.getNotes());
        boolean ok = new FormDialog(UiUtils.windowOf(table), existing == null ? "Add employee" : "Edit " + e.getEmployeeCode(), f, "Save", () -> {
            e.setFirstName(FormBuilder.str(first)); e.setLastName(FormBuilder.str(last)); e.setDateOfBirth(f.date(dob, "Date of birth"));
            e.setPhone(FormBuilder.str(phone)); e.setEmail(FormBuilder.str(email)); e.setAddress(FormBuilder.str(address));
            Object d = dept.getEditor().getItem(); e.setDepartment(d == null || d.toString().isBlank() ? null : d.toString().trim());
            e.setPosition(FormBuilder.str(position)); e.setHireDate(f.date(hired, "Hire date")); e.setSalary(f.dbl(salary, "Salary"));
            e.setStatus((EmploymentStatus) st.getSelectedItem()); e.setEmergencyContactName(FormBuilder.str(ecName)); e.setEmergencyContactPhone(FormBuilder.str(ecPhone));
            e.setNotes(FormBuilder.str(notes));
            f.throwIfParseErrors();
            if (existing == null) s.employees.create(e); else s.employees.update(e);
        }).showDialog();
        if (ok) refresh();
    }

    private void delete() {
        Employee e = table.requireSelected("employee");
        if (e == null) return;
        if (UiUtils.confirm(table, "Delete employee", "Move " + e.getFullName() + " to the recycle bin?" + (e.isDriver() ? "\nTheir driver profile will be deactivated." : ""))
                && UiUtils.run(table, () -> s.employees.delete(e.getId()))) refresh();
    }

    private void recycleBin() {
        List<EntityTableModel.Column<Employee>> cols = List.of(
                EntityTableModel.Column.of("Code", Employee::getEmployeeCode), EntityTableModel.Column.of("Name", Employee::getFullName),
                EntityTableModel.Column.of("Department", Employee::getDepartment), EntityTableModel.Column.of("Deleted", e -> DateUtil.display(e.getDeletedAt())));
        new RecycleBinDialog<>(UiUtils.windowOf(table), "Employee recycle bin", cols, s.employees::recycleBin,
                e -> s.employees.restore(e.getId()), e -> s.employees.deletePermanently(e.getId())).setVisible(true);
        refresh();
    }

    private void statistics() {
        EmployeeService.EmployeeStatistics st = UiUtils.call(table, s.employees::statistics);
        if (st == null) return;
        List<String> lines = new ArrayList<>();
        lines.add("Employees: " + st.total() + "  (drivers: " + st.drivers() + ")");
        lines.add("Monthly payroll: " + MoneyUtil.format(st.totalMonthlyPayroll()));
        lines.add("Average salary: " + (st.salary().getCount() == 0 ? "n/a" : MoneyUtil.format(st.salary().getAverage())));
        lines.add("Salary range: " + (st.salary().getCount() == 0 ? "n/a" : MoneyUtil.format(st.salary().getMin()) + " – " + MoneyUtil.format(st.salary().getMax())));
        if (st.topEarner() != null) lines.add("Highest paid: " + st.topEarner().getFullName() + " (" + MoneyUtil.format(st.topEarner().getSalary()) + ")");
        lines.add("");
        lines.add("By department:");
        st.byDepartment().forEach((k, v) -> lines.add("   " + k + ": " + v + "  (avg " + MoneyUtil.format(st.averageSalaryByDepartment().getOrDefault(k, 0.0)) + ")"));
        lines.add("");
        lines.add("By status:");
        st.byStatus().forEach((k, v) -> lines.add("   " + k + ": " + v));
        UiUtils.info(table, "Employee statistics", String.join("\n", lines));
    }
}
