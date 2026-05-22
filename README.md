
```markdown
# Femzyk Car Rental Agency - Vehicle Management System v4.0+

A premium Java Swing desktop application for managing a vehicle rental fleet, featuring **multi-user authentication**, **secure password hashing**, **email-based account recovery**, **vehicle CRUD operations**, **optional renter tracking**, **auto-save**, **user-specific profiles**, and a **Recycle Bin** for safe deletion.

![Java](https://img.shields.io/badge/Made%20with-Java%2017-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/GUI-Java%20Swing-2563EB?style=for-the-badge)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Storage](https://img.shields.io/badge/Storage-Local%20DAT%20Files-16A34A?style=for-the-badge)
![Platform](https://img.shields.io/badge/Desktop-Windows%20%7C%20Java%20Runtime-blue?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Desktop%20Stable-success?style=for-the-badge)
![Future](https://img.shields.io/badge/Future-Web%20%7C%20Mobile%20PWA-purple?style=for-the-badge)

---

## Screenshots

Screenshots of the application interface are stored in the `screenshots/` folder.

Suggested screenshots include:

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

## About The Project

**Femzyk Car Rental Agency - Vehicle Management System** is a professional desktop fleet management application built with **Java 17**, **Java Swing**, and **Maven**.

The system is designed for a rental agency that manages multiple vehicle categories:

- Cars
- Motorcycles
- Trucks

The application demonstrates modern object-oriented programming principles and desktop application design, including:

- Secure user authentication
- Email-based account recovery
- Persistent local storage
- Soft delete with Recycle Bin
- Vehicle recovery system
- User-specific profiles
- Optional renter information
- Real-time fleet statistics
- Premium dark-themed GUI
- Clean object-oriented architecture

---

## Features

### Authentication and Account Management

- User signup
- User login
- Email validation during signup
- Show/hide password option
- Forgot password / password reset
- Password reset using username and registered email
- Secure password hashing with `PBKDF2WithHmacSHA256`
- Separate saved data for each user

### Vehicle Management

- Add cars
- Add motorcycles
- Add trucks
- Edit vehicle details
- Delete vehicles safely
- View vehicles by category
- Store optional renter details
- Auto-save vehicle records

### Recycle Bin

- Deleted vehicles move to Recycle Bin
- Recover deleted vehicles
- Permanently delete selected vehicles
- Clear entire Recycle Bin
- Recycle Bin count shown in statistics

### Persistent Storage

The application saves data automatically using local `.dat` files.

Saved data includes:

```text
Registered users
Password hashes and salts
User-specific active fleet
User-specific recycle bin
Last saved date/time
```

### Fleet Statistics

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

### UI/UX Design

- Professional dark theme
- Vehicle-type color coding
- Card-based fleet display
- Horizontal scrolling per vehicle section
- Vertical scrolling for full dashboard
- Touchpad-friendly scrolling
- Styled toolbar buttons
- User-friendly validation messages

---

## Tech Stack

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

## Project Structure

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

## Getting Started

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

## Build and Run

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

## Usage Guide

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

## Vehicle Types

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

## Recycle Bin Explained

| Action | Result |
|---|---|
| Delete vehicle | Moves vehicle to Recycle Bin |
| Recover | Restores vehicle to active fleet |
| Delete Permanently | Removes vehicle forever |
| Clear Bin | Permanently deletes all recycled vehicles |

---

## Storage Location

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

## Testing

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

## OOP Concepts Demonstrated

| Concept | How It Appears |
|---|---|
| Encapsulation | Private fields with getters/setters |
| Abstraction | Vehicle interface |
| Polymorphism | List of Vehicle objects stores all vehicle types |
| Interface Segregation | CarVehicle, MotorVehicle, TruckVehicle |
| Separation of Concerns | GUI, model, storage, auth, and exception classes separated |
| Exception Handling | Custom validation exceptions |

---

## Roadmap

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

## Future Web and Mobile Direction

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

## Author

**Olufemi Keripe**  
**Brand:** FEMZYK ENTERPRISES LTD  
**Project:** CS 1102-01 Unit 6 Vehicle Information System  

- Nigeria
- AI Powered Full-Stack Developer
- GitHub: https://github.com/FEMZYKENTLTD
- Email: femzykenterprises@gmail.com

---

## Acknowledgments

- University of the People
- CS 1102-01 Programming Course
- Java and Maven ecosystem
- Java Swing framework
- FEMZYK ENTERPRISES LTD

---

<div align="center">

**Built with dedication using Java 17, Swing, Maven, and OOP principles.**

Desktop Today - Web Portal Next - Mobile/PWA Future

</div>
```
