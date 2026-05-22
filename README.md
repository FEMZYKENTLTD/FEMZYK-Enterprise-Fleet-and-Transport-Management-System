# ðŸš— Femzyk Car Rental Agency â€” Vehicle Management System v4.0+

A premium Java Swing desktop application for managing a vehicle rental fleet, featuring **multi-user authentication**, **secure password hashing**, **email-based account recovery**, **vehicle CRUD operations**, **optional renter tracking**, **auto-save**, **user-specific profiles**, and a **Recycle Bin** for safe deletion.

![Java](https://img.shields.io/badge/Made%20with-Java%2017-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/GUI-Java%20Swing-2563EB?style=for-the-badge)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Storage](https://img.shields.io/badge/Storage-Local%20DAT%20Files-16A34A?style=for-the-badge)
![Platform](https://img.shields.io/badge/Desktop-Windows%20%7C%20Java%20Runtime-blue?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Desktop%20Stable-success?style=for-the-badge)
![Future](https://img.shields.io/badge/Future-Web%20%7C%20Mobile%20PWA-purple?style=for-the-badge)

---

## ðŸ“¸ Screenshots

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

## ðŸŽ¯ About The Project

**Femzyk Car Rental Agency â€” Vehicle Management System** is a professional desktop fleet management application built with **Java 17**, **Java Swing**, and **Maven**.

The system is designed for a rental agency that manages multiple vehicle categories:

- ðŸš˜ Cars
- ðŸ Motorcycles
- ðŸšš Trucks

The application demonstrates modern object-oriented programming principles and desktop application design, including:

- ðŸ” Secure user authentication
- ðŸ“§ Email-based account recovery
- ðŸ’¾ Persistent local storage
- ðŸ—‘ï¸ Soft delete with Recycle Bin
- â™»ï¸ Vehicle recovery system
- ðŸ‘¤ User-specific profiles
- ðŸ§¾ Optional renter information
- ðŸ“Š Real-time fleet statistics
- ðŸŽ¨ Premium dark-themed GUI
- ðŸ§± Clean object-oriented architecture

---

## âœ¨ Features

### ðŸ” Authentication & Account Management

- âœ… User signup
- âœ… User login
- âœ… Email validation during signup
- âœ… Show/hide password option
- âœ… Forgot password / password reset
- âœ… Password reset using username + registered email
- âœ… Secure password hashing with `PBKDF2WithHmacSHA256`
- âœ… Separate saved data for each user

### ðŸš— Vehicle Management

- âœ… Add cars
- âœ… Add motorcycles
- âœ… Add trucks
- âœ… Edit vehicle details
- âœ… Delete vehicles safely
- âœ… View vehicles by category
- âœ… Store optional renter details
- âœ… Auto-save vehicle records

### ðŸ—‘ï¸ Recycle Bin

- âœ… Deleted vehicles move to Recycle Bin
- âœ… Recover deleted vehicles
- âœ… Permanently delete selected vehicles
- âœ… Clear entire Recycle Bin
- âœ… Recycle Bin count shown in statistics

### ðŸ’¾ Persistent Storage

The application saves data automatically using local `.dat` files.

Saved data includes:

```text
Registered users
Password hashes and salts
User-specific active fleet
User-specific recycle bin
Last saved date/time
```

### ðŸ“Š Fleet Statistics

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

### ðŸŽ¨ UI/UX Excellence

- ðŸŽ¨ Professional dark theme
- ðŸŒˆ Vehicle-type color coding
- ðŸ§© Card-based fleet display
- â†”ï¸ Horizontal scrolling per vehicle section
- â†•ï¸ Vertical scrolling for full dashboard
- ðŸ–±ï¸ Touchpad-friendly scrolling
- ðŸ”˜ Styled toolbar buttons
- ðŸ§¾ User-friendly validation messages

---

## ðŸ› ï¸ Tech Stack

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

## ðŸ“ Project Structure

```text
unit 6 Vehicle Information System/
â”‚
â”œâ”€â”€ pom.xml
â”œâ”€â”€ README.md
â”‚
â”œâ”€â”€ docs/
â”‚   â”œâ”€â”€ PROJECT_PLAN.md
â”‚   â”œâ”€â”€ USER_GUIDE.md
â”‚   â”œâ”€â”€ TECHNICAL_DESIGN.md
â”‚   â”œâ”€â”€ TEST_PLAN.md
â”‚   â””â”€â”€ ROADMAP.md
â”‚
â”œâ”€â”€ screenshots/
â”œâ”€â”€ backups/
â”‚
â””â”€â”€ src/main/java/com/femzyk/vehiclesystem/
    â”‚
    â”œâ”€â”€ VehicleRentalSystem.java
    â”‚
    â”œâ”€â”€ exception/
    â”‚   â”œâ”€â”€ VehicleException.java
    â”‚   â”œâ”€â”€ InvalidCargoException.java
    â”‚   â”œâ”€â”€ InvalidDoorsException.java
    â”‚   â”œâ”€â”€ InvalidFuelTypeException.java
    â”‚   â”œâ”€â”€ InvalidTransmissionException.java
    â”‚   â”œâ”€â”€ InvalidWheelsException.java
    â”‚   â””â”€â”€ InvalidYearException.java
    â”‚
    â”œâ”€â”€ interfaces/
    â”‚   â”œâ”€â”€ Vehicle.java
    â”‚   â”œâ”€â”€ CarVehicle.java
    â”‚   â”œâ”€â”€ MotorVehicle.java
    â”‚   â””â”€â”€ TruckVehicle.java
    â”‚
    â”œâ”€â”€ model/
    â”‚   â”œâ”€â”€ Car.java
    â”‚   â”œâ”€â”€ Motorcycle.java
    â”‚   â””â”€â”€ Truck.java
    â”‚
    â””â”€â”€ gui/
        â”œâ”€â”€ MainWindow.java
        â”œâ”€â”€ LoginDialog.java
        â”œâ”€â”€ PasswordResetDialog.java
        â”œâ”€â”€ AddCarDialog.java
        â”œâ”€â”€ AddMotorcycleDialog.java
        â”œâ”€â”€ AddTruckDialog.java
        â”œâ”€â”€ FleetPanel.java
        â”œâ”€â”€ VehicleCardPanel.java
        â”œâ”€â”€ StatsPanel.java
        â”œâ”€â”€ RecycleBinDialog.java
        â”œâ”€â”€ ThemeConstants.java
        â”œâ”€â”€ UserAccount.java
        â”œâ”€â”€ UserDatabase.java
        â”œâ”€â”€ PasswordUtil.java
        â”œâ”€â”€ StorageManager.java
        â””â”€â”€ StoredFleetData.java
```

---

## ðŸš€ Getting Started

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

## ðŸ’» Build and Run

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

## ðŸ“– Usage Guide

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

## ðŸš˜ Vehicle Types

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

## ðŸ—‘ï¸ Recycle Bin Explained

| Action | Result |
|---|---|
| Delete vehicle | Moves vehicle to Recycle Bin |
| Recover | Restores vehicle to active fleet |
| Delete Permanently | Removes vehicle forever |
| Clear Bin | Permanently deletes all recycled vehicles |

---

## ðŸ’¾ Storage Location

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

## ðŸ§ª Testing

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

## ðŸ§  OOP Concepts Demonstrated

| Concept | How It Appears |
|---|---|
| Encapsulation | Private fields with getters/setters |
| Abstraction | Vehicle interface |
| Polymorphism | List of Vehicle objects stores all vehicle types |
| Interface Segregation | CarVehicle, MotorVehicle, TruckVehicle |
| Separation of Concerns | GUI, model, storage, auth, and exception classes separated |
| Exception Handling | Custom validation exceptions |

---

## ðŸ—ºï¸ Roadmap

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

## ðŸŒ Future Web and Mobile Direction

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

## ðŸ‘¨â€ðŸ’» Author

**Olufemi Keripe**  
**Brand:** FEMZYK ENTERPRISES LTD  
**Project:** CS 1102-01 Unit 6 Vehicle Information System  

- ðŸŒ Nigeria
- ðŸ’¼ AI Powered Full-Stack Developer
- ðŸ™ GitHub: https://github.com/FEMZYKENTLTD
- ðŸ“§ Email: femzykenterprises@gmail.com

---

## ðŸ™ Acknowledgments

- University of the People
- CS 1102-01 Programming Course
- Java and Maven ecosystem
- Java Swing framework
- FEMZYK ENTERPRISES LTD

---

<div align="center">

**Built with dedication using Java 17, Swing, Maven, and OOP principles.**

Desktop Today Â· Web Portal Next Â· Mobile/PWA Future

</div>