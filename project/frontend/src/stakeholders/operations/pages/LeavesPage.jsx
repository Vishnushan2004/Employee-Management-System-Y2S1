
import { useEffect, useState } from "react";
import { useAuth } from "../../../context/AuthContext";
import { leaveService } from "../../../services/leaveService";
import {
  LoadingState,
  EmptyState,
  ErrorBanner,
  StatusBadge,
  Modal,
} from "../../../components/Common";

const APPROVER_ROLES = [
  "OPERATIONS_MANAGER",
  "ADMINISTRATIVE_OFFICER",
];

// Use local calendar dates rather than UTC dates.
function getTodayLocal() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

export default function LeavesPage() {
  const { user } = useAuth();

  const [leaves, setLeaves] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showApply, setShowApply] = useState(false);
  const [actionError, setActionError] = useState("");

  const isApprover = APPROVER_ROLES.includes(user?.role);

  async function load() {
    setLoading(true);
    setError("");

    try {
      const data = await leaveService.list();
      setLeaves(data);
    } catch (err) {
      setError(err.message || "Failed to load leave requests.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleAction(fn, id) {
    setActionError("");

    try {
      await fn(id);
      await load();
    } catch (err) {
      setActionError(err.message || "The action failed.");
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">
            {isApprover ? "Leave Requests" : "My Leave"}
          </h1>

          <div className="page-subtitle">
            {isApprover
              ? "Review and process employee leave requests"
              : "Apply for leave and track your requests"}
          </div>
        </div>

        <button
          className="btn"
          onClick={() => {
            setActionError("");
            setShowApply(true);
          }}
        >
          Apply for Leave
        </button>
      </div>

      <ErrorBanner message={error || actionError} />

      {loading && <LoadingState />}

      {!loading && leaves.length === 0 && (
        <EmptyState label="No leave requests yet." />
      )}

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

                <td>
                  <StatusBadge status={l.status} />
                </td>

                <td style={{ whiteSpace: "nowrap" }}>
                  {isApprover && l.status === "PENDING" && (
                    <>
                      <button
                        className="btn btn-accent"
                        style={{ marginRight: 6 }}
                        onClick={() =>
                          handleAction(leaveService.approve, l.leaveId)
                        }
                      >
                        Approve
                      </button>

                      <button
                        className="btn btn-danger"
                        onClick={() =>
                          handleAction(leaveService.reject, l.leaveId)
                        }
                      >
                        Reject
                      </button>
                    </>
                  )}

                  {!isApprover &&
                    (l.status === "PENDING" ||
                      l.status === "APPROVED") && (
                      <button
                        className="btn btn-secondary"
                        onClick={() =>
                          handleAction(leaveService.cancel, l.leaveId)
                        }
                      >
                        Cancel
                      </button>
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
          onSaved={async () => {
            setShowApply(false);
            await load();
          }}
        />
      )}
    </div>
  );
}

function ApplyLeaveModal({ onClose, onSaved }) {
  const [form, setForm] = useState({
    leaveType: "ANNUAL",
    startDate: "",
    endDate: "",
    reason: "",
  });

  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  // Recalculate whenever validation runs, so a form left open
  // across midnight still uses the current local date.
  const today = getTodayLocal();

  function updateField(field, value) {
    setForm((previous) => ({
      ...previous,
      [field]: value,
    }));

    setError("");
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");

    const currentDate = getTodayLocal();

    if (!form.startDate || !form.endDate) {
      setError("Please select both the start date and end date.");
      return;
    }

    if (form.startDate < currentDate) {
      setError("Leave start date cannot be in the past.");
      return;
    }

    if (form.endDate < currentDate) {
      setError("Leave end date cannot be in the past.");
      return;
    }

    if (form.endDate < form.startDate) {
      setError("End date cannot be before start date.");
      return;
    }

    setSaving(true);

    try {
      await leaveService.apply(form);
      await onSaved();
    } catch (err) {
      setError(err.message || "Failed to submit leave request.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Apply for Leave" onClose={onClose}>
      <ErrorBanner message={error} />

      <form onSubmit={handleSubmit}>
        <div className="form-field">
          <label htmlFor="leaveType">Leave Type</label>

          <select
            id="leaveType"
            value={form.leaveType}
            onChange={(e) =>
              updateField("leaveType", e.target.value)
            }
            required
          >
            <option value="ANNUAL">Annual</option>
            <option value="CASUAL">Casual</option>
            <option value="SICK">Sick</option>
            <option value="OTHER">Other</option>
          </select>
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="startDate">Start Date</label>

            <input
              id="startDate"
              type="date"
              required
              min={today}
              value={form.startDate}
              onChange={(e) =>
                updateField("startDate", e.target.value)
              }
            />
          </div>

          <div className="form-field">
            <label htmlFor="endDate">End Date</label>

            <input
              id="endDate"
              type="date"
              required
              min={form.startDate || today}
              value={form.endDate}
              onChange={(e) =>
                updateField("endDate", e.target.value)
              }
            />
          </div>
        </div>

        <div className="form-field">
          <label htmlFor="reason">Reason</label>

          <textarea
            id="reason"
            rows={3}
            value={form.reason}
            onChange={(e) => updateField("reason", e.target.value)}
            maxLength={1000}
          />
        </div>

        <div
          style={{
            display: "flex",
            gap: 10,
            justifyContent: "flex-end",
            marginTop: 8,
          }}
        >
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onClose}
            disabled={saving}
          >
            Cancel
          </button>

          <button
            type="submit"
            className="btn"
            disabled={saving}
          >
            {saving ? "Submitting..." : "Submit Request"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
