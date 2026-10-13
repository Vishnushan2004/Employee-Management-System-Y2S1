package com.sliit.ems.controller.administration;

import com.sliit.ems.controller.AuthController;

import com.sliit.ems.dao.it.AuditLogDAO;
import com.sliit.ems.dao.administration.ReportsDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Security;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Administrative Reports - exclusive to the ADMINISTRATIVE_OFFICER role.
 * This is the Administrative Officer's distinguishing feature: a
 * record-keeping/reporting screen other roles don't have, limited strictly
 * to headcount/attendance/leave summaries (no payroll, no recruitment
 * decisions, no system administration - consistent with the role's
 * restricted permission set).
 */
public class ReportsController {

    private final ReportsDAO reportsDAO = new ReportsDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final String ROLE = "ADMINISTRATIVE_OFFICER";

    public HttpHandler adminReports() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange))
                return;
            var session = Security.requireRole(exchange, ROLE);
            if (session == null)
                return;
            try {
                Map<String, Object> report = new LinkedHashMap<>();
                report.put("departmentHeadcount", reportsDAO.departmentHeadcount());
                report.put("attendanceSummary", reportsDAO.attendanceSummaryThisMonth());
                report.put("leaveSummary", reportsDAO.leaveSummary());
                report.put("employeeDirectory", reportsDAO.employeeDirectory());
                auditLogDAO.record(session.userId, "VIEW", "ADMIN_REPORTS", "Viewed administrative reports");
                HttpUtil.sendJson(exchange, 200, report);
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }
}