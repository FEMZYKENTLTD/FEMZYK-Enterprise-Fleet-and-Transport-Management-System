# ============================================================
# Femzyk Vehicle Management System - Documentation Generator
# ============================================================
# Run this script from the project root folder.
# It creates premium documentation files and folders:
# - README.md
# - docs/PROJECT_PLAN.md
# - docs/USER_GUIDE.md
# - docs/TECHNICAL_DESIGN.md
# - docs/TEST_PLAN.md
# - docs/ROADMAP.md
# - screenshots/
# ============================================================

$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

function Write-Utf8File {
    param (
        [string]$Path,
        [string]$Content
    )

    $FullPath = Join-Path (Get-Location) $Path
    $Folder = Split-Path $FullPath -Parent

    if (!(Test-Path $Folder)) {
        New-Item -ItemType Directory -Path $Folder -Force | Out-Null
    }

    $Utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($FullPath, $Content, $Utf8NoBom)
}

$BackupFolder = ".\backups"
$DocsFolder = ".\docs"
$ScreenshotsFolder = ".\screenshots"

New-Item -ItemType Directory -Path $BackupFolder -Force | Out-Null
New-Item -ItemType Directory -Path $DocsFolder -Force | Out-Null
New-Item -ItemType Directory -Path $ScreenshotsFolder -Force | Out-Null

if (Test-Path ".\README.md") {
    Copy-Item ".\README.md" "$BackupFolder\README-backup-$Timestamp.md" -Force
}

if ((Test-Path ".\docs") -and ((Get-ChildItem ".\docs" -Force | Measure-Object).Count -gt 0)) {
    Compress-Archive -Path ".\docs" -DestinationPath "$BackupFolder\docs-backup-$Timestamp.zip" -Force
}

$Readme = @'
# 🚗 Femzyk Car Rental Agency — Vehicle Management System v4.0+

A premium Java Swing desktop application for managing a vehicle rental fleet, featuring **multi-user authentication**, **secure password hashing**, **email-based account recovery**, **vehicle CRUD operations**, **optional renter tracking**, **auto-save**, **user-specific profiles**, and a **Recycle Bin** for safe deletion.

