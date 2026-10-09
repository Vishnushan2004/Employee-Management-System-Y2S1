# RBAC Permission Matrix

All access control is enforced server-side via `Security.requireRole()` at the top of
each controller handler — the frontend only hides nav links for UX, per project rule.
Verified by inspecting every `controller/*.java` file's role-allow-lists.

| Module / Action                          | HR Mgr | Finance & Payroll | Operations Mgr | IT Coordinator | Admin Officer | Employee | Outside User |
|-------------------------------------------|:------:|:------------------:|:---------------:|:----------------:|:---------------:|:---------:|:--------------:|
| View employee directory                   | Y | Y | Y | Y | Y | own only | N |
| Create / edit employee records             | Y | N | N | N | Y (admin fields only) | own contact fields only | N |
| Deactivate employee                        | Y | N | N | N | N | N | N |
| Attendance check-in/out                    | self | self | self | self | self | self | N |
| Correct attendance                         | N | N | Y | N | Y | N | N |
| Apply for leave                            | self | self | self | self | self | self | N |
| Approve / reject / cancel leave            | N | N | Y | N | Y | N | N |
| Shift management                           | N | view | Y | N | N | view own | N |
| Process / verify / pay payroll             | N | Y | N | N | N | N | N |
| View own payslip                           | N | N | N | N | N | Y | N |
| Recruitment: manage vacancies/applicants   | Y | N | N | N | N | N | N |
| Recruitment: accept/reject/schedule        | Y | N | N | N | N | N | N |
| View/download applicant CVs (HR)           | Y | N | N | N | N | N | N |
| Performance goals/reviews                  | Y | N | N | N | N | own view | N |
| Training & certifications                  | Y | N | N | N | N | own view | N |
| Promotions & transfers                     | Y | N | N | N | N | own history view | N |
| User account management (create/disable)   | N | N | N | Y | N | N | N |
| Role & permission management                | N | N | N | Y | N | N | N |
| Audit log viewing                          | N | N | N | Y | N | N | N |
| Admin dashboard (record-keeping stats)      | N | N | N | N | Y | N | N |
| Register as job applicant                   | n/a | n/a | n/a | n/a | n/a | n/a | Y (public) |
| Browse open vacancies                        | N | N | N | N | N | N | Y |
| View/edit own applicant profile              | N | N | N | N | N | N | Y (own only) |
| Apply to a vacancy / upload own CV           | N | N | N | N | N | N | Y (own only) |
| View own application status/interview        | N | N | N | N | N | N | Y (own only, ownership-checked) |
| View own notifications                       | all roles see their own notifications via NotificationController, scoped by session User_ID | | | | | | Y |
| Access any other applicant's data            | N | N | N | N | N | N | **N (IDOR-blocked)** |
| Access employee/payroll/HR-decision data     | N/A | N/A | N/A | N/A | N/A | N/A | **N (role-gated)** |

## Administrative Officer audit finding

Inspected `EmployeeController`, `LeaveController`, `AttendanceController`, `ShiftController`,
`PayrollController`, `RecruitmentController`, `AdminController`, and `AuditLogController`.
`ADMINISTRATIVE_OFFICER` appears only in: employee view/edit (admin fields), leave
approve/reject/cancel, attendance correction, and shift viewing. It does **not** appear in
`PayrollController`, `RecruitmentController`'s decision endpoints, `AdminController`
(user/role/permission management), or `AuditLogController`. No change was required — the
existing role-allow-lists already match the requested boundary (employee record-keeping,
attendance correction, leave processing; no payroll, no recruitment decisions, no system
administration).

## Anti-IDOR enforcement (outside users)

Every `OUTSIDE_USER`-gated endpoint that accepts an ID (`applicationId`) calls
`RecruitmentDAO.applicationBelongsToApplicant(applicationId, session.applicantId)` before
reading or writing anything. `Applicant_ID` is always taken from the authenticated
session, never from the request body or URL. See `ApplicantPortalController.java`.
