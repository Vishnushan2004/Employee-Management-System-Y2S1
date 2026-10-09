import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { attendanceService } from "../services/attendanceService";
import { LoadingState, EmptyState, ErrorBanner, StatusBadge } from "../components/Common";

const STAFF_ROLES = ["OPERATIONS_MANAGER", "ADMINISTRATIVE_OFFICER", "HR_MANAGER", "FINANCE_PAYROLL_MANAGER"];

export default function AttendancePage() {
  const { user } = useAuth();
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  const isStaff = STAFF_ROLES.includes(user?.role);

  function load() {
    setLoading(true);
    attendanceService
      .list()
      .then(setRecords)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function doCheckIn() {
    setActionError("");
    setActionLoading(true);
    try {
      await attendanceService.checkIn();
      load();
    } catch (err) {
      setActionError(err.message);
    } finally {
      setActionLoading(false);
    }
  }

  async function doCheckOut() {
    setActionError("");
    setActionLoading(true);
    try {
      await attendanceService.checkOut();
      load();
    } catch (err) {
      setActionError(err.message);
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">{isStaff ? "Attendance" : "My Attendance"}</h1>
          <div className="page-subtitle">
            {isStaff ? "Attendance records across the organization" : "Check in and check out, and review your history"}
          </div>
        </div>
        {!isStaff && (
          <div style={{ display: "flex", gap: 10 }}>
            <button className="btn" disabled={actionLoading} onClick={doCheckIn}>Check In</button>
            <button className="btn btn-secondary" disabled={actionLoading} onClick={doCheckOut}>Check Out</button>
          </div>
        )}
      </div>

      <ErrorBanner message={error || actionError} />
      {loading && <LoadingState />}
      {!loading && records.length === 0 && <EmptyState label="No attendance records yet." />}

      {!loading && records.length > 0 && (
        <table className="data-table">
          <thead>
            <tr>
              {isStaff && <th>Employee</th>}
              <th>Date</th>
              <th>Check In</th>
              <th>Check Out</th>
              <th>Late (min)</th>
              <th>Overtime (hrs)</th>
              <th>Working Hours</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {records.map((r) => (
              <tr key={r.attendanceId}>
                {isStaff && <td>{r.employeeName}</td>}
                <td>{r.attendanceDate}</td>
                <td>{r.checkInTime ? r.checkInTime.substring(11, 16) : "-"}</td>
                <td>{r.checkOutTime ? r.checkOutTime.substring(11, 16) : "-"}</td>
                <td>{r.lateMinutes}</td>
                <td>{r.overtimeHours}</td>
                <td>{r.workingHours ?? "-"}</td>
                <td><StatusBadge status={r.attendanceStatus} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
