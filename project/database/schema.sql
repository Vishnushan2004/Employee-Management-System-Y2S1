/* ============================================================
   Web-Based Employee Management System
   Group ID: 2026-Y2-S1-NU-07
   Database Schema (Microsoft SQL Server)

   This schema implements the EER diagram submitted for IT2140,
   plus three additions agreed with the group after review:
     - LEAVE_REQUEST   (Leave Management was required but missing
                         from the submitted EER)
     - INTERVIEW       (Recruitment interviews, 1:N from APPLICATION)
     - ALLOWANCE       (Payroll allowances, mirrors DEDUCTION)

   Run this whole script in SSMS against a fresh/target database.
   ============================================================ */

IF DB_ID('EmployeeManagementSystem') IS NULL
BEGIN
    CREATE DATABASE EmployeeManagementSystem;
END
GO

USE EmployeeManagementSystem;
GO

/* ------------------------------------------------------------
   Drop tables if re-running (children first)
   ------------------------------------------------------------ */
IF OBJECT_ID('dbo.AUDIT_LOG', 'U') IS NOT NULL DROP TABLE dbo.AUDIT_LOG;
IF OBJECT_ID('dbo.NOTIFICATION', 'U') IS NOT NULL DROP TABLE dbo.NOTIFICATION;
IF OBJECT_ID('dbo.ROLE_PERMISSION', 'U') IS NOT NULL DROP TABLE dbo.ROLE_PERMISSION;
IF OBJECT_ID('dbo.PERMISSION', 'U') IS NOT NULL DROP TABLE dbo.PERMISSION;
IF OBJECT_ID('dbo.USER_ACCOUNT', 'U') IS NOT NULL DROP TABLE dbo.USER_ACCOUNT;
IF OBJECT_ID('dbo.ROLE', 'U') IS NOT NULL DROP TABLE dbo.ROLE;
IF OBJECT_ID('dbo.PAYSLIP', 'U') IS NOT NULL DROP TABLE dbo.PAYSLIP;
IF OBJECT_ID('dbo.DEDUCTION', 'U') IS NOT NULL DROP TABLE dbo.DEDUCTION;
IF OBJECT_ID('dbo.ALLOWANCE', 'U') IS NOT NULL DROP TABLE dbo.ALLOWANCE;
IF OBJECT_ID('dbo.PAYROLL', 'U') IS NOT NULL DROP TABLE dbo.PAYROLL;
IF OBJECT_ID('dbo.TRANSFER', 'U') IS NOT NULL DROP TABLE dbo.TRANSFER;
IF OBJECT_ID('dbo.PROMOTION', 'U') IS NOT NULL DROP TABLE dbo.PROMOTION;
IF OBJECT_ID('dbo.CERTIFICATION', 'U') IS NOT NULL DROP TABLE dbo.CERTIFICATION;
IF OBJECT_ID('dbo.EMPLOYEE_TRAINING', 'U') IS NOT NULL DROP TABLE dbo.EMPLOYEE_TRAINING;
IF OBJECT_ID('dbo.TRAINING', 'U') IS NOT NULL DROP TABLE dbo.TRAINING;
IF OBJECT_ID('dbo.PERFORMANCE_REVIEW', 'U') IS NOT NULL DROP TABLE dbo.PERFORMANCE_REVIEW;
IF OBJECT_ID('dbo.PERFORMANCE_GOAL', 'U') IS NOT NULL DROP TABLE dbo.PERFORMANCE_GOAL;
IF OBJECT_ID('dbo.INTERVIEW', 'U') IS NOT NULL DROP TABLE dbo.INTERVIEW;
IF OBJECT_ID('dbo.APPLICATION', 'U') IS NOT NULL DROP TABLE dbo.APPLICATION;
IF OBJECT_ID('dbo.JOB_VACANCY', 'U') IS NOT NULL DROP TABLE dbo.JOB_VACANCY;
IF OBJECT_ID('dbo.APPLICANT', 'U') IS NOT NULL DROP TABLE dbo.APPLICANT;
IF OBJECT_ID('dbo.LEAVE_REQUEST', 'U') IS NOT NULL DROP TABLE dbo.LEAVE_REQUEST;
IF OBJECT_ID('dbo.ATTENDANCE', 'U') IS NOT NULL DROP TABLE dbo.ATTENDANCE;
IF OBJECT_ID('dbo.SHIFT_ASSIGNMENT', 'U') IS NOT NULL DROP TABLE dbo.SHIFT_ASSIGNMENT;
IF OBJECT_ID('dbo.SHIFT', 'U') IS NOT NULL DROP TABLE dbo.SHIFT;
IF OBJECT_ID('dbo.CONTRACT_EMPLOYEE', 'U') IS NOT NULL DROP TABLE dbo.CONTRACT_EMPLOYEE;
IF OBJECT_ID('dbo.PERMANENT_EMPLOYEE', 'U') IS NOT NULL DROP TABLE dbo.PERMANENT_EMPLOYEE;
IF OBJECT_ID('dbo.EMPLOYEE', 'U') IS NOT NULL DROP TABLE dbo.EMPLOYEE;
IF OBJECT_ID('dbo.DESIGNATION', 'U') IS NOT NULL DROP TABLE dbo.DESIGNATION;
IF OBJECT_ID('dbo.DEPARTMENT', 'U') IS NOT NULL DROP TABLE dbo.DEPARTMENT;
GO

