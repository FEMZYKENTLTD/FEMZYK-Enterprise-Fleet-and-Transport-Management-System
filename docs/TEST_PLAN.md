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