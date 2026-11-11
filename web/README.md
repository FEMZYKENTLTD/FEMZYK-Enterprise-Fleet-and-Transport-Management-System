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

### `supabase/seed_demo.sql`

The same data set as a plain SQL script, for when you want the database populated
without the app running (new project, CI, a reviewer's own Supabase instance, or a
reset after experimenting). Run it after `0001_schema.sql`:

```
psql "$DATABASE_URL" -f supabase/seed_demo.sql
# or paste it into the Supabase SQL editor
```

It loads 10 employees, 6 drivers, 10 vehicles, 5 assignments, 12 trips, 7 maintenance
records, 10 fuel records and 22 expenses (16 mirrored from fuel/maintenance exactly as
the service layer writes them, 6 manual), plus the four role accounts and their
PBKDF2 password hashes.

Safety properties, so it can be re-run without fear:

* **All-or-nothing.** The whole file runs in one transaction and ends with a `do` block
  that raises an exception — rolling everything back — unless the reload produces the
  exact expected row counts. A bad edit leaves the database untouched.
* **Idempotent.** It truncates and reloads the operational tables, so running it twice
  is the same as running it once.
* **Accounts are preserved, not overwritten.** Users are only inserted when the username
  is missing, so a password changed in the UI survives a reseed.
* **Explicit ids are paired with `setval`**, so the identity sequences continue after the
  seeded rows instead of colliding with them on the next insert.

## Demo accounts

| Username | Password | Role |
| --- | --- | --- |
| `admin` | `Admin@2024` | ADMINISTRATOR — everything, including users and audit |
| `manager` | `Fleet@2026` | FLEET_MANAGER — fleet, people, operations, reports |
| `ops` | `Ops@2026` | OPERATIONS_OFFICER — trips, maintenance, fuel, expenses, reports |
| `viewer` | `View@2026` | VIEWER — read-only dashboards and reports |

## Accounts and password reset

There is **no public sign-up**. This system holds company fleet, driver, payroll and audit data, so accounts
are issued rather than self-registered:

1. The first administrator is created by `POST /api/bootstrap` (see *First-run / seeding* above).
2. That administrator creates everyone else under **Users & roles** → *Create user*, choosing the role. New
   accounts get a temporary password and `must_change_password`, which forces the new user to pick their own
   password at first sign-in.
3. Anyone who forgets their password can use **Forgot your password?** on the sign-in screen
   (`/login/reset`). The username and the e-mail already on the account must both match; the user then sets a
   new password and is signed out of nothing else. There is no mail server in this deployment, so the reset
   completes on the page instead of by e-mail link.
4. An administrator can also reset any password from **Users & roles** without knowing the old one.

## Verified behaviour

* `npx tsc --noEmit` — clean.
* `npm run build` — clean (27 route handlers, 14 screens).
* `npm test` — 11 browser-level interaction tests against the real sign-in and password-reset components
  (jsdom + Testing Library). Covers disabled-until-filled submit buttons, the request actually sent, the
  error banner on a rejected login, network-failure handling, the demo-account chips, and password-mismatch
  validation. Verified to be non-vacuous: removing the post-login navigation makes the suite fail.
* Business rules ported 1:1 from the Java services: vehicle status transition map, licence-category permits,
  one-active-assignment partial unique indexes, trip odometer rules (≤5,000 km, no regression), fuel total derivation,
  maintenance status ↔ vehicle status coupling, expense mirroring, last-administrator protection.

## Verification endpoints

| Endpoint | Auth | Purpose |
| --- | --- | --- |
| `GET /api/health` | public | Database connectivity, per-table row counts, seed state |
| `GET /api/health/deep/<BOOTSTRAP_SECRET>` | secret | Runs every read-side service, all 11 reports, all 4 demo logins, session round-trip, tampered-token rejection, permission assertions, CSV + HTML export |

Latest deep run on the production deployment (`10332f2`): **31/31 checks passed, 0 failed** — 21 read-side
checks (dashboard, 8 list services, all 11 reports) plus 10 authenticated-path checks (4 demo logins with role
assertion, HMAC session round-trip and tampered-token rejection; wrong-password rejection; viewer denied 4
write/admin permissions; administrator sees all 4 users; password reset refusing a mismatched identity; CSV
export; printable-HTML export).

Both probes send `Cache-Control: no-store`, so neither the CDN nor the browser can replay an earlier build's
result. When checking a deployment, note that some HTTP clients and page-fetching tools cache by full URL: if a
probe URL returns an older payload than the deployment you know is live, request a fresh URL (append any unique
query string, e.g. `?cb=2`) before investigating the code — a response that never changes across deployments is
almost always the client's cache, not the app's.

## Deployment

Vercel project `femzyk-fleet` (root directory `web`, framework Next.js) tracking this branch, with
`NEXT_PUBLIC_SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `SESSION_SECRET`, `BOOTSTRAP_SECRET`,
`DEMO_MODE=true` and `NEXT_PUBLIC_APP_VERSION` set for production, preview and development.

Production URL: <https://femzyk-fleet.vercel.app>
