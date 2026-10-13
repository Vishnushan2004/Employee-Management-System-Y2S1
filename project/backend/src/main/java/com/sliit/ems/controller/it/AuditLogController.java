package com.sliit.ems.controller.it;

import com.sliit.ems.controller.AuthController;

import com.sliit.ems.dao.it.AuditLogDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

public class AuditLogController {

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public HttpHandler list() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, "IT_COORDINATOR");
            if (session == null) return;
            try {
                HttpUtil.sendJson(exchange, 200, auditLogDAO.findRecent(200));
            } catch (java.sql.SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }
}
