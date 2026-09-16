package com.sliit.ems.controller;

import com.sliit.ems.dao.LookupDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;

public class LookupController {

    private final LookupDAO lookupDAO = new LookupDAO();

    public HttpHandler departments() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                HttpUtil.sendJson(exchange, 200, lookupDAO.findAllDepartments());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler designations() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                HttpUtil.sendJson(exchange, 200, lookupDAO.findAllDesignations());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler roles() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, "IT_COORDINATOR");
            if (session == null) return;
            try {
                HttpUtil.sendJson(exchange, 200, lookupDAO.findAllRoles());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }
}
