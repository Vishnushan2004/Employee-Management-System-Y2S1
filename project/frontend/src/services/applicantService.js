import { api } from "./api";

// Public, no auth required.
export const publicJobsService = {
  listOpenVacancies: () => api.get("/public/vacancies"),
};

export const applicantService = {
  getProfile: () => api.get("/applicant/profile"),
  updateProfile: (data) => api.put("/applicant/profile", data),

  listMyApplications: () => api.get("/applicant/applications"),
  apply: (data) => api.post("/applicant/applications", data),

  getInterview: (applicationId) => api.get(`/applicant/applications/${applicationId}/interview`),

  uploadCv: (applicationId, filename, fileContentBase64) =>
    api.post(`/applicant/applications/${applicationId}/cv`, { filename, fileContentBase64 }),
  downloadCv: (applicationId) => api.get(`/applicant/applications/${applicationId}/cv`),
};
