# Test Plan

## 1. Strategy

| Level | Tooling | Scope |
|---|---|---|
| Unit / integration (automated) | JUnit 5.10, in-memory SQLite, `mvn test` | Every service rule, repositories through the services, security, backup/restore on a temp file, CSV parser and import, legacy migration with real v4 files, report/PDF generation, headless rendering of all 13 UI panels |
| Build verification | `mvn clean package`, `java -jar … --headless-check --demo` | Jar builds, DB opens, schema applies, integrity check passes, demo data loads once |
| Manual UI walkthrough | Desktop with display | Dialogs, navigation, role visibility (documented as manual because no display exists in the build sandbox) |

Tests run on a fresh in-memory database per test method (`TestSupport.freshRegistry()`), so they are order-independent and take < 10 s in total.

## 2. Automated test cases

| ID | Class · method | Verifies |
|---|---|---|
| T-01 | `SmokeTest.bootstrapDemoDataAndReportsEndToEnd` | bootstrap → demo load (idempotent) → dashboard values → all 10 reports × 3 formats export |
| T-02 | `AuthServiceTest.defaultAdminIsCreatedOnceAndMustChangePassword` | seeding idempotent, hash ≠ plaintext, forced change |
| T-03 | `AuthServiceTest.wrongPasswordIsRejectedAndLocksAfterLimit` | 5 failures → locked message |
| T-04 | `AuthServiceTest.unknownUserGetsGenericMessage` | no username enumeration |
| T-05 | `AuthServiceTest.passwordPolicyEnforced` | length + letter/digit policy |
| T-06 | `AuthServiceTest.viewerCannotManageRecords` | RBAC in service layer |
| T-07 | `AuthServiceTest.lastAdministratorCannotBeDeletedOrDemoted` | lock-out prevention |
| T-08 | `AuthServiceTest.changeOwnPasswordClearsForcedFlagAndOldPasswordStopsWorking` | password change |
| T-09 | `AuthServiceTest.loginsAreAudited` | audit rows for AUTH |
| T-10 | `EmployeeAndDriverServiceTest.createAssignsSequentialCodes` | EMP-0001… |
| T-11 | `…validationCollectsAllErrors` | multiple errors returned together |
| T-12 | `…duplicateEmailRejected` | unique e-mail |
| T-13 | `…searchAndFilter` | text / department / status filters |
| T-14 | `…softDeleteRestoreAndPermanentDelete` | recycle bin lifecycle |
| T-15 | `…driverRequiresEmployeeAndUniqueLicence` | 1:1 employee↔driver, unique licence, code DRV-0001 |
| T-16 | `…terminatingEmployeeDeactivatesDriver` | cascade rule |
| T-17 | `…licenceAlertsListExpiringAndExpired` | 30-day alert, expired licence rejected on create |
| T-18 | `VehicleAndAssignmentServiceTest.polymorphicVehiclesRoundTrip` | Car/Truck persisted and re-hydrated as the right class |
| T-19 | `…registrationIsUniqueAndNormalised` | plate upper-cased, duplicate rejected |
| T-20 | `…statusStateMachine` | illegal transitions, RETIRED final, audit rows |
| T-21 | `…mileageCannotDecrease` | odometer rule |
| T-22 | `…assignmentRulesAndHistory` | licence class, double-assignment both sides, delete blocked, return, history |
| T-23 | `…suspendedDriverOrExpiredLicenceCannotBeAssigned` | driver eligibility |
| T-24 | `…countsByStatusAndType` | aggregate queries |
| T-25 | `TripServiceTest.lifecyclePlannedActiveCompletedUpdatesOdometer` | full trip lifecycle, mileage update, immutability |
| T-26 | `…assignedVehicleReturnsToAssignedAfterTrip` | status restore |
| T-27 | `…vehicleCannotBeOnTwoActiveTrips` | conflict |
| T-28 | `…cancelReleasesVehicleAndPlannedCanBeDeleted` | cancel/delete |
| T-29 | `…startMileageBelowOdometerRejected` | validation |
| T-30 | `…vehicleInMaintenanceCannotStartTrip` | availability |
| T-31 | `MaintenanceFuelExpenseServiceTest.completedMaintenancePostsLinkedExpenseAndSyncsOnUpdateAndDelete` | derived expense lifecycle |
| T-32 | `…inProgressMaintenanceMovesVehicleAndCompletionReleasesIt` | vehicle status coupling |
| T-33 | `…dueAndOverdueDetection` | by date and by mileage |
| T-34 | `…fuelComputesTotalPostsExpenseAndUpdatesMileage` | fuel rules |
| T-35 | `…fuelEfficiencyNeedsTwoOdometerReadings` | km/L maths, no invented figures |
| T-36 | `…expenseTotalsByCategoryAndMonth` | aggregates |
| T-37 | `DashboardAndReportTest.emptyDatabaseGivesZeroSnapshotNotErrors` | empty DB safe |
| T-38 | `…snapshotReflectsDemoData` | KPIs equal SQL counts |
| T-39 | `…reportsContainRealRowsAndPdfIsWellFormed` | report rows, PDF header/trailer |
| T-40 | `PasswordUtilTest.hashVerifiesAndSaltsDiffer` | PBKDF2 |
| T-41 | `PasswordUtilTest.legacyHashFromV4IsStillVerifiable` | backwards-compatible hashing |
| T-42 | `PasswordUtilTest.policy` | policy strings |
| T-43 | `BackupAndCsvTest.backupIsVerifiedAndRestoreBringsDataBack` | backup + restore on file DB, safety copy |
| T-44 | `…invalidFileIsRejectedForRestore` | validation |
| T-45 | `…csvParserHandlesQuotesCommasAndNewlines` | RFC-4180 cases |
| T-46 | `…exportThenImportRoundTripWithRowLevelErrors` | template, import with rejects, export |
| T-47 | `LegacyDataMigratorTest.scanFindsBothFiles` | detection |
| T-48 | `…migratesUsersAndVehiclesNonDestructivelyAndIdempotently` | real v4 `.dat` → users (password works) + vehicles + recycle bin; files untouched; second run no-op |
| T-49 | `HeadlessRenderTest.allModulePanelsRenderOffscreen` | all 13 panels construct, refresh and paint; screenshots saved |

## 3. Manual test checklist (requires a display)

| ID | Steps | Expected |
|---|---|---|
| M-01 | Start jar, sign in as admin | Forced password dialog, then main window with sidebar |
| M-02 | Sign in as a Viewer | Only Dashboard, records, Reports visible; no Add/Edit buttons |
| M-03 | Add employee with bad e-mail and empty name | One dialog listing all problems |
| M-04 | Assign vehicle to class-B driver for a truck | "Licence class B … does not permit" message |
| M-05 | Start trip, try Change status on that vehicle | "on an active trip" message |
| M-06 | Complete trip with end < start | Validation message |
| M-07 | Export Fleet report as PDF, open in a PDF viewer | Multi-page table renders |
| M-08 | Create backup, delete a vehicle, restore backup | Vehicle back after re-login |
| M-09 | Resize window to 1024×680 | No clipped controls; dashboard scrolls vertically only |
| M-10 | Load demo data twice | Second attempt says already loaded |

## 4. Exit criteria

`mvn clean package` green (0 failures/errors), headless check prints `Integrity check: ok`, all M-xx executed on at least one desktop before release (status recorded in TEST-RESULTS.md).