/* ============================================================
   1. DEPARTMENT / DESIGNATION
   ============================================================ */

CREATE TABLE DEPARTMENT (
    Department_ID     INT IDENTITY(1,1) PRIMARY KEY,
    Department_Name   VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE DESIGNATION (
    Designation_ID    INT IDENTITY(1,1) PRIMARY KEY,
    Designation_Name  VARCHAR(100) NOT NULL UNIQUE
);
GO

/* ============================================================
   2. EMPLOYEE  (composite Name / Address flattened into columns;
      Age is a derived attribute -> NOT stored, computed in views/app)
   ============================================================ */

CREATE TABLE EMPLOYEE (
    Employee_ID             INT IDENTITY(1,1) PRIMARY KEY,
    Employee_Number         VARCHAR(20) NOT NULL UNIQUE,

    -- composite Name
    First_Name              VARCHAR(50) NOT NULL,
    Middle_Name             VARCHAR(50) NULL,
    Last_Name                VARCHAR(50) NOT NULL,

    Date_of_Birth           DATE NOT NULL,
    Gender                  VARCHAR(10) NOT NULL
        CHECK (Gender IN ('MALE','FEMALE','OTHER')),
    Phone_Number            VARCHAR(20) NOT NULL,

    -- composite Address
    Street                  VARCHAR(100) NULL,
    City                    VARCHAR(50) NULL,
    Postal_Code             VARCHAR(15) NULL,

    Department_ID           INT NOT NULL,
    Designation_ID          INT NOT NULL,

    Employment_Type         VARCHAR(20) NOT NULL
        CHECK (Employment_Type IN ('PERMANENT','CONTRACT')),
    Employment_Status       VARCHAR(20) NOT NULL
        CHECK (Employment_Status IN ('ACTIVE','ON_LEAVE','SUSPENDED','RESIGNED','TERMINATED'))
        DEFAULT 'ACTIVE',
    Employment_Start_Date   DATE NOT NULL,
    Work_Location           VARCHAR(100) NULL,

    -- recursive Supervises relationship (0..1 supervisor, N subordinates)
    Supervisor_Employee_ID  INT NULL,

    -- simple running leave-balance columns (avoids a separate balance entity)
    Annual_Leave_Balance    DECIMAL(4,1) NOT NULL DEFAULT 14.0,
    Casual_Leave_Balance    DECIMAL(4,1) NOT NULL DEFAULT 7.0,
    Sick_Leave_Balance      DECIMAL(4,1) NOT NULL DEFAULT 7.0,

    CONSTRAINT FK_Employee_Department FOREIGN KEY (Department_ID) REFERENCES DEPARTMENT(Department_ID),
    CONSTRAINT FK_Employee_Designation FOREIGN KEY (Designation_ID) REFERENCES DESIGNATION(Designation_ID),
    CONSTRAINT FK_Employee_Supervisor FOREIGN KEY (Supervisor_Employee_ID) REFERENCES EMPLOYEE(Employee_ID)
);
GO

/* ISA specialization: EMPLOYEE -> PERMANENT_EMPLOYEE | CONTRACT_EMPLOYEE
   Disjoint, total participation: every employee is exactly one of the two.
   Enforced at the application/service layer using Employment_Type, and by
   each subclass table holding Employee_ID as both PK and FK (1:1 with EMPLOYEE). */

CREATE TABLE PERMANENT_EMPLOYEE (
    Employee_ID              INT PRIMARY KEY,
    Permanent_Employee_Salary DECIMAL(12,2) NOT NULL CHECK (Permanent_Employee_Salary >= 0),
    Benefits_Eligibility      BIT NOT NULL DEFAULT 1,
    CONSTRAINT FK_PermEmp_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID) ON DELETE CASCADE
);

