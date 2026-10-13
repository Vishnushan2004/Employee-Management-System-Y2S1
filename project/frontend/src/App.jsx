
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import { AuthProvider, useAuth } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import AppLayout from "./layouts/AppLayout";

import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import EmployeesPage from "./stakeholders/hr/pages/EmployeesPage.jsx";
import LeavesPage from "./stakeholders/operations/pages/LeavesPage.jsx";
import AttendancePage from "./stakeholders/operations/pages/AttendancePage.jsx";
import ProfilePage from "./stakeholders/employee/pages/ProfilePage.jsx";
import NotificationsPage from "./pages/NotificationsPage";
import AuditLogsPage from "./stakeholders/it/pages/AuditLogsPage.jsx";
import ShiftsPage from "./stakeholders/operations/pages/ShiftsPage.jsx";
import PayrollPage from "./stakeholders/finance/pages/PayrollPage.jsx";
import PayrollReportsPage from "./stakeholders/finance/pages/PayrollReportsPage.jsx";
import MyPayrollPage from "./stakeholders/employee/pages/MyPayrollPage.jsx";
import RecruitmentPage from "./stakeholders/hr/pages/RecruitmentPage.jsx";
import PerformancePage from "./stakeholders/hr/pages/PerformancePage.jsx";
import TrainingPage from "./stakeholders/hr/pages/TrainingPage.jsx";
import CareerPage from "./stakeholders/hr/pages/CareerPage.jsx";
import UsersPage from "./stakeholders/it/pages/UsersPage.jsx";
import RolesPermissionsPage from "./stakeholders/it/pages/RolesPermissionsPage.jsx";
import RegisterPage from "./pages/RegisterPage";
import ApplicantProfilePage from "./applicant/pages/ApplicantProfilePage.jsx";
import VacanciesPage from "./applicant/pages/VacanciesPage.jsx";
import ApplyPage from "./applicant/pages/ApplyPage.jsx";
import MyApplicationsPage from "./applicant/pages/MyApplicationsPage.jsx";
import AdminReportsPage from "./stakeholders/administration/pages/AdminReportsPage.jsx";

function LoginRoute() {
  const { user } = useAuth();

  if (user) {
    return <Navigate to="/dashboard" replace />;
  }

  return <LoginPage />;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginRoute />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route
            path="/"
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/dashboard" replace />} />

            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="employees" element={<EmployeesPage />} />
            <Route path="leaves" element={<LeavesPage />} />
            <Route path="attendance" element={<AttendancePage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="notifications" element={<NotificationsPage />} />
            <Route path="audit-logs" element={<AuditLogsPage />} />
            <Route path="shifts" element={<ShiftsPage />} />
            <Route path="my-shifts" element={<ShiftsPage />} />

            <Route path="payroll" element={<PayrollPage />} />
            <Route
              path="payroll-reports"
              element={
                <ProtectedRoute allowedRoles={["FINANCE_PAYROLL_MANAGER"]}>
                  <PayrollReportsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="my-payroll"
              element={
                <ProtectedRoute allowedRoles={["EMPLOYEE"]}>
                  <MyPayrollPage />
                </ProtectedRoute>
              }
            />

            <Route path="recruitment" element={<RecruitmentPage />} />
            <Route path="performance" element={<PerformancePage />} />
            <Route path="training" element={<TrainingPage />} />
            <Route path="career" element={<CareerPage />} />
            <Route path="users" element={<UsersPage />} />
            <Route
              path="roles-permissions"
              element={<RolesPermissionsPage />}
            />

            <Route
              path="applicant/profile"
              element={<ApplicantProfilePage />}
            />
            <Route
              path="applicant/vacancies"
              element={<VacanciesPage />}
            />
            <Route
              path="applicant/apply/:vacancyId"
              element={<ApplyPage />}
            />
            <Route
              path="applicant/applications"
              element={<MyApplicationsPage />}
            />

            <Route path="admin-reports" element={<AdminReportsPage />} />
          </Route>

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
