import { api } from "./api";

export const employeeService = {
  list: (search, departmentId) => api.get("/employees", { search, departmentId }),
  getMe: () => api.get("/employees/me"),
  getById: (id) => api.get(`/employees/${id}`),
  create: (employee) => api.post("/employees", employee),
  update: (id, employee) => api.put(`/employees/${id}`, employee),
  updateOwnProfile: (fields) => api.put("/employees/me", fields),
  deactivate: (id) => api.put(`/employees/${id}/deactivate`),
};
