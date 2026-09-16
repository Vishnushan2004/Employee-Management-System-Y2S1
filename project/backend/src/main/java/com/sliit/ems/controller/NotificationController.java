package com.sliit.ems.controller;

import com.sliit.ems.dao.NotificationDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.util.Map;

public class NotificationController {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    public HttpHandler list() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                HttpUtil.sendJson(exchange, 200, notificationDAO.findByUser(session.userId));
            } catch (java.sql.SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler markRead() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            int id = HttpUtil.extractIdFromPath(exchange);
            try {
                notificationDAO.markRead(id);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Marked as read"));
            } catch (java.sql.SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }
}
