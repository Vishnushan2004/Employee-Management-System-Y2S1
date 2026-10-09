import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { performanceService } from "../services/moduleServices";
import { employeeService } from "../services/employeeService";
import { LoadingState, EmptyState, ErrorBanner, StatusBadge, Modal } from "../components/Common";

export default function PerformancePage() {
  const { user } = useAuth();
  const isHr = user?.role === "HR_MANAGER";
  const [goals, setGoals] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showGoal, setShowGoal] = useState(false);
  const [showReview, setShowReview] = useState(false);

  function load() {
    setLoading(true);
    Promise.all([performanceService.listGoals(), performanceService.listReviews()])
      .then(([g, r]) => { setGoals(g); setReviews(r); })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  async function updateProgress(goalId, progress, status) {
    try { await performanceService.updateGoal(goalId, { progress, status }); load(); } catch (e) { setError(e.message); }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">{isHr ? "Performance Management" : "My Performance"}</h1>
        </div>
        {isHr && (
          <div style={{ display: "flex", gap: 10 }}>
            <button className="btn btn-secondary" onClick={() => setShowGoal(true)}>New Goal</button>
            <button className="btn" onClick={() => setShowReview(true)}>New Review</button>
          </div>
        )}
      </div>

      <ErrorBanner message={error} />
      {loading && <LoadingState />}

      {!loading && (
        <>
          <h2 style={{ fontSize: 15 }}>Performance Goals</h2>
          {goals.length === 0 ? <EmptyState label="No goals yet." /> : (
            <table className="data-table" style={{ marginBottom: 24 }}>
              <thead><tr>{isHr && <th>Employee</th>}<th>Goal</th><th>Target</th><th>Progress</th><th>Status</th></tr></thead>
              <tbody>
                {goals.map((g) => (
                  <tr key={g.goalId}>
                    {isHr && <td>{g.employeeName}</td>}
                    <td>{g.goalTitle}</td><td>{g.targetValue}</td>
                    <td>
                      {isHr ? (
                        <input type="number" min="0" max="100" defaultValue={g.progress} style={{ width: 60 }}
                          onBlur={(e) => updateProgress(g.goalId, Number(e.target.value), g.status)} />
                      ) : `${g.progress}%`}
                    </td>
                    <td><StatusBadge status={g.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <h2 style={{ fontSize: 15 }}>Performance Reviews</h2>
          {reviews.length === 0 ? <EmptyState label="No reviews yet." /> : (
            <table className="data-table">
              <thead><tr>{isHr && <th>Employee</th>}<th>Period</th><th>Date</th><th>Score</th><th>Feedback</th></tr></thead>
              <tbody>
                {reviews.map((r) => (
                  <tr key={r.reviewId}>
                    {isHr && <td>{r.employeeName}</td>}
                    <td>{r.reviewPeriod}</td><td>{r.reviewDate}</td><td>{r.performanceScore ?? "-"}</td><td>{r.feedback}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {showGoal && <GoalModal onClose={() => setShowGoal(false)} onSaved={() => { setShowGoal(false); load(); }} />}
      {showReview && <ReviewModal onClose={() => setShowReview(false)} onSaved={() => { setShowReview(false); load(); }} />}
    </div>
  );
}

function GoalModal({ onClose, onSaved }) {
  const [employees, setEmployees] = useState([]);
  const [form, setForm] = useState({ employeeId: "", goalTitle: "", description: "", targetValue: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => { employeeService.list().then(setEmployees).catch(() => {}); }, []);

  async function submit(e) {
    e.preventDefault();
    setSaving(true); setError("");
    try { await performanceService.createGoal({ ...form, employeeId: Number(form.employeeId) }); onSaved(); }
    catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  return (
    <Modal title="New Performance Goal" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={submit}>
        <div className="form-field">
          <label>Employee</label>
          <select required value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
            <option value="">Select...</option>
            {employees.map((emp) => <option key={emp.employeeId} value={emp.employeeId}>{emp.fullName}</option>)}
          </select>
        </div>
        <div className="form-field"><label>Goal Title</label><input required value={form.goalTitle} onChange={(e) => setForm({ ...form, goalTitle: e.target.value })} /></div>
        <div className="form-field"><label>Description</label><textarea rows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
        <div className="form-field"><label>Target Value</label><input value={form.targetValue} onChange={(e) => setForm({ ...form, targetValue: e.target.value })} /></div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Goal"}</button>
        </div>
      </form>
    </Modal>
  );
}

function ReviewModal({ onClose, onSaved }) {
  const [employees, setEmployees] = useState([]);
  const [form, setForm] = useState({ employeeId: "", reviewDate: "", reviewPeriod: "", performanceScore: "", feedback: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => { employeeService.list().then(setEmployees).catch(() => {}); }, []);

  async function submit(e) {
    e.preventDefault();
    setSaving(true); setError("");
    try {
      await performanceService.createReview({
        ...form, employeeId: Number(form.employeeId),
        performanceScore: form.performanceScore ? Number(form.performanceScore) : null,
      });
      onSaved();
    } catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  return (
    <Modal title="New Performance Review" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={submit}>
        <div className="form-field">
          <label>Employee</label>
          <select required value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
            <option value="">Select...</option>
            {employees.map((emp) => <option key={emp.employeeId} value={emp.employeeId}>{emp.fullName}</option>)}
          </select>
        </div>
        <div className="form-row">
          <div className="form-field"><label>Review Date</label><input type="date" required value={form.reviewDate} onChange={(e) => setForm({ ...form, reviewDate: e.target.value })} /></div>
          <div className="form-field"><label>Review Period</label><input required placeholder="e.g. H1 2026" value={form.reviewPeriod} onChange={(e) => setForm({ ...form, reviewPeriod: e.target.value })} /></div>
        </div>
        <div className="form-field"><label>Score (0-5)</label><input type="number" step="0.1" min="0" max="5" value={form.performanceScore} onChange={(e) => setForm({ ...form, performanceScore: e.target.value })} /></div>
        <div className="form-field"><label>Feedback</label><textarea rows={3} value={form.feedback} onChange={(e) => setForm({ ...form, feedback: e.target.value })} /></div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Review"}</button>
        </div>
      </form>
    </Modal>
  );
}
