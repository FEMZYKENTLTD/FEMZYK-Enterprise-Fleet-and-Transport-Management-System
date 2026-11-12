# FEMZYK Fleet Management — desktop edition (v2.0.0)

This folder is a complete, runnable copy of the **desktop (offline) edition** of
the FEMZYK Enterprise Fleet and Transport Management System.

The desktop edition is at version **2.0.0**; the web edition is at **3.0.0**.
They are two front ends over the same rules and data model, but they are
versioned and released separately — this package contains only the desktop one.

It runs on your own machine with no server, no internet connection and no
database to install: everything lives in a single SQLite file that is created
the first time you start the application.

---

## 1. Before you start: install Java

The application needs **Java 17 or newer**. That is the only prerequisite.

**Windows**

1. Go to <https://adoptium.net>.
2. Download the **Windows x64 `.msi`** installer (pick JDK 17 or 21).
3. Run it and accept the defaults. Let it set `JAVA_HOME`.

**macOS**

```bash
brew install openjdk@21
```

Or download the `.pkg` for your chip (Intel / Apple Silicon) from
<https://adoptium.net>.

**Linux (Debian / Ubuntu)**

```bash
sudo apt update && sudo apt install openjdk-21-jre
```

Check it worked — this should print a version like `openjdk 21.0.x`:

```bash
java -version
```

---

## 2. Start the application

**Windows** — double-click **`windows\FEMZYK-Fleet.bat`**.

If Windows shows a SmartScreen warning because the file was downloaded from the
internet, choose **More info → Run anyway**. (The `.jar` is not code-signed;
signing needs a paid certificate.)

**macOS / Linux**

```bash
cd femzyk-fleet-desktop-2.0.0
./run-linux-macos.sh
```

**Any system, manually** — this always works and is worth knowing:

```bash
java -jar FEMZYK-Fleet-Management.jar
```

### What you should see

A sign-in window. Use the default administrator account created on first run:

| Username | Password |
|----------|----------|
| `admin`  | `Admin@2024` |

You are asked to choose a new password the first time you sign in. Do that —
the default password is published here and in the source code, so it is not a
secret.

---

## 3. Load the demonstration fleet

The application starts empty apart from your administrator account. To explore
it with realistic data, add `--demo` when you start it:

```bash
java -jar FEMZYK-Fleet-Management.jar --demo
```

This loads 10 employees, 6 drivers, 10 vehicles, 5 vehicle assignments,
12 trips, 7 maintenance records, 10 fuel records and 6 expenses. Loading it a
second time is a no-op, so it is safe to leave in your shortcut.

To use the pre-populated database that ships in this folder instead, point
`--data` at a folder containing it **named `fleet.db`**:

```bash
java -jar FEMZYK-Fleet-Management.jar --data database\demo-data
```

The database file is renamed to `fleet.db` in that folder precisely because
the application always opens `fleet.db` inside whatever folder you give it. If
you point `--data` at a folder that does not contain a `fleet.db`, it will
create an empty one beside your data and you will see an empty fleet rather
than an error.

Two things to know before you do that:

- The application writes its `logs\` folder next to the database, so it will
  add files inside the package. That is harmless, but if you would rather keep
  the package pristine, copy the folder somewhere first and point `--data` at
  the copy.
- Whatever you do in this database is permanent. It is a real SQLite file, not
  a read-only sample, so if you want to experiment and start over, keep a
  backup of the original `demo-data` folder.

---

## 4. Where your data lives

By default the application creates and uses:

| Platform | Location |
|----------|----------|
| Windows  | `%USERPROFILE%\FemzykFleetManagement\` |
| macOS    | `~/FemzykFleetManagement/` |
| Linux    | `~/FemzykFleetManagement/` |

Inside that folder:

- `fleet.db` — the whole database, one SQLite file.
- `logs/` — application logs, useful if something goes wrong.

To keep data somewhere else (a USB stick, a shared drive, a separate company
file), pass a folder:

```bash
java -jar FEMZYK-Fleet-Management.jar --data "D:\Fleet Data"
```

**Backing up is just copying.** Close the application and copy `fleet.db`
somewhere safe. To restore, copy it back. There is no server and no separate
backup tool.

---

## 5. Checking it works without a screen

On a server, or over SSH, there is no display for the window to open on. Use the
self-check instead — it initialises the database, loads demo data if asked, and
verifies the contents:

```bash
java -jar FEMZYK-Fleet-Management.jar --headless-check --demo
```

Expected output:

```
FEMZYK Enterprise Fleet & Transport Management System v2.0.0
Database: /home/you/FemzykFleetManagement/fleet.db
Integrity check: ok
Demo data loaded: Summary[loaded=true, employees=10, drivers=6, vehicles=10, ...]
Users: 1, vehicles: 10, employees: 10, audit rows: 94
```

`Integrity check: ok` means the database file is valid. This is the same command
used to verify this build before release.

---

## 6. What is in this folder

```
FEMZYK-Fleet-Management.jar     the application (everything is inside this file)
windows\FEMZYK-Fleet.bat        double-click launcher for Windows
run-linux-macos.sh              launcher for macOS and Linux
database\demo-data\fleet.db    10 vehicles, 10 employees, 12 trips — ready to open
database\fleet-demo.db         the same data under its original name
database\schema.sql            the schema, for reference or manual inspection
sample-data\*.csv               CSV files to try the import feature
documentation\                  user guide, architecture, database design, test plan
screenshots\                    screenshots of the main screens
```

---

## 7. Common problems

**"Java was not found on this computer"**
Java is not installed, or not on your PATH. Install it (section 1). If it is
installed but the launcher still cannot find it, set the `JAVA_HOME` environment
variable to the folder containing `bin\java.exe`.

**The window opens and immediately closes**
Run it from a terminal with `java -jar FEMZYK-Fleet-Management.jar` so the error
message stays visible.

**"Unsupported class file major version"**
Your Java is too old. Install Java 17 or newer.

**"Database is locked"**
Another copy of the application is already running against the same data
folder. Close it, or use a different `--data` folder.

**The native-access warning on Java 24+**
```
WARNING: A restricted method in java.lang.System has been called
```
This comes from the SQLite driver and is harmless. It does not affect your
data. It does not appear on Java 17 or 21.

---

## 8. Signing in with different roles

The application has four roles. New accounts are created by an administrator
inside the application (**Administration → Users**), not by self-registration —
this edition is installed on one machine and has no e-mail service to verify
anyone's address.

| Role | Can do |
|------|--------|
| Administrator | everything, including user management |
| Fleet Manager | vehicles, drivers, assignments, maintenance, reports |
| Operations Officer | trips, fuel, day-to-day records |
| Viewer | read-only access to reports |

To see the permission model in action, create one account of each role and sign
in as each.

---

## 9. Where the rest of the system is

This package is the **desktop** edition — a single-machine application.

The same system also ships as a **web edition** (a hosted, multi-user
application reachable from any browser), which is the version deployed at
<https://femzyk-fleet.vercel.app>. That edition is in the `femzyk-fleet-web-3.0.0`
package and in the `web/` folder of the repository, and it is the one to use if
several people in different locations need to work on the same data at once.

Both editions share the same business rules, the same role model and the same
audit approach; only the storage engine and the way you reach them differ.