CREATE TABLE CONTRACT_EMPLOYEE (
    Employee_ID        INT PRIMARY KEY,
    Contract_Rate       DECIMAL(12,2) NOT NULL CHECK (Contract_Rate >= 0),
    Contract_End_Date   DATE NOT NULL,
    CONSTRAINT FK_ContractEmp_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID) ON DELETE CASCADE
);
GO

/* ============================================================
   3. SHIFT / SHIFT_ASSIGNMENT  (M:N Employee<->Shift)
   ============================================================ */

CREATE TABLE SHIFT (
    Shift_ID        INT IDENTITY(1,1) PRIMARY KEY,
    Shift_Name      VARCHAR(50) NOT NULL,
    Shift_Type      VARCHAR(20) NOT NULL
        CHECK (Shift_Type IN ('MORNING','EVENING','NIGHT','GENERAL')),
    Start_Time      TIME NOT NULL,
    End_Time        TIME NOT NULL,
    Working_Hours   DECIMAL(4,2) NOT NULL CHECK (Working_Hours > 0)
);

CREATE TABLE SHIFT_ASSIGNMENT (
    Assignment_ID         INT IDENTITY(1,1) PRIMARY KEY,
    Employee_ID           INT NOT NULL,
    Shift_ID              INT NOT NULL,
    Assignment_Start_Date DATE NOT NULL,
    Assignment_End_Date   DATE NULL,
    CONSTRAINT FK_ShiftAssign_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID),
    CONSTRAINT FK_ShiftAssign_Shift FOREIGN KEY (Shift_ID) REFERENCES SHIFT(Shift_ID),
    CONSTRAINT CK_ShiftAssign_Dates CHECK (Assignment_End_Date IS NULL OR Assignment_End_Date >= Assignment_Start_Date)
);
GO

/* ============================================================
   4. ATTENDANCE
      Working_Hours / Late_Minutes / Overtime_Hours are calculated
      by the service layer at check-out time, not entered by hand.
   ============================================================ */

CREATE TABLE ATTENDANCE (
    Attendance_ID      INT IDENTITY(1,1) PRIMARY KEY,
    Employee_ID        INT NOT NULL,
    Attendance_Date    DATE NOT NULL,
    Check_In_Time      DATETIME NULL,
    Check_Out_Time     DATETIME NULL,
    Late_Minutes       INT NOT NULL DEFAULT 0,
    Overtime_Hours     DECIMAL(4,2) NOT NULL DEFAULT 0,
    Working_Hours      DECIMAL(4,2) NULL,
    Attendance_Status  VARCHAR(20) NOT NULL
        CHECK (Attendance_Status IN ('PRESENT','ABSENT','LATE','ON_LEAVE','HALF_DAY'))
        DEFAULT 'PRESENT',
    CONSTRAINT FK_Attendance_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID),
    CONSTRAINT UQ_Attendance_EmployeeDate UNIQUE (Employee_ID, Attendance_Date),
    CONSTRAINT CK_Attendance_Times CHECK (Check_Out_Time IS NULL OR Check_In_Time IS NULL OR Check_Out_Time > Check_In_Time)
);
GO

/* ============================================================
   5. LEAVE_REQUEST  (added to the EER after group review;
      Leave Management was a required module with no entity)
   ============================================================ */

CREATE TABLE LEAVE_REQUEST (
    Leave_ID        INT IDENTITY(1,1) PRIMARY KEY,
    Employee_ID     INT NOT NULL,
    Leave_Type      VARCHAR(20) NOT NULL
        CHECK (Leave_Type IN ('ANNUAL','CASUAL','SICK','OTHER')),
    Start_Date      DATE NOT NULL,
    End_Date        DATE NOT NULL,
    Reason          VARCHAR(500) NULL,
    Status          VARCHAR(20) NOT NULL
        CHECK (Status IN ('PENDING','APPROVED','REJECTED','CANCELLED'))
        DEFAULT 'PENDING',
    Applied_Date    DATETIME NOT NULL DEFAULT GETDATE(),
    Approved_By     INT NULL,
    Approved_Date   DATETIME NULL,
    CONSTRAINT FK_Leave_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID),
    CONSTRAINT FK_Leave_ApprovedBy FOREIGN KEY (Approved_By) REFERENCES EMPLOYEE(Employee_ID),
    CONSTRAINT CK_Leave_Dates CHECK (End_Date >= Start_Date)
);
GO

