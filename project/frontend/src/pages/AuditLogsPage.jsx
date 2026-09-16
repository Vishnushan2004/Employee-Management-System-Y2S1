import { useEffect, useState } from "react";
import { auditLogService } from "../services/miscServices";
import { LoadingState, EmptyState, ErrorBanner } from "../components/Common";

export default function AuditLogsPage() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    auditLogService
      .list()
      .then(setLogs)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Audit Logs</h1>
          <div className="page-subtitle">Recent sensitive actions across the system</div>
        </div>
      </div>

      <ErrorBanner message={error} />
      {loading && <LoadingState />}
      {!loading && logs.length === 0 && <EmptyState label="No audit activity yet." />}

      {!loading && logs.length > 0 && (
        <table className="data-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>User</th>
              <th>Action</th>
              <th>Module</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((l) => (
              <tr key={l.auditLogId}>
                <td>{l.actionDate}</td>
                <td>{l.username}</td>
                <td>{l.actionType}</td>
                <td>{l.module}</td>
                <td>{l.actionDetails}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
