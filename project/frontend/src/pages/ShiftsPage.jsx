import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { shiftService } from "../services/moduleServices";
import { employeeService } from "../services/employeeService";
import { LoadingState, EmptyState, ErrorBanner, Modal } from "../components/Common";

export default function ShiftsPage() {
  const { user } = useAuth();
  const isOps = user?.role === "OPERATIONS_MANAGER";
  const [shifts, setShifts] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showNewShift, setShowNewShift] = useState(false);
  const [showAssign, setShowAssign] = useState(false);

  function load() {
    setLoading(true);
    Promise.all([shiftService.listShifts(), shiftService.listAssignments()])
      .then(([s, a]) => { setShifts(s); setAssignments(a); })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Shift Management</h1>
          <div className="page-subtitle">{isOps ? "Create shifts and assign employees" : "View shift assignments"}</div>
        </div>
        {isOps && (
          <div style={{ display: "flex", gap: 10 }}>
            <button className="btn btn-secondary" onClick={() => setShowNewShift(true)}>New Shift</button>
            <button className="btn" onClick={() => setShowAssign(true)}>Assign Shift</button>
          </div>
        )}
      </div>

      <ErrorBanner message={error} />
      {loading && <LoadingState />}

      {!loading && (
        <>
          <h2 style={{ fontSize: 15 }}>Shifts</h2>
          {shifts.length === 0 ? <EmptyState label="No shifts defined yet." /> : (
            <table className="data-table" style={{ marginBottom: 24 }}>
              <thead><tr><th>Name</th><th>Type</th><th>Start</th><th>End</th><th>Working Hours</th></tr></thead>
              <tbody>
                {shifts.map((s) => (
                  <tr key={s.shiftId}><td>{s.shiftName}</td><td>{s.shiftType}</td><td>{s.startTime}</td><td>{s.endTime}</td><td>{s.workingHours}</td></tr>
                ))}
              </tbody>
            </table>
          )}

          <h2 style={{ fontSize: 15 }}>Assignments</h2>
          {assignments.length === 0 ? <EmptyState label="No shift assignments yet." /> : (
            <table className="data-table">
              <thead><tr>{isOps && <th>Employee</th>}<th>Shift</th><th>Start Date</th><th>End Date</th></tr></thead>
              <tbody>
                {assignments.map((a) => (
                  <tr key={a.assignmentId}>
                    {isOps && <td>{a.employeeName}</td>}
                    <td>{a.shiftName}</td>
                    <td>{a.assignmentStartDate}</td>
                    <td>{a.assignmentEndDate || "Ongoing"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {showNewShift && <NewShiftModal onClose={() => setShowNewShift(false)} onSaved={() => { setShowNewShift(false); load(); }} />}
      {showAssign && <AssignShiftModal shifts={shifts} onClose={() => setShowAssign(false)} onSaved={() => { setShowAssign(false); load(); }} />}
    </div>
  );
}

function NewShiftModal({ onClose, onSaved }) {
  const [form, setForm] = useState({ shiftName: "", shiftType: "MORNING", startTime: "09:00", endTime: "17:00", workingHours: 8 });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await shiftService.createShift({ ...form, workingHours: Number(form.workingHours) });
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="New Shift" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={handleSubmit}>
        <div className="form-field"><label>Shift Name</label><input required value={form.shiftName} onChange={(e) => setForm({ ...form, shiftName: e.target.value })} /></div>
        <div className="form-field">
          <label>Shift Type</label>
          <select value={form.shiftType} onChange={(e) => setForm({ ...form, shiftType: e.target.value })}>
            <option value="MORNING">Morning</option><option value="EVENING">Evening</option><option value="NIGHT">Night</option><option value="GENERAL">General</option>
          </select>
        </div>
        <div className="form-row">
          <div className="form-field"><label>Start Time</label><input type="time" required value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} /></div>
          <div className="form-field"><label>End Time</label><input type="time" required value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} /></div>
        </div>
        <div className="form-field"><label>Working Hours</label><input type="number" step="0.5" required value={form.workingHours} onChange={(e) => setForm({ ...form, workingHours: e.target.value })} /></div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Shift"}</button>
        </div>
      </form>
    </Modal>
  );
}

function AssignShiftModal({ shifts, onClose, onSaved }) {
  const [employees, setEmployees] = useState([]);
  const [form, setForm] = useState({ employeeId: "", shiftId: "", assignmentStartDate: "", assignmentEndDate: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => { employeeService.list().then(setEmployees).catch(() => {}); }, []);

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await shiftService.assignShift({
        ...form,
        employeeId: Number(form.employeeId),
        shiftId: Number(form.shiftId),
        assignmentEndDate: form.assignmentEndDate || null,
      });
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Assign Shift" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={handleSubmit}>
        <div className="form-field">
          <label>Employee</label>
          <select required value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
            <option value="">Select...</option>
            {employees.map((e) => <option key={e.employeeId} value={e.employeeId}>{e.fullName} ({e.employeeNumber})</option>)}
          </select>
        </div>
        <div className="form-field">
          <label>Shift</label>
          <select required value={form.shiftId} onChange={(e) => setForm({ ...form, shiftId: e.target.value })}>
            <option value="">Select...</option>
            {shifts.map((s) => <option key={s.shiftId} value={s.shiftId}>{s.shiftName}</option>)}
          </select>
        </div>
        <div className="form-row">
          <div className="form-field"><label>Start Date</label><input type="date" required value={form.assignmentStartDate} onChange={(e) => setForm({ ...form, assignmentStartDate: e.target.value })} /></div>
          <div className="form-field"><label>End Date (optional)</label><input type="date" value={form.assignmentEndDate} onChange={(e) => setForm({ ...form, assignmentEndDate: e.target.value })} /></div>
        </div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Assign"}</button>
        </div>
      </form>
    </Modal>
  );
}