/* ============================================================
   6. RECRUITMENT: APPLICANT -> APPLICATION -> JOB_VACANCY, + INTERVIEW
   ============================================================ */

CREATE TABLE APPLICANT (
    Applicant_ID     INT IDENTITY(1,1) PRIMARY KEY,
    Name             VARCHAR(100) NOT NULL,
    Email            VARCHAR(100) NOT NULL UNIQUE,
    Phone_Number     VARCHAR(20) NOT NULL,
    Qualifications   VARCHAR(500) NULL,
    Skills           VARCHAR(500) NULL,
    Work_Experience  VARCHAR(500) NULL
);

CREATE TABLE JOB_VACANCY (
    Vacancy_ID              INT IDENTITY(1,1) PRIMARY KEY,
    Job_Title               VARCHAR(100) NOT NULL,
    Job_Description         VARCHAR(1000) NULL,
    Required_Qualifications VARCHAR(500) NULL,
    Required_Skills         VARCHAR(500) NULL,
    Opening_Date            DATE NOT NULL,
    Closing_Date            DATE NOT NULL,
    Vacancy_Status          VARCHAR(20) NOT NULL
        CHECK (Vacancy_Status IN ('OPEN','CLOSED','FILLED'))
        DEFAULT 'OPEN',
    CONSTRAINT CK_Vacancy_Dates CHECK (Closing_Date >= Opening_Date)
);

CREATE TABLE APPLICATION (
    Application_ID     INT IDENTITY(1,1) PRIMARY KEY,
    Applicant_ID       INT NOT NULL,
    Vacancy_ID         INT NOT NULL,
    Application_Date   DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    Application_Status VARCHAR(20) NOT NULL
        CHECK (Application_Status IN ('SUBMITTED','SHORTLISTED','INTERVIEWED','OFFERED','REJECTED','HIRED'))
        DEFAULT 'SUBMITTED',
    CV_Resume          VARCHAR(255) NULL, -- generated server-side filename only (e.g. "<uuid>.pdf"); actual file bytes live on disk under backend/uploads/cv/, never in the DB - see docs/database-design.md
    Cover_Letter       VARCHAR(1000) NULL,
    Expected_Salary    DECIMAL(12,2) NULL CHECK (Expected_Salary IS NULL OR Expected_Salary >= 0),
    CONSTRAINT FK_Application_Applicant FOREIGN KEY (Applicant_ID) REFERENCES APPLICANT(Applicant_ID),
    CONSTRAINT FK_Application_Vacancy FOREIGN KEY (Vacancy_ID) REFERENCES JOB_VACANCY(Vacancy_ID)
);

CREATE TABLE INTERVIEW (
    Interview_ID    INT IDENTITY(1,1) PRIMARY KEY,
    Application_ID  INT NOT NULL,
    Interview_Date  DATE NOT NULL,
    Interview_Time  TIME NOT NULL,
    Location        VARCHAR(100) NULL,
    Result          VARCHAR(20) NULL
        CHECK (Result IS NULL OR Result IN ('PASSED','FAILED','PENDING')),
    Remarks         VARCHAR(500) NULL,
    CONSTRAINT FK_Interview_Application FOREIGN KEY (Application_ID) REFERENCES APPLICATION(Application_ID)
);
GO

/* ============================================================
   7. PERFORMANCE
   ============================================================ */

CREATE TABLE PERFORMANCE_GOAL (
    Goal_ID       INT IDENTITY(1,1) PRIMARY KEY,
    Employee_ID   INT NOT NULL,
    Goal_Title    VARCHAR(100) NOT NULL,
    Description   VARCHAR(500) NULL,
    Target_Value  VARCHAR(100) NULL,
    Progress      DECIMAL(5,2) NOT NULL DEFAULT 0 CHECK (Progress BETWEEN 0 AND 100),
    Status        VARCHAR(20) NOT NULL
        CHECK (Status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED','CANCELLED'))
        DEFAULT 'NOT_STARTED',
    CONSTRAINT FK_Goal_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID)
);

