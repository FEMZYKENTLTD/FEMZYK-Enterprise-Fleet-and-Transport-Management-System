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