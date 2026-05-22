# 🚗✨ Femzyk Car Rental Agency — Vehicle Management System v4.0+

<div align="center">

![Java](https://img.shields.io/badge/Made%20with-Java%2017-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/GUI-Java%20Swing-2563EB?style=for-the-badge)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Storage](https://img.shields.io/badge/Storage-Local%20DAT%20Files-16A34A?style=for-the-badge)
![Security](https://img.shields.io/badge/Security-PBKDF2%20Password%20Hashing-7C3AED?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Desktop%20Stable-success?style=for-the-badge)
![Future](https://img.shields.io/badge/Future-Web%20%7C%20Mobile%20PWA-purple?style=for-the-badge)

### A premium Java Swing desktop fleet management system with authentication, persistent storage, renter tracking, recycle bin recovery, and professional dark-theme UI.

**Desktop Today · Web Portal Next · Mobile/PWA Future**

</div>

---

## 📌 Table of Contents

- [📖 Executive Summary](#-executive-summary)
- [🎯 Project Objectives](#-project-objectives)
- [📸 Screenshots](#-screenshots)
- [✨ Feature Highlights](#-feature-highlights)
- [🔐 Authentication and Security](#-authentication-and-security)
- [🚘 Vehicle Management](#-vehicle-management)
- [🗑️ Recycle Bin System](#️-recycle-bin-system)
- [💾 Persistent Storage](#-persistent-storage)
- [📊 Dashboard and Statistics](#-dashboard-and-statistics)
- [🧠 OOP Concepts Demonstrated](#-oop-concepts-demonstrated)
- [🛠️ Tech Stack](#️-tech-stack)
- [📁 Project Structure](#-project-structure)
- [🚀 Getting Started](#-getting-started)
- [🧪 Testing Guide](#-testing-guide)
- [🗺️ Roadmap](#️-roadmap)
- [🌐 Future Web and Mobile Expansion](#-future-web-and-mobile-expansion)
- [👨‍💻 Author](#-author)
- [🙏 Acknowledgments](#-acknowledgments)

---

## 📖 Executive Summary

**Femzyk Car Rental Agency — Vehicle Management System** is a premium Java Swing desktop application built to manage a rental fleet containing **cars**, **motorcycles**, and **trucks**.

The system is designed as a professional-grade academic desktop application that demonstrates strong software engineering practices, including:

- 🔐 Secure multi-user authentication
- 📧 Email-based account recovery
- 🔑 Password hashing with PBKDF2
- 🚘 Vehicle CRUD operations
- 👤 Optional renter information
- 💾 Persistent local storage
- 🗑️ Soft delete with Recycle Bin
- ♻️ Vehicle recovery
- 📊 Real-time fleet statistics
- 🎨 Modern dark-themed user interface
- 🧱 Clean object-oriented architecture

This project is suitable for demonstrating **Java programming**, **GUI development**, **OOP principles**, **file-based persistence**, **authentication workflows**, and **desktop application design**.

---

## 🎯 Project Objectives

The system was developed to achieve the following objectives:

| No. | Objective |
|---:|---|
| 1 | Provide a graphical system for managing rental vehicles |
| 2 | Support multiple vehicle categories: cars, motorcycles, and trucks |
| 3 | Allow users to add, edit, view, and safely delete vehicle records |
| 4 | Store optional renter name and phone number |
| 5 | Save user and fleet data automatically |
| 6 | Support multiple users through signup and login |
| 7 | Keep each user's fleet data separate |
| 8 | Provide password reset using registered email |
| 9 | Prevent accidental deletion using a Recycle Bin |
| 10 | Demonstrate professional OOP architecture and clean code practices |

---

## 📸 Screenshots

The `screenshots/` folder contains visual documentation of the application interface, including authentication, vehicle creation, editing, dashboard layout, recycle bin actions, validation errors, and GitHub deployment progress.

### 🔐 Authentication Screens

| First Screen | Login With Details | Sign Up |
|---|---|---|
| ![Login Page](screenshots/Login%20page%20with%20details%20.png) | ![Sign Up Page](screenshots/sign%20up%20page.png) |

| Signing Up Details | Invalid Email Error | Username Already Exists |
|---|---|---|
| ![Signing Up Details](screenshots/signing%20up%20details.png) | ![Invalid Email](screenshots/using%20invalid%20email%20to%20register.png) | ![Username Exists](screenshots/signing%20up%20using%20a%20username%20that%20already%20exists.png) |

| Email Already Used | Password Reset |
|---|---|
| ![Email Already Used](screenshots/email%20already%20used%20error.png) | ![Password Reset](screenshots/reset%20password%20interface.png) |

---

### 📊 Dashboard and Fleet Display

| Dashboard After Login | Dashboard With Vehicles and Scroll |
|---|---|
| ![Dashboard After Login](screenshots/Dashboard%20after%20login.png) | ![Dashboard With Vehicles](screenshots/the%20dashboard%20after%20adding%20vehicles%20with%20vertical%20and%20horizontal%20scroll%20enabled.png) |

| Screen Auto Resizing | User Separation |
|---|---|
| ![Screen Auto Resizing](screenshots/screen%20autoresizing.png) | ![User Separation](screenshots/user%20separation%20using%20different%20accounts.png) |

---

### 🚗 Adding Vehicles

| Add Car Form | Car Fuel Options |
|---|---|
| ![Add Car](screenshots/Adding%20a%20car.png) | ![Car Options](screenshots/Adding%20a%20car%20options.png) |

| Add Motorcycle Form | Motorcycle Options |
|---|---|
| ![Add Motorcycle](screenshots/Adding%20a%20Motorcycle.png) | ![Motorcycle Options](screenshots/Adding%20a%20Motorcycle%20options.png) |

| Add Truck Form | Truck Options |
|---|---|
| ![Add Truck](screenshots/Adding%20a%20Truck.png) | ![Truck Options](screenshots/Adding%20a%20Truck%20options.png) |

---

### ✏️ Editing Vehicles

| Edit Car | Edit Motorcycle | Edit Truck |
|---|---|---|
| ![Edit Car](screenshots/editing%20car%20info.png) | ![Edit Motorcycle](screenshots/Editing%20motorcycle%20info.png) | ![Edit Truck](screenshots/editing%20truck%20info.png) |

---

### 🗑️ Recycle Bin and Deletion Workflow

| Delete Warning | Recycle Bin Interface | Select Vehicle To Recover |
|---|---|---|
| ![Delete Warning](screenshots/delete%20notification%20or%20warning.png) | ![Recycle Bin](screenshots/recycle%20bin%20interface.png) | ![Select Vehicle To Recover](screenshots/select%20vehicle%20to%20recover.png) |

| Permanent Delete Warning | Clear Bin Warning | Clear Fleet Warning |
|---|---|---|
| ![Permanent Delete](screenshots/permanently%20delete%20notification%20or%20warning.png) | ![Clear Bin](screenshots/clear%20all%20bin%20warning.png) | ![Clear Fleet](screenshots/clear%20fleet%20warning.png) |

---

### 🚪 Session and Deployment

| Logout Confirmation | GitHub Push |
|---|---|
| ![Logout](screenshots/log%20out%20notification.png) | ![GitHub Push](screenshots/WHILE%20PUSHING%20TO%20GITHUB.png) |
---

## ✨ Feature Highlights

### 🧩 Core Application Features

- 🚘 Add, edit, and manage cars
- 🏍️ Add, edit, and manage motorcycles
- 🚚 Add, edit, and manage trucks
- 👤 Optional renter name and phone number
- 🗑️ Recycle Bin for safe deletion
- ♻️ Recover deleted vehicles
- 💥 Permanent deletion from Recycle Bin
- 🔥 Clear Recycle Bin
- 💾 Automatic saving
- 📊 Real-time statistics panel
- 🎨 Premium dark theme
- ↔️ Horizontal scrolling for each vehicle section
- ↕️ Vertical scrolling for dashboard navigation
- 🖱️ Touchpad-friendly scrolling behavior

---

## 🔐 Authentication and Security

The application includes a full local authentication system.

### 🔑 Authentication Features

| Feature | Description |
|---|---|
| 👤 Signup | New users can create accounts |
| 🔓 Login | Existing users can securely log in |
| 📧 Email validation | Signup requires a valid email |
| 👁️ Show password | Users can reveal password while typing |
| 🔁 Forgot password | Users can reset password using username and email |
| 🔐 Password hashing | Plain passwords are never stored |
| 👥 User separation | Each user has separate fleet data |

### 🛡️ Password Security

Passwords are hashed using:
PBKDF2WithHmacSHA256

The system stores:

Username
Email
Password salt
Password hash
Date created
Last login date
Last password reset date
```

The system does **not** store plain text passwords.

---

## 🚘 Vehicle Management

The system supports three vehicle categories.

---

### 🚗 Cars

A car record includes:

```text
Make
Model
Year
Number of doors
Fuel type
Optional renter name
Optional renter phone
```

Fuel type options:

```text
PETROL
DIESEL
ELECTRIC
```

---

### 🏍️ Motorcycles

A motorcycle record includes:

```text
Make
Model
Year
Number of wheels
Motorcycle type
Optional renter name
Optional renter phone
```

Motorcycle type options:

```text
SPORT
CRUISER
OFF-ROAD
```

---

### 🚚 Trucks

A truck record includes:

```text
Make
Model
Year
Cargo capacity
Transmission type
Optional renter name
Optional renter phone
```

Transmission options:

```text
MANUAL
AUTOMATIC
```

---

## 👤 Renter Information

Renter details are optional.

Each vehicle can store:

```text
Renter name
Renter phone number
```

If no renter is assigned, the system displays:

```text
Renter: Not assigned
Phone: N/A
```

Phone numbers are validated only when entered.

Accepted phone examples:

```text
08012345678
+1 555 123 4567
(555) 123-4567
555-123-4567
```

---

## 🗑️ Recycle Bin System

The application uses soft deletion.

When a vehicle is deleted, it is moved to the Recycle Bin instead of being removed permanently.

### 🧹 Recycle Bin Actions

| Action | Result |
|---|---|
| 🗑️ Delete vehicle | Moves vehicle to Recycle Bin |
| ♻️ Recover | Restores vehicle to active fleet |
| 💥 Delete Permanently | Removes vehicle forever |
| 🔥 Clear Bin | Permanently deletes all recycled vehicles |

This prevents accidental data loss and gives users a chance to restore deleted vehicles.

---

## 💾 Persistent Storage

The application uses Java serialization and `.dat` files for local storage.

### 📂 Storage Structure

Application data is stored under the current user's home folder:

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

### 💽 Saved Data Includes

```text
Registered users
Password salts
Password hashes
Active fleet vehicles
Recycle bin vehicles
Last saved date/time
Application version
```

### ⚙️ Auto-Save Events

The system saves automatically when:

- A vehicle is added
- A vehicle is edited
- A vehicle is moved to Recycle Bin
- A vehicle is recovered
- A vehicle is permanently deleted
- The Recycle Bin is cleared
- The fleet is cleared
- The user logs out
- The application closes

---

## 📊 Dashboard and Statistics

The statistics panel displays:

```text
Current logged-in user
Total active vehicles
Number of cars
Number of motorcycles
Number of trucks
Recycle Bin count
Last added vehicle
```

This gives users an immediate overview of the fleet composition.

---

## 🎨 UI/UX Design

The application uses a professional dark interface.

### 🎭 Design Highlights

- 🌑 Deep navy background
- 🔵 Blue accent for cars
- 🟠 Orange accent for motorcycles
- 🔴 Red accent for trucks and danger actions
- 🟢 Green accent for recovery/success actions
- 🧾 Card-based fleet layout
- ↔️ Horizontal scroll per vehicle section
- ↕️ Vertical scroll for the full dashboard
- 🖱️ Touchpad-friendly interaction
- 🧩 Structured dialogs for add/edit operations

---

## 🧠 OOP Concepts Demonstrated

| OOP Concept | How It Appears |
|---|---|
| 🧱 Encapsulation | Private fields with getters and setters |
| 🎭 Abstraction | `Vehicle` interface defines shared vehicle behavior |
| 🔁 Polymorphism | `List<Vehicle>` stores cars, motorcycles, and trucks |
| 🧩 Interface Segregation | `CarVehicle`, `MotorVehicle`, and `TruckVehicle` separate type-specific behavior |
| 🧼 Separation of Concerns | GUI, storage, authentication, models, and exceptions are separated |
| 🚨 Exception Handling | Custom validation exceptions improve error handling |
| 💾 Persistence | Serializable model objects are saved to local `.dat` files |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| ☕ Language | Java 17 |
| 🖥️ GUI Framework | Java Swing |
| 🧰 Build Tool | Maven |
| 💾 Storage | Java Serialization / `.dat` files |
| 🔐 Authentication | Local user database |
| 🔑 Password Security | PBKDF2WithHmacSHA256 |
| 🧱 Architecture | Object-Oriented Programming |
| 🧪 Testing Method | Manual functional testing + Maven compile validation |

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

### 📌 Prerequisites

Install:

- Java 17 or later
- Maven
- A Java IDE such as IntelliJ IDEA, VS Code, Eclipse, or Visual Studio with Java/Maven support

Verify installation:

```powershell
java -version
mvn -version
```

---

## 💻 Build and Run

### 🧱 Compile

```powershell
mvn clean compile
```

Expected result:

```text
BUILD SUCCESS
```

### ▶️ Run

```powershell
mvn exec:java
```

Alternative:

```powershell
java -cp target\classes com.femzyk.vehiclesystem.VehicleRentalSystem
```

---

## 📖 Usage Guide

### 👤 Create Account

1. Open the application.
2. Click **Sign Up**.
3. Enter username.
4. Enter email.
5. Enter password.
6. Confirm password.
7. Click **Sign Up**.

### 🔓 Login

1. Enter username.
2. Enter password.
3. Optionally check **Show password**.
4. Click **Login**.

### 🔁 Reset Password

1. Click **Forgot Password**.
2. Enter username.
3. Enter registered email.
4. Enter new password.
5. Confirm new password.
6. Click **Reset Password**.

### ➕ Add Vehicle

Use the toolbar buttons:

```text
Add Car
Add Motorcycle
Add Truck
```

### ✏️ Edit Vehicle

Click:

```text
Edit
```

on a vehicle card.

### 🗑️ Delete Vehicle

Click:

```text
Delete
```

The vehicle moves to Recycle Bin.

### ♻️ Recover Vehicle

1. Click **Recycle Bin**.
2. Select a vehicle.
3. Click **Recover**.

### 🚪 Logout

Click:

```text
Logout
```

The system saves data and returns to the login screen.

---

## 🧪 Testing Guide

A full testing checklist is available in:

```text
docs/TEST_PLAN.md
```

Basic verification:

```powershell
mvn clean compile
mvn exec:java
```

### Recommended Manual Tests

- Signup with valid email
- Signup with invalid email
- Duplicate username rejection
- Duplicate email rejection
- Login with correct password
- Login with wrong password
- Password reset
- Add vehicle
- Edit vehicle
- Delete vehicle
- Recover vehicle
- Clear Recycle Bin
- Logout and login again
- Confirm saved data reloads
- Confirm different users have separate fleets

---

## 🗺️ Roadmap

### ✅ Completed

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
- [x] Touchpad-friendly scrolling

### 🚧 Planned Desktop Enhancements

- [ ] Vehicle search
- [ ] Vehicle filtering
- [ ] Export to CSV
- [ ] Import from CSV
- [ ] Backup and restore button
- [ ] Admin role
- [ ] Light/dark theme toggle
- [ ] Printable rental reports
- [ ] Rental price tracking
- [ ] Rental due dates

### 🌐 Planned Web Portal

- [ ] Java Spring Boot backend
- [ ] REST API authentication
- [ ] Browser-based dashboard
- [ ] Responsive web interface
- [ ] Website integration support
- [ ] Database migration to SQLite/PostgreSQL

### 📱 Planned Mobile/PWA

- [ ] Responsive mobile interface
- [ ] Progressive Web App support
- [ ] Installable mobile experience
- [ ] Touch-first forms
- [ ] Mobile-friendly dashboard

---

## 🌐 Future Web and Mobile Direction

The long-term plan is to evolve the system into three connected platforms:

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

This direction allows the system to later integrate into another website and support phone, tablet, and laptop users.

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

## ⭐ Show Your Support

If this project is helpful, educational, or inspiring, consider giving it a star on GitHub.

**Built with dedication using Java 17, Swing, Maven, and OOP principles.**

Desktop Today · Web Portal Next · Mobile/PWA Future

</div>
```