CREATE TABLE PERFORMANCE_REVIEW (
    Review_ID          INT IDENTITY(1,1) PRIMARY KEY,
    Employee_ID        INT NOT NULL,
    Review_Date        DATE NOT NULL,
    Review_Period      VARCHAR(50) NOT NULL,
    Performance_Score  DECIMAL(4,2) NULL CHECK (Performance_Score IS NULL OR Performance_Score BETWEEN 0 AND 5),
    Feedback           VARCHAR(1000) NULL,
    CONSTRAINT FK_Review_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID)
);
GO

/* ============================================================
   8. TRAINING / CERTIFICATION
   ============================================================ */

CREATE TABLE TRAINING (
    Training_ID    INT IDENTITY(1,1) PRIMARY KEY,
    Training_Title VARCHAR(100) NOT NULL,
    Provider       VARCHAR(100) NULL,
    Start_Date     DATE NOT NULL,
    End_Date       DATE NULL,
    Training_Type  VARCHAR(50) NULL,
    CONSTRAINT CK_Training_Dates CHECK (End_Date IS NULL OR End_Date >= Start_Date)
);

/* Resolves the EER's M:N "attends" relationship between EMPLOYEE and TRAINING.
   One training program can be attended by many employees, and one employee
   can attend many training programs. */
CREATE TABLE EMPLOYEE_TRAINING (
    Employee_ID       INT NOT NULL,
    Training_ID       INT NOT NULL,
    Completion_Status VARCHAR(20) NOT NULL
        CHECK (Completion_Status IN ('ASSIGNED','IN_PROGRESS','COMPLETED','NOT_COMPLETED'))
        DEFAULT 'ASSIGNED',
    PRIMARY KEY (Employee_ID, Training_ID),
    CONSTRAINT FK_EmpTraining_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID),
    CONSTRAINT FK_EmpTraining_Training FOREIGN KEY (Training_ID) REFERENCES TRAINING(Training_ID)
);

CREATE TABLE CERTIFICATION (
    Certification_ID     INT IDENTITY(1,1) PRIMARY KEY,
    Employee_ID          INT NOT NULL,
    Certification_Name   VARCHAR(100) NOT NULL,
    Issuing_Organization VARCHAR(100) NULL,
    Issue_Date           DATE NOT NULL,
    Expiry_Date          DATE NULL,
    Status               VARCHAR(20) NOT NULL
        CHECK (Status IN ('ACTIVE','EXPIRED','REVOKED'))
        DEFAULT 'ACTIVE',
    CONSTRAINT FK_Certification_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID),
    CONSTRAINT CK_Certification_Dates CHECK (Expiry_Date IS NULL OR Expiry_Date >= Issue_Date)
);
GO

/* ============================================================
   9. PROMOTION / TRANSFER  (history preserved, one TRANSFER entity)
   ============================================================ */

CREATE TABLE PROMOTION (
    Promotion_ID          INT IDENTITY(1,1) PRIMARY KEY,
    Employee_ID           INT NOT NULL,
    Previous_Designation  VARCHAR(100) NOT NULL,
    New_Designation       VARCHAR(100) NOT NULL,
    Effective_Date        DATE NOT NULL,
    Reason                VARCHAR(500) NULL,
    CONSTRAINT FK_Promotion_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID)
);

CREATE TABLE TRANSFER (
    Transfer_ID          INT IDENTITY(1,1) PRIMARY KEY,
    Employee_ID          INT NOT NULL,
    Previous_Department  VARCHAR(100) NOT NULL,
    New_Department        VARCHAR(100) NOT NULL,
    Effective_Date        DATE NOT NULL,
    Reason                VARCHAR(500) NULL,
    CONSTRAINT FK_Transfer_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID)
);
GO

/* ============================================================
   10. PAYROLL / ALLOWANCE / DEDUCTION / PAYSLIP
   ============================================================ */

