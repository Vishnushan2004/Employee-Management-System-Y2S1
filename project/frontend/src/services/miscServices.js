import { api } from "./api";

export const lookupService = {
  departments: () => api.get("/departments"),
  designations: () => api.get("/designations"),
  roles: () => api.get("/roles"),
};

export const dashboardService = {
  get: () => api.get("/dashboard"),
};

export const notificationService = {
  list: () => api.get("/notifications"),
  markRead: (id) => api.put(`/notifications/${id}/read`),
};

export const auditLogService = {
  list: () => api.get("/audit-logs"),
};
