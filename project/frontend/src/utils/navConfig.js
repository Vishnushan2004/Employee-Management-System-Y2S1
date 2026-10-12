
export const NAV_BY_ROLE = {
  HR_MANAGER: [
    { to: "/dashboard", label: "Dashboard" },
    { to: "/employees", label: "Employees" },
    { to: "/recruitment", label: "Recruitment" },
    { to: "/performance", label: "Performance" },
    { to: "/training", label: "Training & Certifications" },
    { to: "/career", label: "Promotions & Transfers" },
    { to: "/leaves", label: "Leave Requests" },
    { to: "/attendance", label: "Attendance" },
    { to: "/my-payroll", label: "My Payroll Summary" },
    { to: "/notifications", label: "Notifications" },
  ],

  FINANCE_PAYROLL_MANAGER: [
    { to: "/dashboard", label: "Dashboard" },
    { to: "/payroll", label: "Payroll Processing" },
    { to: "/payroll-reports", label: "Payroll Reports" },
    { to: "/employees", label: "Employee Directory" },
    { to: "/attendance", label: "Attendance" },
    { to: "/my-payroll", label: "My Payroll Summary" },
    { to: "/notifications", label: "Notifications" },
  ],

  OPERATIONS_MANAGER: [
    { to: "/dashboard", label: "Dashboard" },
    { to: "/employees", label: "Employees" },
    { to: "/shifts", label: "Shift Management" },
    { to: "/attendance", label: "Attendance" },
    { to: "/leaves", label: "Leave Requests" },
    { to: "/my-payroll", label: "My Payroll Summary" },
    { to: "/notifications", label: "Notifications" },
  ],

  IT_COORDINATOR: [
    { to: "/dashboard", label: "Dashboard" },
    { to: "/users", label: "User Accounts" },
    { to: "/roles-permissions", label: "Roles & Permissions" },
    { to: "/employees", label: "Employee Directory" },
    { to: "/audit-logs", label: "Audit Logs" },
    { to: "/my-payroll", label: "My Payroll Summary" },
    { to: "/notifications", label: "Notifications" },
  ],

  ADMINISTRATIVE_OFFICER: [
    { to: "/dashboard", label: "Dashboard" },
    { to: "/employees", label: "Employees" },
    { to: "/attendance", label: "Attendance" },
    { to: "/leaves", label: "Leave Processing" },
    { to: "/admin-reports", label: "Administrative Reports" },
    { to: "/my-payroll", label: "My Payroll Summary" },
    { to: "/notifications", label: "Notifications" },
  ],

  EMPLOYEE: [
    { to: "/dashboard", label: "Dashboard" },
    { to: "/profile", label: "My Profile" },
    { to: "/attendance", label: "My Attendance" },
    { to: "/leaves", label: "My Leave" },
    { to: "/my-shifts", label: "My Shift" },
    { to: "/payroll", label: "My Payslips" },
    { to: "/my-payroll", label: "My Payroll Summary" },
    { to: "/performance", label: "My Performance" },
    { to: "/training", label: "My Training" },
    { to: "/career", label: "My Career History" },
    { to: "/my-payroll", label: "My Payroll Summary" },
    { to: "/notifications", label: "Notifications" },
  ],

  OUTSIDE_USER: [
    { to: "/dashboard", label: "Dashboard" },
    { to: "/applicant/profile", label: "My Profile" },
    { to: "/applicant/vacancies", label: "Job Vacancies" },
    { to: "/applicant/applications", label: "My Applications" },
    { to: "/notifications", label: "Notifications" },
  ],
};

export const ROLE_LABELS = {
  HR_MANAGER: "HR Manager",
  FINANCE_PAYROLL_MANAGER: "Finance & Payroll Manager",
  OPERATIONS_MANAGER: "Operations Manager",
  IT_COORDINATOR: "IT Coordinator",
  ADMINISTRATIVE_OFFICER: "Administrative Officer",
  EMPLOYEE: "Employee",
  OUTSIDE_USER: "Job Applicant",
};