CREATE TABLE PAYROLL (
    Payroll_ID      INT IDENTITY(1,1) PRIMARY KEY,
    Employee_ID     INT NOT NULL,
    Payroll_Month   INT NOT NULL CHECK (Payroll_Month BETWEEN 1 AND 12),
    Payroll_Year    INT NOT NULL CHECK (Payroll_Year BETWEEN 2000 AND 2100),
    Basic_Salary    DECIMAL(12,2) NOT NULL CHECK (Basic_Salary >= 0),
    Gross_Salary    DECIMAL(12,2) NOT NULL CHECK (Gross_Salary >= 0),
    Net_Salary      DECIMAL(12,2) NOT NULL CHECK (Net_Salary >= 0),
    Payment_Date    DATE NULL,
    Payroll_Status  VARCHAR(20) NOT NULL
        CHECK (Payroll_Status IN ('DRAFT','VERIFIED','PAID'))
        DEFAULT 'DRAFT',
    CONSTRAINT FK_Payroll_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID),
    CONSTRAINT UQ_Payroll_EmployeeMonthYear UNIQUE (Employee_ID, Payroll_Month, Payroll_Year)
);

/* Added to the EER after group review: Payroll allowances, mirrors Deduction */
CREATE TABLE ALLOWANCE (
    Allowance_ID    INT IDENTITY(1,1) PRIMARY KEY,
    Payroll_ID      INT NOT NULL,
    Allowance_Type  VARCHAR(50) NOT NULL,
    Amount          DECIMAL(12,2) NOT NULL CHECK (Amount >= 0),
    CONSTRAINT FK_Allowance_Payroll FOREIGN KEY (Payroll_ID) REFERENCES PAYROLL(Payroll_ID) ON DELETE CASCADE
);

CREATE TABLE DEDUCTION (
    Deduction_ID    INT IDENTITY(1,1) PRIMARY KEY,
    Payroll_ID      INT NOT NULL,
    Deduction_Type  VARCHAR(50) NOT NULL,
    Amount          DECIMAL(12,2) NOT NULL CHECK (Amount >= 0),
    CONSTRAINT FK_Deduction_Payroll FOREIGN KEY (Payroll_ID) REFERENCES PAYROLL(Payroll_ID) ON DELETE CASCADE
);

CREATE TABLE PAYSLIP (
    Payslip_ID      INT IDENTITY(1,1) PRIMARY KEY,
    Payroll_ID      INT NOT NULL UNIQUE, -- 1:1 with PAYROLL
    Pay_Date        DATE NOT NULL,
    Payment_Status  VARCHAR(20) NOT NULL
        CHECK (Payment_Status IN ('PENDING','PAID'))
        DEFAULT 'PENDING',
    CONSTRAINT FK_Payslip_Payroll FOREIGN KEY (Payroll_ID) REFERENCES PAYROLL(Payroll_ID) ON DELETE CASCADE
);
GO

/* ============================================================
   11. USER_ACCOUNT / ROLE / PERMISSION  (RBAC)
   ============================================================ */

-- Role_Name is UNIQUE only (no CHECK list) so the IT Coordinator can add new
-- roles through the "Manage Roles" screen. The 7 starting roles (6 staff
-- roles + OUTSIDE_USER for the public job-applicant portal) are seeded
-- in seed.sql and documented in docs/database-design.md.
CREATE TABLE ROLE (
    Role_ID      INT IDENTITY(1,1) PRIMARY KEY,
    Role_Name    VARCHAR(50) NOT NULL UNIQUE,
    Description  VARCHAR(255) NULL
);

CREATE TABLE PERMISSION (
    Permission_ID    INT IDENTITY(1,1) PRIMARY KEY,
    Permission_Name  VARCHAR(100) NOT NULL,
    Module           VARCHAR(50) NOT NULL,
    Action           VARCHAR(20) NOT NULL
        CHECK (Action IN ('VIEW','CREATE','UPDATE','DELETE','APPROVE','EXPORT')),
    CONSTRAINT UQ_Permission UNIQUE (Module, Action)
);

/* ROLE M:N PERMISSION resolved as a junction table */
CREATE TABLE ROLE_PERMISSION (
    Role_ID        INT NOT NULL,
    Permission_ID  INT NOT NULL,
    PRIMARY KEY (Role_ID, Permission_ID),
    CONSTRAINT FK_RolePermission_Role FOREIGN KEY (Role_ID) REFERENCES ROLE(Role_ID),
    CONSTRAINT FK_RolePermission_Permission FOREIGN KEY (Permission_ID) REFERENCES PERMISSION(Permission_ID)
);

