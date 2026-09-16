import { api } from "./api";

export const leaveService = {
  list: (scope) => api.get("/leaves", { scope }),
  apply: (leave) => api.post("/leaves", leave),
  approve: (id) => api.put(`/leaves/${id}/approve`),
  reject: (id) => api.put(`/leaves/${id}/reject`),
  cancel: (id) => api.put(`/leaves/${id}/cancel`),
};
