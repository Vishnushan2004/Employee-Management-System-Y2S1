
# Backend compile
cd D:\EMS_Project_2026-Y2-S1-NU-07\project\backend
javac -cp "lib\mssql-jdbc-13.4.0.jre11.jar" -d out (Get-ChildItem -Recurse -Filter *.java src).FullName

# Backend run
java -cp "out;lib\mssql-jdbc-13.4.0.jre11.jar" com.sliit.ems.Main


# Frontend run

npm install
npm run build

# Frontend run

#for start Front End: npm run dev

# red Error
Ctrl + shift + P to solve red code errors



# Web-Based Employee Management System

**Group ID:** 2026-Y2-S1-NU-07 (SLIIT — SE2030 / IT2140)

A locally-run employee management system built with React (frontend),
Java SE (backend, plain `com.sun.net.httpserver`, no framework), and
Microsoft SQL Server (database).

## 1. Prerequisites

- **Java JDK 21** (or newer) — `java -version` should print 21+
- **Node.js 18+** and npm — for the React frontend
- **Microsoft SQL Server** (Developer/Express edition works fine) running locally
- **SQL Server Management Studio (SSMS)** — to run the schema/seed scripts
- **VS Code** (or any editor) for development

## 2. Database Setup

1. Open SSMS and connect to your local SQL Server instance.
2. Open `database/schema.sql` and execute it. This creates the
   `EmployeeManagementSystem` database and all tables, constraints, and indexes.
3. Open `database/seed.sql` and execute it against the same database.
   This inserts sample data for every module and 6 demo login accounts.

See `docs/database-design.md` for notes on how the schema maps to the
submitted EER diagram, including the 3 entities added after review
(`LEAVE_REQUEST`, `INTERVIEW`, `ALLOWANCE`) and the `EMPLOYEE_TRAINING`
junction table that resolves the Employee↔Training M:N relationship.

## 3. Backend Setup (Java SE)

### 3.1 Download the SQL Server JDBC driver

The backend needs the Microsoft JDBC Driver for SQL Server (not bundled
in this repo — it's a ~1.5MB jar, download once per machine):

1. Go to https://learn.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server
2. Download the latest `.jre11` (or newer) jar, e.g. `mssql-jdbc-12.8.1.jre11.jar`
3. Place it in `backend/lib/`

> Password hashing (BCrypt) does **not** need a separate download — the
> project vendors a small single-file, ISC-licensed implementation at
> `backend/src/main/java/org/mindrot/jbcrypt/BCrypt.java` so there's no
> extra jar to fetch just for that.

### 3.2 Configure the database connection

```bash
cd backend
cp config.properties.example config.properties
```

Edit `config.properties` and set `db.user` / `db.password` to match your
local SQL Server login. `config.properties` is git-ignored — never commit
real credentials.

### 3.3 Compile and run

From the `backend/` folder:

**Windows (PowerShell/cmd):**
```bash
javac -d out -cp "lib/*" $(Get-ChildItem -Recurse -Filter *.java src | % { $_.FullName })
java -cp "out;lib/*" com.sliit.ems.Main
```

**macOS/Linux:**
```bash
find src -name "*.java" > sources.txt
javac -d out -cp "lib/*" @sources.txt
java -cp "out:lib/*" com.sliit.ems.Main
```

If the database connects successfully you'll see:
```
Connected to SQL Server successfully.
Employee Management System backend running on http://localhost:8080
```

If it can't connect, double check SQL Server is running, `config.properties`
has the right credentials, and that TCP/IP is enabled for your SQL Server
instance (SQL Server Configuration Manager → SQL Server Network Configuration).

## 4. Frontend Setup (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

The app opens at `http://localhost:5173` and talks to the backend at
`http://localhost:8080/api` (hardcoded in `src/services/api.js` — change
the `API_BASE` constant there if you run the backend on a different port).

## 5. Demo Accounts

All accounts use the password: **`Pass@123`**

| Username | Role |
|---|---|
| nadeesha.hr | HR Manager |
| imesha.finance | Finance & Payroll Manager |
| chamara.ops | Operations Manager |
| kavindu.it | IT Coordinator |
| ravindu.emp | Employee |
| tharushi.admin | Administrative Officer |

## 6. What's implemented so far

**Working end-to-end (backend ↔ database ↔ frontend):**
- Login / logout with BCrypt-verified passwords and a simple in-memory session token (30-minute inactivity timeout)
- Role-based route protection and role-aware sidebar navigation for all 6 roles
- Role-specific dashboard with real counts from SQL Server (no hardcoded numbers)
- Employee directory: search, view, add (HR/Admin), own-profile contact-detail editing (Employee)
- Attendance: check-in/check-out (working hours & late minutes calculated server-side), staff view of all records
- Leave management: apply, approve/reject (Operations/Admin), cancel — with transactional leave-balance updates and the "rejected leave never touches balance" rule enforced in the DAO
- Notifications: list + mark as read
- Audit log viewer (IT Coordinator)

**Not yet implemented (planned next, see Development Order below):**
- Shift management UI, Payroll/Payslip UI, Recruitment UI, Performance/Training/Certification UI, Promotion/Transfer UI, Roles & Permissions management UI, Reports/print/export

The database schema and DAOs already exist for all of the above modules
(see `database/schema.sql`); what's missing is the controller + frontend
layer, which follows the same pattern as Employees/Leaves/Attendance above.

## 7. Development Order (for the rest of the build)

1. Shift management (Operations)
2. Payroll + Payslip (Finance)
3. Recruitment: vacancies → applicants → applications → interviews (HR)
4. Performance, Training, Certification (HR)
5. Promotion / Transfer (HR)
6. User/Role/Permission management (IT Coordinator)
7. Reports, print, CSV export
8. UI polish, final validation pass, demo rehearsal

## 8. Project Structure

```
backend/
  src/main/java/com/sliit/ems/
    config/      DB connection + config loading
    model/       Plain data classes
    dao/         JDBC data access (PreparedStatement everywhere, no string-built SQL from user input)
    service/     Business logic (currently just AuthService; DAOs carry most logic for now)
    controller/  HTTP handlers (one per resource)
    util/        JSON helper, HTTP helper, session manager, password hashing, security/RBAC checks
  src/main/java/org/mindrot/jbcrypt/  vendored BCrypt implementation (ISC license)
  Main.java      Wires routes and starts the server
frontend/
  src/
    components/  Shared UI (StatCard, Modal, badges, etc.)
    context/     AuthContext (session state)
    layouts/     Sidebar + topbar shell
    pages/       One page per route
    services/    fetch() wrappers per API resource
    utils/       Role → navigation config
database/
  schema.sql     Full DDL
  seed.sql       Sample data + 6 demo accounts
docs/
  database-design.md   EER-to-schema mapping and design decisions
```

## 9. Troubleshooting

- **"Could not connect to SQL Server"** — check `config.properties`, confirm
  SQL Server is running and TCP/IP is enabled, and that `db.trustServerCertificate=true`
  is set if you're using a local self-signed setup.
- **Login always fails** — confirm you ran `seed.sql` (it contains the working
  demo password hashes) and that you're using `Pass@123` exactly.
- **CORS errors in the browser console** — make sure the backend is running
  on port 8080; the backend sends permissive CORS headers for local dev only.
- **"Microsoft JDBC Driver not found on classpath"** — you're missing the
  jar in `backend/lib/`, see section 3.1.
- **Frontend shows "Failed to fetch"** — the backend isn't running, or it's
  running on a different port than `API_BASE` in `src/services/api.js`.
