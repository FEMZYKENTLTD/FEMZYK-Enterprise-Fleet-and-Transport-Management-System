# FEMZYK Fleet — web edition

Browser edition of the **FEMZYK Enterprise Fleet & Transport Management System** (CSC392 project).
The Java 17 + Swing desktop application in the repository root remains the primary reference implementation;
this directory is a full, functionally equivalent web tier built with Next.js, PostgreSQL (Supabase) and Tailwind CSS.

Everything the desktop edition does is available here, with the same business rules, permissions and audit trail.

## Stack

| Layer | Desktop edition | Web edition (this directory) |
| --- | --- | --- |
| UI | Java Swing (`ui/modules/*Panel`) | Next.js 16 App Router + React 19 + Tailwind 4 |
| Service layer | `com.femzyk.fleetmanagement.service.*` | `src/lib/services/*` (server-only) |
| Persistence | JDBC + SQLite (`FleetDatabase`) | `@supabase/supabase-js` + PostgreSQL 15 |
| Schema | `src/main/resources/schema.sql` | `supabase/migrations/0001_schema.sql` |
| Auth | PBKDF2-HMAC-SHA256, 65,536 iterations | Same algorithm, same hashes — users can be shared |
| Session | `SessionContext` (in-process) | HMAC-SHA256 signed, http-only cookie (8 h) |
| Reports | `ReportService` (10 kinds) | `src/lib/services/reports.ts` (11 kinds, CSV + printable HTML) |

## Security model

* The browser **never** talks to the database. Every read/write goes through a Next.js route handler running on the server.
* The Supabase **service-role key lives only in server environment variables**. Row-level security is enabled on all twelve
  tables **with no policies**, so the publishable/anon key can read nothing even if one is ever exposed.
* Sessions are stateless HMAC-signed cookies (`SESSION_COOKIE`), verified on every server render and API request.
* Passwords are PBKDF2-HMAC-SHA256, 65,536 iterations, 256-bit key, 16-byte salt, Base64-encoded — byte-compatible with
  `PasswordUtil` in the Java edition.
* Every mutating service function calls `log_audit()`, so `audit_logs` matches the desktop audit trail.
* Permission checks run in the **service layer** (`requirePermission`), not only in the UI.

## Modules

| Route | Desktop equivalent | Notes |
| --- | --- | --- |
| `/dashboard` | `DashboardPanel` | 8 KPI cards, cost trend, status donut, service/licence alerts, top-cost vehicles, recent activity |
| `/vehicles` | `VehiclePanel` | Full CRUD, type-specific attributes (doors / wheels+style / tonnage), guarded status transitions, soft delete |
| `/employees` | `EmployeePanel` | CRUD, department filter, payroll totals, recycle bin with restore |
| `/drivers` | `DriverPanel` | CRUD, licence class + expiry badges, employee linkage, expiry alerts |
| `/assignments` | `AssignmentPanel` | Assign / return / cancel, licence-class validation, one active assignment per vehicle and driver |
| `/trips` | `TripPanel` | Plan → start → complete, automatic distance from odometer, mileage roll-up, cancel |
| `/maintenance` | `MaintenancePanel` | Book / edit / delete, service-due reminders (date + odometer), automatic expense mirror |
| `/fuel` | `FuelPanel` | Litres × unit price = total, odometer monotonicity, automatic expense mirror |
| `/expenses` | `ExpensePanel` | Ten categories, fleet-wide or per vehicle, auto-generated rows are read-only |
| `/reports` | `ReportPanel` | 11 live reports, CSV download, printable HTML for PDF export |
| `/audit` | System ▸ audit | Filterable audit trail with limit selector |
| `/system` | `SystemPanel` | Dataset CSV export, CSV import, row counts, security summary |
| `/users` | `UserManagementPanel` | Create / edit / deactivate users, password reset, role matrix (last admin protected) |

## Local development

```bash
cd web
cp .env.example .env.local     # fill in the Supabase URL + service-role key + secrets
npm install
npm run dev                    # http://localhost:3000
```

Environment variables:

| Variable | Purpose |
| --- | --- |
| `NEXT_PUBLIC_SUPABASE_URL` | Supabase project URL |
| `SUPABASE_SERVICE_ROLE_KEY` | Service-role key — **server only** |
| `SESSION_SECRET` | ≥16 random characters; signs the session cookie |
| `BOOTSTRAP_SECRET` | One-time secret for `POST /api/bootstrap` and `POST /api/demo` |
| `DEMO_MODE` | `true` shows the demo-account chips on the sign-in page |
| `NEXT_PUBLIC_APP_VERSION` | Version label |

## First-run / seeding

1. Apply `supabase/migrations/0001_schema.sql` to a fresh Supabase project.
2. `curl -X POST https://<host>/api/bootstrap -H "x-bootstrap-secret: $BOOTSTRAP_SECRET"`
   → creates the default administrator (`admin` / `Admin@2024`).
3. `curl -X POST https://<host>/api/demo -H "x-bootstrap-secret: $BOOTSTRAP_SECRET"`
   → loads the demonstration data set through the normal service functions (idempotent).

## Demo accounts

| Username | Password | Role |
| --- | --- | --- |
| `admin` | `Admin@2024` | ADMINISTRATOR — everything, including users and audit |
| `manager` | `Fleet@2026` | FLEET_MANAGER — fleet, people, operations, reports |
| `ops` | `Ops@2026` | OPERATIONS_OFFICER — trips, maintenance, fuel, expenses, reports |
| `viewer` | `View@2026` | VIEWER — read-only dashboards and reports |

## Verified behaviour

* `npx tsc --noEmit` — clean.
* `npm run build` — clean (26 route handlers, 13 screens).
* Business rules ported 1:1 from the Java services: vehicle status transition map, licence-category permits,
  one-active-assignment partial unique indexes, trip odometer rules (≤5,000 km, no regression), fuel total derivation,
  maintenance status ↔ vehicle status coupling, expense mirroring, last-administrator protection.
