package com.sliit.ems.controller;

import com.sliit.ems.dao.*;
import com.sliit.ems.dao.hr.RecruitmentDAO;
import com.sliit.ems.dao.hr.PerformanceDAO;
import com.sliit.ems.dao.hr.TrainingCertDAO;
import com.sliit.ems.dao.finance.PayrollDAO;
import com.sliit.ems.dao.it.AdminDAO;
import com.sliit.ems.dao.it.AuditLogDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GET /api/dashboard - returns role-appropriate real counts from SQL Server, no hardcoded numbers. */
public class DashboardController {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final LeaveRequestDAO leaveDAO = new LeaveRequestDAO();
    private final RecruitmentDAO recruitmentDAO = new RecruitmentDAO();
    private final PerformanceDAO performanceDAO = new PerformanceDAO();
    private final TrainingCertDAO trainingCertDAO = new TrainingCertDAO();
    private final PayrollDAO payrollDAO = new PayrollDAO();
    private final AdminDAO adminDAO = new AdminDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public HttpHandler get() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                Map<String, Object> data = switch (session.roleName) {
                    case "HR_MANAGER" -> hrDashboard();
                    case "FINANCE_PAYROLL_MANAGER" -> financeDashboard();
                    case "OPERATIONS_MANAGER" -> operationsDashboard();
                    case "IT_COORDINATOR" -> itDashboard();
                    case "ADMINISTRATIVE_OFFICER" -> adminDashboard();
                    case "OUTSIDE_USER" -> outsideUserDashboard(session.applicantId);
                    default -> employeeDashboard(session.employeeId);
                };
                HttpUtil.sendJson(exchange, 200, data);
            } catch (Exception e) {
                AuthController.handleDbError(exchange, new java.sql.SQLException(e));
            }
        };
    }

    private Map<String, Object> hrDashboard() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalEmployees", employeeDAO.countAll());
        m.put("activeEmployees", employeeDAO.countActive());
        m.put("openVacancies", recruitmentDAO.countOpenVacancies());
        m.put("pendingApplications", recruitmentDAO.countPendingApplications());
        m.put("pendingPerformanceReviews", performanceDAO.countPendingReviews());
        m.put("certificationsExpiringSoon", trainingCertDAO.countExpiringSoon());
        return m;
    }

    private Map<String, Object> financeDashboard() throws Exception {
        Map<String, Object> summary = payrollDAO.summary(null, null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalEmployees", employeeDAO.countActive());
        if (summary != null) {
            m.put("payrollRecordsProcessed", summary.get("payrollCount"));
            m.put("totalGrossSalary", summary.get("totalGross"));
            m.put("totalNetSalary", summary.get("totalNet"));
        }
        return m;
    }

    private Map<String, Object> operationsDashboard() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("presentToday", attendanceDAO.countPresentToday());
        m.put("absentToday", attendanceDAO.countAbsentToday());
        m.put("lateToday", attendanceDAO.countLateToday());
        m.put("pendingLeaveRequests", leaveDAO.countPending());
        return m;
    }

    private Map<String, Object> itDashboard() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activeEmployees", employeeDAO.countActive());
        m.put("totalUsers", employeeDAO.countActive());
        m.put("recentAuditActivity", auditLogDAO.findRecent(5).size());
        return m;
    }

    private Map<String, Object> adminDashboard() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalEmployees", employeeDAO.countAll());
        m.put("presentToday", attendanceDAO.countPresentToday());
        m.put("pendingLeaveRequests", leaveDAO.countPending());
        return m;
    }

    private Map<String, Object> outsideUserDashboard(int applicantId) throws Exception {
        List<Map<String, Object>> myApplications = recruitmentDAO.findApplicationsByApplicant(applicantId);
        long underReview = myApplications.stream()
            .filter(a -> java.util.Set.of("SUBMITTED", "SHORTLISTED", "INTERVIEWED").contains(a.get("applicationStatus")))
            .count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("openVacancies", recruitmentDAO.countOpenVacancies());
        m.put("myApplications", myApplications.size());
        m.put("applicationsUnderReview", underReview);
        return m;
    }

    private Map<String, Object> employeeDashboard(int employeeId) throws Exception {
        var employee = employeeDAO.findById(employeeId);
        var myLeaves = leaveDAO.findByEmployee(employeeId);
        long pending = myLeaves.stream().filter(l -> "PENDING".equals(l.status)).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("annualLeaveBalance", employee.annualLeaveBalance);
        m.put("casualLeaveBalance", employee.casualLeaveBalance);
        m.put("sickLeaveBalance", employee.sickLeaveBalance);
        m.put("pendingLeaveRequests", pending);
        return m;
    }
}
