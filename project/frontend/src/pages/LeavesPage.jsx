import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { leaveService } from "../services/leaveService";
import { LoadingState, EmptyState, ErrorBanner, StatusBadge, Modal } from "../components/Common";

const APPROVER_ROLES = ["OPERATIONS_MANAGER", "ADMINISTRATIVE_OFFICER"];

export default function LeavesPage() {
  const { user } = useAuth();
  const [leaves, setLeaves] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showApply, setShowApply] = useState(false);
  const [actionError, setActionError] = useState("");

  const isApprover = APPROVER_ROLES.includes(user?.role);

  function load() {
    setLoading(true);
    leaveService
      .list()
      .then(setLeaves)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleAction(fn, id) {
    setActionError("");
    try {
      await fn(id);
      load();
    } catch (err) {
      setActionError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">{isApprover ? "Leave Requests" : "My Leave"}</h1>
          <div className="page-subtitle">
            {isApprover ? "Review and process employee leave requests" : "Apply for leave and track your requests"}
          </div>
        </div>
        <button className="btn" onClick={() => setShowApply(true)}>Apply for Leave</button>
      </div>

      <ErrorBanner message={error || actionError} />
      {loading && <LoadingState />}
      {!loading && leaves.length === 0 && <EmptyState label="No leave requests yet." />}

      {!loading && leaves.length > 0 && (
        <table className="data-table">
          <thead>
            <tr>
              {isApprover && <th>Employee</th>}
              <th>Type</th>
              <th>Start</th>
              <th>End</th>
              <th>Reason</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {leaves.map((l) => (
              <tr key={l.leaveId}>
                {isApprover && <td>{l.employeeName}</td>}
                <td>{l.leaveType}</td>
                <td>{l.startDate}</td>
                <td>{l.endDate}</td>
                <td>{l.reason}</td>
                <td><StatusBadge status={l.status} /></td>
                <td style={{ whiteSpace: "nowrap" }}>
                  {isApprover && l.status === "PENDING" && (
                    <>
                      <button className="btn btn-accent" style={{ marginRight: 6 }} onClick={() => handleAction(leaveService.approve, l.leaveId)}>Approve</button>
                      <button className="btn btn-danger" onClick={() => handleAction(leaveService.reject, l.leaveId)}>Reject</button>
                    </>
                  )}
                  {!isApprover && (l.status === "PENDING" || l.status === "APPROVED") && (
                    <button className="btn btn-secondary" onClick={() => handleAction(leaveService.cancel, l.leaveId)}>Cancel</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {showApply && (
        <ApplyLeaveModal
          onClose={() => setShowApply(false)}
          onSaved={() => { setShowApply(false); load(); }}
        />
      )}
    </div>
  );
}

function ApplyLeaveModal({ onClose, onSaved }) {
  const [form, setForm] = useState({ leaveType: "ANNUAL", startDate: "", endDate: "", reason: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    if (form.endDate < form.startDate) {
      setError("End date cannot be before start date.");
      return;
    }
    setSaving(true);
    try {
      await leaveService.apply(form);
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Apply for Leave" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={handleSubmit}>
        <div className="form-field">
          <label>Leave Type</label>
          <select value={form.leaveType} onChange={(e) => setForm({ ...form, leaveType: e.target.value })}>
            <option value="ANNUAL">Annual</option>
            <option value="CASUAL">Casual</option>
            <option value="SICK">Sick</option>
            <option value="OTHER">Other</option>
          </select>
        </div>
        <div className="form-row">
          <div className="form-field">
            <label>Start Date</label>
            <input type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
          </div>
          <div className="form-field">
            <label>End Date</label>
            <input type="date" required value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
          </div>
        </div>
        <div className="form-field">
          <label>Reason</label>
          <textarea rows={3} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} />
        </div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 8 }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Submitting..." : "Submit Request"}</button>
        </div>
      </form>
    </Modal>
  );
}