![Java](https://img.shields.io/badge/Made%20with-Java%2017-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/GUI-Java%20Swing-2563EB?style=for-the-badge)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Storage](https://img.shields.io/badge/Storage-Local%20DAT%20Files-16A34A?style=for-the-badge)
![Platform](https://img.shields.io/badge/Desktop-Windows%20%7C%20Java%20Runtime-blue?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Desktop%20Stable-success?style=for-the-badge)
![Future](https://img.shields.io/badge/Future-Web%20%7C%20Mobile%20PWA-purple?style=for-the-badge)

---

## 📸 Screenshots

Screenshots of the interface should be placed inside the `screenshots/` folder.

Suggested screenshots:

```text
screenshots/
    login-screen.png
    signup-screen.png
    password-reset.png
    dashboard.png
    add-car-dialog.png
    fleet-sections.png
    recycle-bin.png
```

---

## 🎯 About The Project

**Femzyk Car Rental Agency — Vehicle Management System** is a professional desktop fleet management application built with **Java 17**, **Java Swing**, and **Maven**.

The system is designed for a rental agency that manages multiple vehicle categories:

- 🚘 Cars
- 🏍 Motorcycles
- 🚚 Trucks

The application demonstrates modern object-oriented programming principles and desktop application design, including:

- 🔐 Secure user authentication
- 📧 Email-based account recovery
- 💾 Persistent local storage
- 🗑️ Soft delete with Recycle Bin
- ♻️ Vehicle recovery system
- 👤 User-specific profiles
- 🧾 Optional renter information
- 📊 Real-time fleet statistics
- 🎨 Premium dark-themed GUI
- 🧱 Clean object-oriented architecture

---

## ✨ Features

### 🔐 Authentication & Account Management

- ✅ User signup
- ✅ User login
- ✅ Email validation during signup
- ✅ Show/hide password option
- ✅ Forgot password / password reset
- ✅ Password reset using username + registered email
- ✅ Secure password hashing with `PBKDF2WithHmacSHA256`
- ✅ Separate saved data for each user

### 🚗 Vehicle Management

- ✅ Add cars
- ✅ Add motorcycles
- ✅ Add trucks
- ✅ Edit vehicle details
- ✅ Delete vehicles safely
- ✅ View vehicles by category
- ✅ Store optional renter details
- ✅ Auto-save vehicle records

### 🗑️ Recycle Bin

- ✅ Deleted vehicles move to Recycle Bin
- ✅ Recover deleted vehicles
- ✅ Permanently delete selected vehicles
- ✅ Clear entire Recycle Bin
- ✅ Recycle Bin count shown in statistics

### 💾 Persistent Storage

The application saves data automatically using local `.dat` files.

Saved data includes:

```text
Registered users
Password hashes and salts
User-specific active fleet
User-specific recycle bin
Last saved date/time
```

### 📊 Fleet Statistics

The dashboard sidebar displays:

```text
Current user
Total active vehicles
Cars count
Motorcycles count
Trucks count
Recycle Bin count
Last added vehicle
```

### 🎨 UI/UX Excellence

- 🎨 Professional dark theme
- 🌈 Vehicle-type color coding
- 🧩 Card-based fleet display
- ↔️ Horizontal scrolling per vehicle section
- ↕️ Vertical scrolling for full dashboard
- 🖱️ Touchpad-friendly scrolling
- 🔘 Styled toolbar buttons
- 🧾 User-friendly validation messages

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| GUI | Java Swing |
| Build Tool | Maven |
| Storage | Java Serialization / `.dat` files |
| Authentication | Local user database |
| Password Security | PBKDF2WithHmacSHA256 |
| Architecture | Object-Oriented Programming |

---

## 📁 Project Structure

```text
unit 6 Vehicle Information System/
│
├── pom.xml
├── README.md
│
├── docs/
│   ├── PROJECT_PLAN.md
│   ├── USER_GUIDE.md
│   ├── TECHNICAL_DESIGN.md
│   ├── TEST_PLAN.md
│   └── ROADMAP.md
│
├── screenshots/
├── backups/
│
└── src/main/java/com/femzyk/vehiclesystem/
    │
    ├── VehicleRentalSystem.java
    │
    ├── exception/
    │   ├── VehicleException.java
    │   ├── InvalidCargoException.java
    │   ├── InvalidDoorsException.java
    │   ├── InvalidFuelTypeException.java
    │   ├── InvalidTransmissionException.java
    │   ├── InvalidWheelsException.java
    │   └── InvalidYearException.java
    │
    ├── interfaces/
    │   ├── Vehicle.java
    │   ├── CarVehicle.java
    │   ├── MotorVehicle.java
    │   └── TruckVehicle.java
    │
    ├── model/
    │   ├── Car.java
    │   ├── Motorcycle.java
    │   └── Truck.java
    │
    └── gui/
        ├── MainWindow.java
        ├── LoginDialog.java
        ├── PasswordResetDialog.java
        ├── AddCarDialog.java
        ├── AddMotorcycleDialog.java
        ├── AddTruckDialog.java
        ├── FleetPanel.java
        ├── VehicleCardPanel.java
        ├── StatsPanel.java
        ├── RecycleBinDialog.java
        ├── ThemeConstants.java
        ├── UserAccount.java
        ├── UserDatabase.java
        ├── PasswordUtil.java
        ├── StorageManager.java
        └── StoredFleetData.java
```

---

## 🚀 Getting Started

### Prerequisites

Install:

- Java 17+
- Maven
- A Java IDE such as IntelliJ IDEA, VS Code, Eclipse, or Visual Studio with Java/Maven support

Verify installation:

```powershell
java -version
mvn -version
```

---

## 💻 Build and Run

### Compile

```powershell
mvn clean compile
```

Expected:

```text
BUILD SUCCESS
```

### Run

```powershell
mvn exec:java
```

Alternative:

```powershell
java -cp target\classes com.femzyk.vehiclesystem.VehicleRentalSystem
```

---

## 📖 Usage Guide

### Create Account

1. Open the app.
2. Go to **Sign Up**.
3. Enter username, email, password, and confirm password.
4. Click **Sign Up**.

### Login

1. Enter username.
2. Enter password.
3. Optionally check **Show password**.
4. Click **Login**.

### Reset Password

1. Click **Forgot Password**.
2. Enter username and registered email.
3. Enter and confirm a new password.
4. Click **Reset Password**.

---

## 🚘 Vehicle Types

### Car

```text
Make
Model
Year
Number of doors
Fuel type
Optional renter name
Optional renter phone
```

Fuel types:

```text
PETROL
DIESEL
ELECTRIC
```

### Motorcycle

```text
Make
Model
Year
Number of wheels
Motorcycle type
Optional renter name
Optional renter phone
```

Motorcycle types:

```text
SPORT
CRUISER
OFF-ROAD
```

### Truck

```text
Make
Model
Year
Cargo capacity
Transmission type
Optional renter name
Optional renter phone
```

Transmission types:

```text
MANUAL
AUTOMATIC
```

---

## 🗑️ Recycle Bin Explained

| Action | Result |
|---|---|
| Delete vehicle | Moves vehicle to Recycle Bin |
| Recover | Restores vehicle to active fleet |
| Delete Permanently | Removes vehicle forever |
| Clear Bin | Permanently deletes all recycled vehicles |

---

## 💾 Storage Location

Application data is stored under the current Windows user's home folder:

```text
user-home/FemzykVehicleSystem/
```

Typical Windows example:

```text
C:/Users/FemiBenua/FemzykVehicleSystem/
```

Inside:

```text
FemzykVehicleSystem/
    users.dat
    profiles/
        username/
            fleet-data.dat
```

---

## 🧪 Testing

Full testing checklist is available in:

```text
docs/TEST_PLAN.md
```

Basic test command:

```powershell
mvn clean compile
mvn exec:java
```

---

## 🧠 OOP Concepts Demonstrated

| Concept | How It Appears |
|---|---|
| Encapsulation | Private fields with getters/setters |
| Abstraction | Vehicle interface |
| Polymorphism | List of Vehicle objects stores all vehicle types |
| Interface Segregation | CarVehicle, MotorVehicle, TruckVehicle |
| Separation of Concerns | GUI, model, storage, auth, and exception classes separated |
| Exception Handling | Custom validation exceptions |

---

## 🗺️ Roadmap

### Completed

- [x] Java Swing desktop application
- [x] Vehicle CRUD
- [x] User signup/login
- [x] Email validation
- [x] Password reset
- [x] Secure password hashing
- [x] User-specific profiles
- [x] Persistent storage
- [x] Recycle Bin
- [x] Auto-save
- [x] Fleet statistics
- [x] Scrollable vehicle sections

### Planned

- [ ] Vehicle search
- [ ] Vehicle filters
- [ ] Export to CSV
- [ ] Import from CSV
- [ ] Backup/restore button
- [ ] Admin role
- [ ] Web portal
- [ ] Mobile/PWA version
- [ ] Backend API
- [ ] SQLite/PostgreSQL migration
- [ ] Integration into another website

---

## 🌐 Future Web and Mobile Direction

The long-term plan is to expand the system into:

```text
1. Desktop Application
2. Website Portal
3. Mobile/PWA Application
```

Recommended future architecture:

```text
Shared Backend API
Desktop Client
Responsive Web Portal
Mobile/PWA Frontend
Shared Database
```

---

## 👨‍💻 Author

**Olufemi Keripe**  
**Brand:** FEMZYK ENTERPRISES LTD  
**Project:** CS 1102-01 Unit 6 Vehicle Information System  

- 🌍 Nigeria
- 💼 AI Powered Full-Stack Developer
- 🐙 GitHub: https://github.com/FEMZYKENTLTD
- 📧 Email: femzykenterprises@gmail.com

---

## 🙏 Acknowledgments

- University of the People
- CS 1102-01 Programming Course
- Java and Maven ecosystem
- Java Swing framework
- FEMZYK ENTERPRISES LTD

---

<div align="center">

**Built with dedication using Java 17, Swing, Maven, and OOP principles.**

Desktop Today · Web Portal Next · Mobile/PWA Future

</div>
'@

Write-Utf8File ".\README.md" $Readme

$ProjectPlan = @'
# Standard Project Plan
## Femzyk Car Rental Agency — Vehicle Management System v4.0+

---

## 1. Project Overview

The Femzyk Car Rental Agency Vehicle Management System is a Java Swing desktop application designed to manage a rental fleet consisting of cars, motorcycles, and trucks.

The application supports vehicle registration, editing, deletion, renter information, statistics, persistent storage, user-based profiles, authentication, password recovery, and recycle bin recovery.

---

## 2. Project Objectives

1. Allow users to manage a vehicle rental fleet through a graphical interface.
2. Support cars, motorcycles, and trucks.
3. Allow users to add, edit, view, and delete vehicle records.
4. Store optional renter name and renter phone number.
5. Persist application data across program restarts.
6. Support multiple users through signup and login.
7. Keep each user's fleet separate.
8. Provide account recovery using registered email.
9. Provide a recycle bin for deleted vehicles.
10. Allow recovery or permanent deletion of recycled vehicles.
11. Auto-save data after important actions.
12. Maintain professional and well-commented code.
13. Prepare for future desktop, web, and mobile expansion.

---

## 3. Current Features

- Multi-user signup and login
- Email validation
- Password reset
- Secure password hashing
- Cars, motorcycles, and trucks
- Optional renter details
- Edit and delete operations
- Recycle bin recovery
- Persistent local storage
- User-specific profiles
- Fleet statistics
- Premium Swing GUI

---

## 4. Future Expansion

The project may later become:

```text
1. Desktop Application
2. Website Portal
3. Mobile/PWA Application
```

Recommended future architecture:

```text
Backend API
Desktop client
Responsive web portal
Mobile/PWA frontend
Shared database
```
'@

Write-Utf8File ".\docs\PROJECT_PLAN.md" $ProjectPlan

$UserGuide = @'
# User Guide
## Femzyk Car Rental Agency — Vehicle Management System

---

## 1. Starting the Application

```powershell
mvn exec:java
```

---

## 2. Creating an Account

1. Click **Sign Up**.
2. Enter username.
3. Enter email.
4. Enter password.
5. Confirm password.
6. Click **Sign Up**.

---

## 3. Logging In

1. Enter username.
2. Enter password.
3. Optionally check **Show password**.
4. Click **Login**.

---

## 4. Resetting Password

1. Click **Forgot Password**.
2. Enter username.
3. Enter registered email.
4. Enter new password.
5. Confirm new password.
6. Click **Reset Password**.

---

## 5. Adding Vehicles

Use:

```text
Add Car
Add Motorcycle
Add Truck
```

---

## 6. Editing Vehicles

Click **Edit** on a vehicle card.

---

## 7. Deleting Vehicles

Click **Delete**. The vehicle moves to Recycle Bin.

---

## 8. Recycle Bin

Click **Recycle Bin**.

Available actions:

```text
Recover
Delete Permanently
Clear Bin
Close
```

---

## 9. Logout

Click **Logout**.
'@

Write-Utf8File ".\docs\USER_GUIDE.md" $UserGuide

$TechnicalDesign = @'
# Technical Design
## Femzyk Vehicle Management System

---

## 1. Architecture Overview

```text
Entry Point
Authentication
GUI
Fleet Management
Model Classes
Interfaces
Exceptions
Storage
```

---

## 2. Key Components

| Component | Purpose |
|---|---|
| VehicleRentalSystem | Application entry point |
| MainWindow | Main dashboard |
| LoginDialog | Login/signup interface |
| PasswordResetDialog | Password recovery |
| FleetPanel | Active fleet and recycle bin logic |
| VehicleCardPanel | Vehicle card UI |
| StatsPanel | Fleet statistics |
| StorageManager | User fleet storage |
| UserDatabase | User account storage |
| PasswordUtil | Password hashing |
| StoredFleetData | Serialized fleet data |
| UserAccount | Serialized user account |

---

## 3. OOP Principles

| Principle | Application |
|---|---|
| Encapsulation | Private fields and getters/setters |
| Abstraction | Vehicle interface |
| Polymorphism | Vehicle list stores different concrete vehicle types |
| Interface Segregation | CarVehicle, MotorVehicle, TruckVehicle |
| Separation of Concerns | GUI, auth, storage, model separated |
'@

Write-Utf8File ".\docs\TECHNICAL_DESIGN.md" $TechnicalDesign

$TestPlan = @'
# Test Plan
## Femzyk Vehicle Management System

---

## 1. Build Test

```powershell
mvn clean compile
```

Expected:

```text
BUILD SUCCESS
```

---

## 2. Authentication Tests

| Test | Expected |
|---|---|
| Signup invalid email | Error message |
| Signup valid email | Account created |
| Duplicate username | Rejected |
| Duplicate email | Rejected |
| Wrong password | Rejected |
| Correct login | Main window opens |
| Show password | Password becomes visible |

---

## 3. Password Reset Tests

| Test | Expected |
|---|---|
| Wrong email | Reset rejected |
| Correct email | Password reset |
| Old password | Login fails |
| New password | Login succeeds |

---

## 4. Vehicle Tests

| Test | Expected |
|---|---|
| Add car | Appears in CARS |
| Add motorcycle | Appears in MOTORCYCLES |
| Add truck | Appears in TRUCKS |
| Edit vehicle | Details update |
| Blank renter | Shows Not assigned / N/A |
| Invalid phone | Error message |

---

## 5. Recycle Bin Tests

| Test | Expected |
|---|---|
| Delete vehicle | Moves to bin |
| Recover vehicle | Returns to active fleet |
| Delete permanently | Cannot be recovered |
| Clear bin | Bin becomes empty |

---

## 6. Persistence Tests

| Test | Expected |
|---|---|
| Restart app | Saved vehicles reload |
| Different user | Separate fleet |

---

## 7. Scrolling Tests

| Test | Expected |
|---|---|
| Many cars | Horizontal scrollbar appears |
| Touchpad scroll | Vertical scrolling works |
'@

Write-Utf8File ".\docs\TEST_PLAN.md" $TestPlan

$Roadmap = @'
# Roadmap
## Femzyk Vehicle Management System

---

## Completed Desktop Features

- [x] Java Swing desktop application
- [x] Vehicle CRUD
- [x] User signup/login
- [x] Email validation
- [x] Password reset
- [x] Secure password hashing
- [x] User-specific profiles
- [x] Local `.dat` storage
- [x] Recycle bin
- [x] Auto-save
- [x] Fleet statistics
- [x] Horizontal vehicle sections
- [x] Touchpad scroll support

---

## Planned Desktop Enhancements

- [ ] Search vehicles
- [ ] Filter vehicles
- [ ] Export to CSV
- [ ] Import from CSV
- [ ] Backup/restore button
- [ ] Admin user role
- [ ] Light/dark theme toggle
- [ ] Printable rental reports
- [ ] Rental price tracking
- [ ] Rental due dates

---

## Web Portal Plan

Recommended stack:

```text
Backend: Java Spring Boot
Frontend: React or HTML/CSS/JavaScript
Database: SQLite first, PostgreSQL later
Authentication: API-based login
```

---

## Mobile/PWA Plan

Recommended first mobile version:

```text
Progressive Web App
```
'@

Write-Utf8File ".\docs\ROADMAP.md" $Roadmap

$GitIgnore = @'
# Java / Maven
target/
*.class

# IDE
.idea/
.vscode/
*.iml
.project
.classpath
.settings/

# Logs
*.log

# OS files
.DS_Store
Thumbs.db

# Backups
backups/

# Local generated app data
FemzykVehicleSystem/
*.dat

# Temporary files
*.tmp
*.bak
'@

if (-not (Test-Path ".\.gitignore")) {
    Write-Utf8File ".\.gitignore" $GitIgnore
}

Write-Host ""
Write-Host "Documentation created successfully."
Write-Host ""
Write-Host "Created or updated:"
Write-Host " - README.md"
Write-Host " - docs/PROJECT_PLAN.md"
Write-Host " - docs/USER_GUIDE.md"
Write-Host " - docs/TECHNICAL_DESIGN.md"
Write-Host " - docs/TEST_PLAN.md"
Write-Host " - docs/ROADMAP.md"
Write-Host " - screenshots/"
Write-Host " - .gitignore if it did not already exist"
Write-Host ""
