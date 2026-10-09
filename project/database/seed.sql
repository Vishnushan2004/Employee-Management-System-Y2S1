/* ============================================================
   Web-Based Employee Management System
   Group ID: 2026-Y2-S1-NU-07
   Seed Data (Microsoft SQL Server)

   Run AFTER schema.sql, in the same database.

   Demo login credentials (documented here for the viva - NOT real
   passwords). All demo accounts use the plaintext password "Pass@123".
   The Password_Hash value used below is a REAL bcrypt hash of "Pass@123"
   (generated once with the bcrypt library, $2a$ format for jBCrypt
   compatibility on the Java backend). Do not reuse this hash or password
   for a real production system.
   ============================================================ */

USE EmployeeManagementSystem;
GO

/* ------------------------------------------------------------
   1. DEPARTMENT / DESIGNATION
   ------------------------------------------------------------ */
INSERT INTO DEPARTMENT (Department_Name) VALUES
('Human Resources'), ('Finance'), ('Operations'), ('Information Technology'), ('Administration');

INSERT INTO DESIGNATION (Designation_Name) VALUES
('HR Manager'), ('Finance & Payroll Manager'), ('Operations Manager'),
('IT Coordinator'), ('Administrative Officer'), ('Software Engineer'),
('HR Executive'), ('Accountant'), ('Machine Operator'), ('Office Assistant');
GO

/* ------------------------------------------------------------
   2. EMPLOYEE
   Only the 6 required staff/stakeholders
   ------------------------------------------------------------ */

INSERT INTO EMPLOYEE
(
    Employee_Number,
    First_Name,
    Middle_Name,
    Last_Name,
    Date_of_Birth,
    Gender,
    Phone_Number,
    Street,
    City,
    Postal_Code,
    Department_ID,
    Designation_ID,
    Employment_Type,
    Employment_Status,
    Employment_Start_Date,
    Work_Location,
    Supervisor_Employee_ID
)
VALUES
('EMP-001','Nadeesha',NULL,'Perera','1985-03-12','FEMALE',
 '0771234501','12 Galle Rd','Colombo','00300',
 1,1,'PERMANENT','ACTIVE','2015-01-05','Head Office',NULL),

('EMP-002','Imesha',NULL,'Wickramasinghe','1987-07-22','FEMALE',
 '0771234502','45 Kandy Rd','Kandy','20000',
 2,2,'PERMANENT','ACTIVE','2016-02-10','Head Office',NULL),

('EMP-003','Chamara',NULL,'Wijesinghe','1983-11-02','MALE',
 '0771234503','8 Negombo Rd','Negombo','11500',
 3,3,'PERMANENT','ACTIVE','2014-06-01','Head Office',NULL),

('EMP-004','Kavindu',NULL,'Senanayake','1990-05-18','MALE',
 '0771234504','19 Malabe Rd','Malabe','10115',
 4,4,'PERMANENT','ACTIVE','2018-09-15','Head Office',NULL),

('EMP-005','Ravindu',NULL,'Gunawardena','1994-09-09','MALE',
 '0771234505','3 Nugegoda Rd','Nugegoda','10250',
 3,6,'PERMANENT','ACTIVE','2021-03-01','Head Office',3),

('EMP-006','Tharushi',NULL,'Fernando','1992-01-27','FEMALE',
 '0771234506','27 Rajagiriya Rd','Rajagiriya','10107',
 5,5,'PERMANENT','ACTIVE','2019-07-20','Head Office',NULL);

GO

-- Bring leave balances in line with the leave history seeded below:
-- Employee 5 has an APPROVED 3-day annual leave (2026-09-10 to 2026-09-12) -> balance reduced.
-- Employee 10 had an approved leave that was later CANCELLED -> balance restored (stays at default).
UPDATE EMPLOYEE SET Annual_Leave_Balance = 11.0 WHERE Employee_ID = 5; -- 14.0 - 3 days
GO

INSERT INTO PERMANENT_EMPLOYEE (Employee_ID, Permanent_Employee_Salary, Benefits_Eligibility) VALUES
(1, 350000, 1), (2, 320000, 1), (3, 280000, 1), (4, 260000, 1),
(5, 120000, 1), (6, 150000, 1), (8, 95000, 1), (9, 140000, 1);

INSERT INTO CONTRACT_EMPLOYEE (Employee_ID, Contract_Rate, Contract_End_Date) VALUES
(7, 4500, '2026-12-31'),
(10, 3000, '2026-06-30');
GO

/* ------------------------------------------------------------
   3. SHIFT / SHIFT_ASSIGNMENT
   ------------------------------------------------------------ */