-- NOTE (additive to the EER, not a redesign): the EER modeled USER_ACCOUNT as
-- strictly 1:1 with EMPLOYEE (every login belongs to a staff member). The
-- outside-user/applicant portal needs USER_ACCOUNT rows for people who are
-- NOT employees (job applicants), so Employee_ID was relaxed to NULL-able and
-- a parallel nullable, unique Applicant_ID FK was added, with a CHECK
-- constraint enforcing that exactly one of the two is set per row (never both,
-- never neither). Since this schema.sql is a from-scratch DDL script run
-- fresh in SSMS (not a live migration against data), this is done directly in
-- the CREATE TABLE rather than as separate ALTER statements.
CREATE TABLE USER_ACCOUNT (
    User_ID         INT IDENTITY(1,1) PRIMARY KEY,
    Employee_ID     INT NULL UNIQUE, -- 1:1 with EMPLOYEE when this account belongs to a staff member
    Applicant_ID    INT NULL UNIQUE, -- 1:1 with APPLICANT when this account belongs to an outside/applicant user
    Username        VARCHAR(50) NOT NULL UNIQUE,
    Password_Hash   VARCHAR(255) NOT NULL,
    Role_ID         INT NOT NULL,
    Account_Status  VARCHAR(20) NOT NULL
        CHECK (Account_Status IN ('ACTIVE','LOCKED','DISABLED'))
        DEFAULT 'ACTIVE',
    Last_Login      DATETIME NULL,
    CONSTRAINT FK_UserAccount_Employee FOREIGN KEY (Employee_ID) REFERENCES EMPLOYEE(Employee_ID),
    CONSTRAINT FK_UserAccount_Applicant FOREIGN KEY (Applicant_ID) REFERENCES APPLICANT(Applicant_ID),
    CONSTRAINT FK_UserAccount_Role FOREIGN KEY (Role_ID) REFERENCES ROLE(Role_ID),
    CONSTRAINT CK_UserAccount_ExactlyOneOwner CHECK (
        (CASE WHEN Employee_ID IS NULL THEN 0 ELSE 1 END)
      + (CASE WHEN Applicant_ID IS NULL THEN 0 ELSE 1 END) = 1
    )
);
GO

/* ============================================================
   12. NOTIFICATION / AUDIT_LOG
   ============================================================ */

CREATE TABLE NOTIFICATION (
    Notification_ID    INT IDENTITY(1,1) PRIMARY KEY,
    User_ID            INT NOT NULL,
    Notification_Type  VARCHAR(50) NOT NULL,
    Message            VARCHAR(500) NOT NULL,
    Created_Date       DATETIME NOT NULL DEFAULT GETDATE(),
    Status             VARCHAR(10) NOT NULL
        CHECK (Status IN ('UNREAD','READ'))
        DEFAULT 'UNREAD',
    Read_Date          DATETIME NULL,
    CONSTRAINT FK_Notification_User FOREIGN KEY (User_ID) REFERENCES USER_ACCOUNT(User_ID)
);

CREATE TABLE AUDIT_LOG (
    Audit_Log_ID    INT IDENTITY(1,1) PRIMARY KEY,
    User_ID         INT NOT NULL,
    Action_Type     VARCHAR(50) NOT NULL,
    Module          VARCHAR(50) NOT NULL,
    Action_Date     DATETIME NOT NULL DEFAULT GETDATE(),
    Action_Details  VARCHAR(1000) NULL,
    CONSTRAINT FK_AuditLog_User FOREIGN KEY (User_ID) REFERENCES USER_ACCOUNT(User_ID)
);
GO

/* ============================================================
   Helpful indexes for search/filter requirements
   ============================================================ */
CREATE INDEX IX_Employee_Department ON EMPLOYEE(Department_ID);
CREATE INDEX IX_Employee_Designation ON EMPLOYEE(Designation_ID);
CREATE INDEX IX_Employee_Status ON EMPLOYEE(Employment_Status);
CREATE INDEX IX_Attendance_Date ON ATTENDANCE(Attendance_Date);
CREATE INDEX IX_Leave_Status ON LEAVE_REQUEST(Status);
CREATE INDEX IX_Payroll_MonthYear ON PAYROLL(Payroll_Month, Payroll_Year);
CREATE INDEX IX_Application_Status ON APPLICATION(Application_Status);
GO

PRINT 'Schema created successfully.';
