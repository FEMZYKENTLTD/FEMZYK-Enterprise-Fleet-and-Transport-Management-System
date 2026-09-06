# Installation Guide

## Requirements

* **Java 17** or newer (JDK for building, JRE is enough for running). Check with `java -version`.
* **Apache Maven 3.8+** (build only).
* Any OS with a desktop (Windows, macOS, Linux). Disk: ~20 MB for the jar, database grows with data.

No database server, no installer, no internet connection needed at runtime.

## Option A — run the packaged jar

```bash
java -jar release/FEMZYK-Fleet-Management.jar
```

Optional switches:

| Switch | Effect |
|---|---|
| `--data DIR` | Keep the database, backups, exports and logs in `DIR` instead of `~/.femzyk-fleet` |
| `--demo` | Load the demonstration data set on first start (idempotent) |
| `--headless-check` | Open the database, run `PRAGMA integrity_check`, print counts and exit (no window; useful for CI/servers) |

First sign-in: **admin / Admin@2024** — you will be asked to set a new password (8+ characters, letters and digits).

## Option B — build from source

```bash
git clone https://github.com/FEMZYKENTLTD/Femzyk-Vehicle-Management-System.git
cd Femzyk-Vehicle-Management-System
mvn clean package          # runs the 49 unit/integration tests, then builds target/FEMZYK-Fleet-Management.jar
java -jar target/FEMZYK-Fleet-Management.jar
```

`mvn clean test` runs the tests only. The Swing render test runs headless, so the build works on a server without a display.

## Data locations

```
~/.femzyk-fleet/
├── fleet.db          SQLite database (WAL files fleet.db-wal / -shm may appear while running)
├── backups/          fleet-backup-YYYYMMDD-HHMMSS.db (verified copies)
├── exports/          default folder for CSV / HTML / PDF exports
└── logs/             fleet-0.log … rotating application log
```

On Windows `~` is `C:\Users\<name>`.

## Upgrading from Femzyk Vehicle Management System v4.x

Keep your old `~/FemzykVehicleSystem` folder. On first sign-in as an administrator the app detects it and offers **Data & System → Scan & migrate legacy data**. Your old accounts keep their passwords (they must be changed at first sign-in) and old vehicles arrive with placeholder plates `MIG-0001…` that you should update. Old files are never modified.

## Uninstall

Delete the jar and, if you no longer want the data, the `~/.femzyk-fleet` folder.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `UnsupportedClassVersionError` | You are running Java < 17. Install a Java 17+ runtime. |
| "database is locked" | Another instance is running against the same folder. Close it, or use `--data` with a different folder. |
| Blank/garbled fonts on Linux | Install a font package such as `fonts-dejavu`; the UI falls back automatically. |
| Forgot admin password | Sign in with another administrator and use Users → Reset password. If there is none, restore a backup made before the change. |
