
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import { AuthProvider, useAuth } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import AppLayout from "./layouts/AppLayout";

import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import EmployeesPage from "./pages/EmployeesPage";
import LeavesPage from "./pages/LeavesPage";
import AttendancePage from "./pages/AttendancePage";
import ProfilePage from "./pages/ProfilePage";
import NotificationsPage from "./pages/NotificationsPage";
import AuditLogsPage from "./pages/AuditLogsPage";
import ShiftsPage from "./pages/ShiftsPage";
import PayrollPage from "./pages/PayrollPage";
import PayrollReportsPage from "./pages/PayrollReportsPage";
import MyPayrollPage from "./pages/MyPayrollPage";
import RecruitmentPage from "./pages/RecruitmentPage";
import PerformancePage from "./pages/PerformancePage";
import TrainingPage from "./pages/TrainingPage";
import CareerPage from "./pages/CareerPage";
import UsersPage from "./pages/UsersPage";
import RolesPermissionsPage from "./pages/RolesPermissionsPage";
import RegisterPage from "./pages/RegisterPage";
import ApplicantProfilePage from "./pages/ApplicantProfilePage";
import VacanciesPage from "./pages/VacanciesPage";
import ApplyPage from "./pages/ApplyPage";
import MyApplicationsPage from "./pages/MyApplicationsPage";
import AdminReportsPage from "./pages/AdminReportsPage";

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
