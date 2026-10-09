import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { dashboardService } from "../services/miscServices";
import { StatCard, LoadingState, ErrorBanner } from "../components/Common";
import { ROLE_LABELS } from "../utils/navConfig";

const LABELS = {
  totalEmployees: "Total Employees",
  activeEmployees: "Active Employees",
  presentToday: "Present Today",
  absentToday: "Absent Today",
  lateToday: "Late Today",
  pendingLeaveRequests: "Pending Leave Requests",
  annualLeaveBalance: "Annual Leave Balance",
  casualLeaveBalance: "Casual Leave Balance",
  sickLeaveBalance: "Sick Leave Balance",
};

export default function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    dashboardService
      .get()
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Welcome back, {user?.username}</h1>
          <div className="page-subtitle">{ROLE_LABELS[user?.role]} dashboard</div>
        </div>
      </div>

      <ErrorBanner message={error} />
      {loading && <LoadingState />}

      {data && (
        <div className="card-grid">
          {Object.entries(data).map(([key, value]) => (
            <StatCard key={key} label={LABELS[key] || key} value={value} />
          ))}
        </div>
      )}
    </div>
  );
}
