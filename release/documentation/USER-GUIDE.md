# User Guide

Screens are shown in `screenshots/v2/` (rendered from the demo data set).

## 1. Signing in

Start the application, enter your username and password. After 5 wrong attempts an account is locked for 15 minutes. **Forgot password…** lets you reset it if you know the username and the e-mail registered on the account. New accounts (and the built-in `admin`) must change their password at first sign-in.

## 2. Getting around

The dark sidebar groups the modules: **Overview** (Dashboard), **People** (Employees, Drivers, Users), **Fleet** (Vehicles, Assignments, Trips), **Operations** (Maintenance, Fuel, Expenses), **Insight** (Reports, Audit Log, Data & System). Only modules your role may see are listed. `F5` refreshes the current screen. Every list screen has a search box (filters as you type), drop-down filters, sortable columns (click a header) and a record count. Double-click a row to edit it.

## 3. Recommended set-up order

1. **Employees** → add staff (name, phone, e-mail, department, position, hire date).
2. **Drivers** → *Add driver*, pick an employee, enter licence number, class (A motorcycle · B car · C light truck/van · D heavy truck/bus · E articulated) and expiry.
3. **Vehicles** → *Add vehicle*, choose the type, then fill the form. Registration numbers are unique.
4. **Assignments** → *Assign vehicle*: only AVAILABLE vehicles and free, active drivers whose licence class permits the vehicle type are offered.
5. **Trips** → *Plan trip*, then *Start* (vehicle becomes IN SERVICE) and *Complete* with the end odometer (vehicle mileage is updated).
6. **Maintenance / Fuel** as work happens — the matching expense is posted automatically.

## 4. Module notes

**Employees** — statuses ACTIVE, ON_LEAVE, SUSPENDED, TERMINATED. Setting a non-working status deactivates the person's driver profile. *Statistics* shows payroll and department breakdowns. Deleted employees go to the *Recycle bin* where they can be restored; permanent deletion is refused while trip/assignment history exists.

**Drivers** — the *Licence* column shows VALID / EXPIRING SOON (≤30 days) / EXPIRED. Expired-licence drivers cannot be assigned or start trips. *Assignment history* lists every vehicle the driver has held.

**Vehicles** — status colours: AVAILABLE green, ASSIGNED/IN SERVICE blue, MAINTENANCE orange, OUT OF SERVICE/RETIRED red. *Change status* offers only legal transitions (ASSIGNED and IN SERVICE are set by Assignments and Trips, never by hand; RETIRED is final). Mileage can never be lowered. *History* opens assignments, trips, maintenance, fuel and efficiency for the vehicle.

**Assignments** — *Return vehicle* ends an assignment (vehicle → AVAILABLE); *Reassign* returns and assigns in one step; *Cancel* records a cancelled assignment. A vehicle or driver can only have one active assignment.

**Trips** — PLANNED → ACTIVE → COMPLETED or CANCELLED. End odometer must be ≥ start and within 5,000 km; completed trips cannot be edited or deleted.

**Maintenance** — types ROUTINE_SERVICE, REPAIR, INSPECTION, TYRE, BODYWORK, ELECTRICAL, OTHER; statuses SCHEDULED, IN_PROGRESS (vehicle → MAINTENANCE), COMPLETED, CANCELLED. Fill *Next service date/mileage* to get DUE SOON / OVERDUE alerts on the dashboard.

**Fuel** — quantity × unit price is computed. Enter the odometer to enable *Efficiency* (km/L is computed only from consecutive readings — never estimated).

**Expenses** — manual entries in categories FUEL, MAINTENANCE, REPAIRS, INSURANCE, REGISTRATION, TOLLS, PARKING, FINES, CLEANING, MISCELLANEOUS. Rows marked *Fuel* or *Maintenance* in the Source column are system-generated and must be edited from their source record. *Totals* shows month / year-to-date breakdowns.

**Dashboard** — eight KPI tiles, six charts (vehicles by status/type, expenses and fuel per month, expenses by category, headcount by department) and an *Attention required* list of overdue maintenance and licence problems. Everything is read from the database when the screen opens; press *Refresh* after changes.

**Reports** — choose a report (Fleet, Employee, Driver, Assignment, Trip, Maintenance, Fuel, Expense, Monthly Summary, Audit Trail) and an optional period, *Generate* to preview, then export **CSV** (spreadsheets), **HTML** (print from a browser) or **PDF**.

**Audit Log** (administrators) — filter by module/action or search the description.

**Users** (administrators) — create accounts with a role, edit role/active flag, reset passwords. The last administrator cannot be deleted or demoted.

**Data & System** — *Create backup now* writes a verified copy to the backups folder; *Restore* replaces the live database (a safety copy is kept and you are signed out). *CSV export/import* with downloadable templates for employees, vehicles and drivers — each row is validated and rejected rows are listed with their line number. *Load demo data* inserts the sample data set once (records carry `[DEMO DATA]` in their notes). *Scan & migrate legacy data* imports v4.x `.dat` files.

## 5. Keyboard & tips

* `Enter` submits a dialog, `Esc` cancels.
* Dates accept `2026-09-06` or `06/09/2026`; date-times `2026-09-06 08:30`.
* Amounts are Naira (₦); type plain numbers.
