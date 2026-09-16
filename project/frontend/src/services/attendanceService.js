import { api } from "./api";

export const attendanceService = {
  list: (params) => api.get("/attendance", params),
  checkIn: () => api.post("/attendance/check-in"),
  checkOut: () => api.post("/attendance/check-out"),
  correct: (id, fields) => api.put(`/attendance/correct/${id}`, fields),
};
