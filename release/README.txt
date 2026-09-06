FEMZYK Enterprise Fleet & Transport Management System — release 2.0.0 (2026-09-06)

FEMZYK-Fleet-Management.jar   Runnable application (Java 17+). Run:  java -jar FEMZYK-Fleet-Management.jar
database/fleet-demo.db        Ready-made database with demo data (login admin / Admin@2024, then set a new password).
                              To use it: copy to ~/FemzykFleetManagement/fleet.db, or run with
                              java -jar FEMZYK-Fleet-Management.jar --data <folder containing fleet.db>
database/schema.sql           The schema the application creates on first run.
sample-data/                  CSV import templates (header only) and demo exports for employees, vehicles, drivers.
documentation/                Full documentation set (same as docs/ in the repository).
screenshots/                  Application screens rendered from version 2.0.0.

Automated tests at build time: 49 passed, 0 failed (mvn clean package).