INSERT INTO SHIFT (Shift_Name, Shift_Type, Start_Time, End_Time, Working_Hours) VALUES
('Day Shift','MORNING','08:00','17:00',8.0),
('Evening Shift','EVENING','14:00','22:00',8.0),
('Night Shift','NIGHT','22:00','06:00',8.0),
('General Shift','GENERAL','09:00','18:00',8.0);

INSERT INTO SHIFT_ASSIGNMENT (Employee_ID, Shift_ID, Assignment_Start_Date, Assignment_End_Date) VALUES
(5, 1, '2026-01-01', NULL),
(8, 1, '2026-01-01', NULL),
(7, 4, '2026-01-01', NULL),
(10, 4, '2026-01-01', NULL);
GO

/* ------------------------------------------------------------
   4. ATTENDANCE (last few days, sample)
   ------------------------------------------------------------ */
INSERT INTO ATTENDANCE (Employee_ID, Attendance_Date, Check_In_Time, Check_Out_Time, Late_Minutes, Overtime_Hours, Working_Hours, Attendance_Status) VALUES
(5,'2026-09-01','2026-09-01 08:05','2026-09-01 17:10',5,0.0,8.08,'LATE'),
(5,'2026-09-02','2026-09-02 07:58','2026-09-02 17:00',0,0.0,8.03,'PRESENT'),
(8,'2026-09-01','2026-09-01 08:00','2026-09-01 18:30',0,1.5,9.5,'PRESENT'),
(8,'2026-09-02', NULL, NULL, 0, 0, NULL, 'ABSENT'),
(7,'2026-09-01','2026-09-01 09:10','2026-09-01 18:00',10,0.0,7.83,'LATE'),
(10,'2026-09-01','2026-09-01 09:00','2026-09-01 18:00',0,0.0,8.0,'PRESENT');
GO

/* ------------------------------------------------------------
   5. LEAVE_REQUEST
   ------------------------------------------------------------ */
INSERT INTO LEAVE_REQUEST (Employee_ID, Leave_Type, Start_Date, End_Date, Reason, Status, Approved_By, Approved_Date) VALUES
(5,'ANNUAL','2026-09-10','2026-09-12','Family event','APPROVED',3,'2026-09-03 10:00'),
(7,'SICK','2026-09-05','2026-09-05','Fever','PENDING',NULL,NULL),
(8,'CASUAL','2026-08-20','2026-08-20','Personal work','REJECTED',3,'2026-08-18 09:00'),
(10,'ANNUAL','2026-07-01','2026-07-03','Vacation','CANCELLED',6,'2026-06-25 14:00');
GO

/* ------------------------------------------------------------
   6. RECRUITMENT: JOB_VACANCY / APPLICANT / APPLICATION / INTERVIEW
   ------------------------------------------------------------ */
INSERT INTO JOB_VACANCY (Job_Title, Job_Description, Required_Qualifications, Required_Skills, Opening_Date, Closing_Date, Vacancy_Status) VALUES
('Junior Software Engineer','Develop and maintain internal tools.','BSc in IT/CS','Java, SQL','2026-08-01','2026-09-30','OPEN'),
('HR Executive','Support recruitment and employee records.','BBA in HRM','Communication, MS Office','2026-07-15','2026-08-31','CLOSED');

INSERT INTO APPLICANT (Name, Email, Phone_Number, Qualifications, Skills, Work_Experience) VALUES
('Amali Rathnayake','amali.r@example.com','0712223301','BSc IT','Java, React','1 year internship'),
('Kasun Perera','kasun.p@example.com','0712223302','BSc Computer Science','SQL, Python','No prior experience'),
('Nadeeka Silva','nadeeka.s@example.com','0712223303','BBA HRM','Communication','2 years HR assistant');

INSERT INTO APPLICATION (Applicant_ID, Vacancy_ID, Application_Date, Application_Status, CV_Resume, Cover_Letter, Expected_Salary) VALUES
(1, 1, '2026-08-05','INTERVIEWED','/cv/amali_rathnayake.pdf','Keen to grow as an engineer.',95000),
(2, 1, '2026-08-10','SUBMITTED','/cv/kasun_perera.pdf', NULL, 90000),
(3, 2, '2026-07-20','HIRED','/cv/nadeeka_silva.pdf','Excited to join HR team.',85000);

INSERT INTO INTERVIEW (Application_ID, Interview_Date, Interview_Time, Location, Result, Remarks) VALUES
(1, '2026-08-20','10:00','Head Office - Room 2','PENDING','Technical round scheduled'),
(3, '2026-07-28','14:00','Head Office - Room 1','PASSED','Strong communication skills');
GO

/* ------------------------------------------------------------
   7. PERFORMANCE
   ------------------------------------------------------------ */
