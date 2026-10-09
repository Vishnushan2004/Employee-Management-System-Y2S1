import { api } from "./api";

export const shiftService = {
  listShifts: () => api.get("/shifts"),
  createShift: (shift) => api.post("/shifts", shift),
  updateShift: (id, shift) => api.put(`/shifts/${id}`, shift),
  listAssignments: () => api.get("/shift-assignments"),
  assignShift: (assignment) => api.post("/shift-assignments", assignment),
};

export const payrollService = {
  list: (params) => api.get("/payroll", params),
  detail: (id) => api.get(`/payroll/detail/${id}`),
  process: (payload) => api.post("/payroll", payload),
  verify: (id) => api.put(`/payroll/${id}/verify`),
  markPaid: (id) => api.put(`/payroll/${id}/pay`),
  summary: (params) => api.get("/payroll/summary", params),
};

export const recruitmentService = {
  listVacancies: () => api.get("/vacancies"),
  createVacancy: (v) => api.post("/vacancies", v),
  updateVacancyStatus: (id, status) => api.put(`/vacancies/${id}/status`, { status }),
  listApplicants: (search) => api.get("/applicants", { search }),
  createApplicant: (a) => api.post("/applicants", a),
  listApplications: () => api.get("/applications"),
  createApplication: (a) => api.post("/applications", a),
  updateApplicationStatus: (id, status) => api.put(`/applications/${id}/status`, { status }),
  downloadCv: (id) => api.get(`/applications/${id}/cv`),
  listInterviews: () => api.get("/interviews"),
  scheduleInterview: (i) => api.post("/interviews", i),
  recordInterviewResult: (id, result, remarks) => api.put(`/interviews/${id}/result`, { result, remarks }),
};

export const performanceService = {
  listGoals: (employeeId) => api.get("/performance/goals", { employeeId }),
  createGoal: (g) => api.post("/performance/goals", g),
  updateGoal: (id, fields) => api.put(`/performance/goals/${id}`, fields),
  listReviews: (employeeId) => api.get("/performance/reviews", { employeeId }),
  createReview: (r) => api.post("/performance/reviews", r),
};

export const trainingService = {
  list: () => api.get("/training"),
  create: (t) => api.post("/training", t),
  assign: (id, employeeIds) => api.post(`/training/${id}/assign`, { employeeIds }),
  attendees: (id) => api.get(`/training/${id}/attendees`),
  listCertifications: () => api.get("/certifications"),
  createCertification: (c) => api.post("/certifications", c),
};

export const careerService = {
  listPromotions: () => api.get("/promotions"),
  createPromotion: (p) => api.post("/promotions", p),
  listTransfers: () => api.get("/transfers"),
  createTransfer: (t) => api.post("/transfers", t),
};

export const adminService = {
  listUsers: () => api.get("/users"),
  createUser: (u) => api.post("/users", u),
  setAccountStatus: (id, status) => api.put(`/users/${id}/status`, { status }),
  changeRole: (id, roleId) => api.put(`/users/${id}/role`, { roleId }),
  createRole: (role) => api.post("/roles", role),
  listPermissions: () => api.get("/permissions"),
  permissionsForRole: (roleId) => api.get(`/permissions/role/${roleId}`),
  assignPermission: (roleId, permissionId) => api.post("/role-permissions", { roleId, permissionId }),
};
