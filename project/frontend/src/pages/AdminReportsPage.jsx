import { useEffect, useState } from "react";
import { reportsService } from "../services/miscServices";
import { LoadingState, EmptyState, ErrorBanner } from "../components/Common";

function toCsv(rows, columns) {
  const header = columns.map((c) => c.label).join(",");
  const lines = rows.map((r) =>
    columns.map((c) => `"${String(r[c.key] ?? "").replace(/"/g, '""')}"`).join(",")
  );
  return [header, ...lines].join("\n");
}

function downloadCsv(filename, rows, columns) {
  const blob = new Blob([toCsv(rows, columns)], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

export default function AdminReportsPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    reportsService
      .getAdminReports()
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingState />;
  if (error) return <ErrorBanner message={error} />;
  if (!data) return null;

  const { departmentHeadcount, attendanceSummary, leaveSummary, employeeDirectory } = data;

  return (
    <div className="print-reports">
      <div className="page-header no-print">
        <div>
          <h1 className="page-title">Administrative Reports</h1>
          <div className="page-subtitle">Headcount, attendance, and leave summaries for record-keeping</div>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <button className="btn btn-secondary" onClick={() => window.print()}>Print</button>
          <button
            className="btn"
            onClick={() =>
              downloadCsv("employee_directory.csv", employeeDirectory, [
                { key: "employeeNumber", label: "Employee Number" },
                { key: "employeeName", label: "Name" },
                { key: "departmentName", label: "Department" },
                { key: "designationName", label: "Designation" },
                { key: "employmentStatus", label: "Status" },
              ])
            }
          >
            Export Directory (CSV)
          </button>
        </div>
      </div>

      <div className="card-grid">
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Department Headcount</h3>
          {departmentHeadcount.length === 0 ? (
            <EmptyState label="No departments found." />
          ) : (
            <table className="data-table">
              <thead><tr><th>Department</th><th>Active Employees</th></tr></thead>
              <tbody>
                {departmentHeadcount.map((d) => (
                  <tr key={d.departmentName}><td>{d.departmentName}</td><td>{d.employeeCount}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>Attendance This Month</h3>
          {attendanceSummary.length === 0 ? (
            <EmptyState label="No attendance records this month." />
          ) : (
            <table className="data-table">
              <thead><tr><th>Status</th><th>Count</th></tr></thead>
              <tbody>
                {attendanceSummary.map((a) => (
                  <tr key={a.attendanceStatus}><td>{a.attendanceStatus}</td><td>{a.recordCount}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>Leave Requests by Type &amp; Status</h3>
          {leaveSummary.length === 0 ? (
            <EmptyState label="No leave requests found." />
          ) : (
            <table className="data-table">
              <thead><tr><th>Type</th><th>Status</th><th>Count</th></tr></thead>
              <tbody>
                {leaveSummary.map((l, i) => (
                  <tr key={i}><td>{l.leaveType}</td><td>{l.status}</td><td>{l.requestCount}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <div className="card" style={{ marginTop: 20 }}>
        <h3 style={{ marginTop: 0 }}>Employee Directory</h3>
        {employeeDirectory.length === 0 ? (
          <EmptyState label="No employees found." />
        ) : (
          <table className="data-table">
            <thead>
              <tr><th>Employee #</th><th>Name</th><th>Department</th><th>Designation</th><th>Status</th></tr>
            </thead>
            <tbody>
              {employeeDirectory.map((e) => (
                <tr key={e.employeeNumber}>
                  <td>{e.employeeNumber}</td><td>{e.employeeName}</td><td>{e.departmentName}</td>
                  <td>{e.designationName}</td><td>{e.employmentStatus}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}