INSERT INTO PERFORMANCE_GOAL (Employee_ID, Goal_Title, Description, Target_Value, Progress, Status) VALUES
(5,'Complete React Training','Finish the internal React onboarding course','100% completion',60,'IN_PROGRESS'),
(8,'Reduce Machine Downtime','Cut unplanned downtime on Line 2','Under 5% downtime',80,'IN_PROGRESS');

INSERT INTO PERFORMANCE_REVIEW (Employee_ID, Review_Date, Review_Period, Performance_Score, Feedback) VALUES
(5,'2026-06-30','H1 2026',4.2,'Consistently meets deadlines and communicates well.'),
(8,'2026-06-30','H1 2026',3.8,'Good technical work, needs to improve punctuality.');
GO

/* ------------------------------------------------------------
   8. TRAINING / CERTIFICATION
   ------------------------------------------------------------ */
INSERT INTO TRAINING (Training_Title, Provider, Start_Date, End_Date, Training_Type) VALUES
('React Fundamentals','Internal L&D Team','2026-07-01','2026-07-15','TECHNICAL'),
('Payroll Compliance Workshop','Finance Institute','2026-05-01','2026-05-03','COMPLIANCE');

-- M:N: multiple employees can attend the same training program
INSERT INTO EMPLOYEE_TRAINING (Employee_ID, Training_ID, Completion_Status) VALUES
(5, 1, 'COMPLETED'),
(8, 1, 'IN_PROGRESS'),
(9, 2, 'COMPLETED');

INSERT INTO CERTIFICATION (Employee_ID, Certification_Name, Issuing_Organization, Issue_Date, Expiry_Date, Status) VALUES
(5,'AWS Cloud Practitioner','Amazon Web Services','2025-01-15','2028-01-15','ACTIVE'),
(9,'Certified Payroll Professional','APA','2023-03-01','2026-03-01','EXPIRED');
GO

/* ------------------------------------------------------------
   9. PROMOTION / TRANSFER
   ------------------------------------------------------------ */
INSERT INTO PROMOTION (Employee_ID, Previous_Designation, New_Designation, Effective_Date, Reason) VALUES
(5,'Trainee Engineer','Software Engineer','2022-06-01','Consistent strong performance');

INSERT INTO TRANSFER (Employee_ID, Previous_Department, New_Department, Effective_Date, Reason) VALUES
(10,'Finance','Administration','2025-01-01','Departmental restructuring');
GO

/* ------------------------------------------------------------
   10. PAYROLL / ALLOWANCE / DEDUCTION / PAYSLIP
   ------------------------------------------------------------ */
INSERT INTO PAYROLL (Employee_ID, Payroll_Month, Payroll_Year, Basic_Salary, Gross_Salary, Net_Salary, Payment_Date, Payroll_Status) VALUES
(5, 8, 2026, 120000, 135000, 128000, '2026-08-31','PAID'),
(8, 8, 2026, 95000, 108500, 101500, '2026-08-31','PAID'),
(7, 8, 2026, 90000, 90000, 85000, NULL,'DRAFT');
GO

INSERT INTO ALLOWANCE (Payroll_ID, Allowance_Type, Amount) VALUES
(1,'Transport',10000), (1,'Meal',5000),
(2,'Transport',8000), (2,'Overtime',5500);

INSERT INTO DEDUCTION (Payroll_ID, Deduction_Type, Amount) VALUES
(1,'EPF Employee Share',7000),
(2,'EPF Employee Share',7000),
(3,'EPF Employee Share',5000);

INSERT INTO PAYSLIP (Payroll_ID, Pay_Date, Payment_Status) VALUES
(1,'2026-08-31','PAID'),
(2,'2026-08-31','PAID'),
(3, NULL, 'PENDING');
GO

/* ------------------------------------------------------------
   11. ROLE / PERMISSION / ROLE_PERMISSION / USER_ACCOUNT
   ------------------------------------------------------------ */
INSERT INTO ROLE (Role_Name, Description) VALUES
('EMPLOYEE','Standard employee self-service access'),
('HR_MANAGER','Manages recruitment, performance, training, promotions, transfers'),
('FINANCE_PAYROLL_MANAGER','Manages payroll processing and financial reports'),
('OPERATIONS_MANAGER','Manages attendance, shifts and leave approvals'),
('IT_COORDINATOR','Manages users, roles, permissions and audit logs'),
('ADMINISTRATIVE_OFFICER','Manages employee records and administrative reports'),
('OUTSIDE_USER','Public job-applicant portal access: own profile, open vacancies, own applications/interviews/notifications only');

INSERT INTO PERMISSION (Permission_Name, Module, Action) VALUES
('View Employees','EMPLOYEE','VIEW'), ('Manage Employees','EMPLOYEE','UPDATE'),
('View Payroll','PAYROLL','VIEW'), ('Process Payroll','PAYROLL','CREATE'), ('Verify Payroll','PAYROLL','APPROVE'),
('View Attendance','ATTENDANCE','VIEW'), ('Correct Attendance','ATTENDANCE','UPDATE'),
('Apply Leave','LEAVE','CREATE'), ('Approve Leave','LEAVE','APPROVE'),
('Manage Recruitment','RECRUITMENT','CREATE'),
('Manage Users','USER_ACCOUNT','UPDATE'), ('View Audit Logs','AUDIT_LOG','VIEW');

-- Wire a reasonable starting matrix (kept small and demonstrable)
INSERT INTO ROLE_PERMISSION (Role_ID, Permission_ID)
SELECT r.Role_ID, p.Permission_ID
FROM ROLE r
CROSS JOIN PERMISSION p
WHERE (r.Role_Name = 'EMPLOYEE' AND p.Permission_Name IN ('Apply Leave'))
   OR (r.Role_Name = 'HR_MANAGER' AND p.Permission_Name IN ('View Employees','Manage Employees','Manage Recruitment','View Attendance'))
   OR (r.Role_Name = 'FINANCE_PAYROLL_MANAGER' AND p.Permission_Name IN ('View Payroll','Process Payroll','Verify Payroll'))
   OR (r.Role_Name = 'OPERATIONS_MANAGER' AND p.Permission_Name IN ('View Attendance','Correct Attendance','Approve Leave'))
   OR (r.Role_Name = 'IT_COORDINATOR' AND p.Permission_Name IN ('Manage Users','View Audit Logs'))
   OR (r.Role_Name = 'ADMINISTRATIVE_OFFICER' AND p.Permission_Name IN ('View Employees','Correct Attendance'));
GO

-- Demo password for ALL accounts below: Pass@123
-- This is a REAL bcrypt hash of "Pass@123" (cost factor 10, $2a$ format),
-- verified to work with jBCrypt (org.mindrot.jbcrypt.BCrypt) on the backend.
INSERT INTO USER_ACCOUNT (Employee_ID, Username, Password_Hash, Role_ID, Account_Status) VALUES
(1,'nadeesha.hr','$2a$10$.DgsHI4JH48SnlFM0Th96euDYMP1dbU5Y/QsIddlyWtUut0R6aJTm', 2, 'ACTIVE'),
(2,'imesha.finance','$2a$10$.DgsHI4JH48SnlFM0Th96euDYMP1dbU5Y/QsIddlyWtUut0R6aJTm', 3, 'ACTIVE'),
(3,'chamara.ops','$2a$10$.DgsHI4JH48SnlFM0Th96euDYMP1dbU5Y/QsIddlyWtUut0R6aJTm', 4, 'ACTIVE'),
(4,'kavindu.it','$2a$10$.DgsHI4JH48SnlFM0Th96euDYMP1dbU5Y/QsIddlyWtUut0R6aJTm', 5, 'ACTIVE'),
(5,'ravindu.emp','$2a$10$.DgsHI4JH48SnlFM0Th96euDYMP1dbU5Y/QsIddlyWtUut0R6aJTm', 1, 'ACTIVE'),
(6,'tharushi.admin','$2a$10$.DgsHI4JH48SnlFM0Th96euDYMP1dbU5Y/QsIddlyWtUut0R6aJTm', 6, 'ACTIVE');
GO

-- Demo OUTSIDE_USER (job-applicant portal) account, linked via Applicant_ID instead of
-- Employee_ID (Applicant_ID 1 = Amali Rathnayake, inserted above). Same demo password.
INSERT INTO USER_ACCOUNT (Applicant_ID, Username, Password_Hash, Role_ID, Account_Status) VALUES
(1,'amali.applicant','$2a$10$.DgsHI4JH48SnlFM0Th96euDYMP1dbU5Y/QsIddlyWtUut0R6aJTm', 7, 'ACTIVE');
GO

/* ------------------------------------------------------------
   12. NOTIFICATION / AUDIT_LOG
   ------------------------------------------------------------ */
INSERT INTO NOTIFICATION (User_ID, Notification_Type, Message, Status) VALUES
(3,'LEAVE_PENDING','A new leave request from Dilani Silva is pending approval.','UNREAD'),
(5,'LEAVE_APPROVED','Your leave request for 2026-09-10 to 2026-09-12 has been approved.','READ'),
(2,'PAYROLL_PENDING','Payroll for August 2026 is pending verification.','UNREAD');

INSERT INTO AUDIT_LOG (User_ID, Action_Type, Module, Action_Details) VALUES
(3,'APPROVE','LEAVE_REQUEST','Approved leave request #1 for Employee_ID 5'),
(2,'CREATE','PAYROLL','Processed payroll for Employee_ID 5, August 2026'),
(4,'UPDATE','USER_ACCOUNT','Reset password for username kavindu.it');
GO

PRINT 'Seed data inserted successfully.';
