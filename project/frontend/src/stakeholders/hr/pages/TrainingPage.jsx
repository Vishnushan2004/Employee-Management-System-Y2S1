import { useEffect, useState } from "react";
import { useAuth } from "../../../context/AuthContext";
import { trainingService } from "../../../services/moduleServices";
import { employeeService } from "../../../services/employeeService";
import { LoadingState, EmptyState, ErrorBanner, StatusBadge, Modal } from "../../../components/Common";

export default function TrainingPage() {
  const { user } = useAuth();
  const isHr = user?.role === "HR_MANAGER";
  const [training, setTraining] = useState([]);
  const [certifications, setCertifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showTraining, setShowTraining] = useState(false);
  const [showCert, setShowCert] = useState(false);
  const [assignFor, setAssignFor] = useState(null);

  function load() {
    setLoading(true);
    Promise.all([trainingService.list(), trainingService.listCertifications()])
      .then(([t, c]) => { setTraining(t); setCertifications(c); })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">{isHr ? "Training & Certifications" : "My Training & Certifications"}</h1>
        </div>
        {isHr && (
          <div style={{ display: "flex", gap: 10 }}>
            <button className="btn btn-secondary" onClick={() => setShowTraining(true)}>New Training</button>
            <button className="btn" onClick={() => setShowCert(true)}>Add Certification</button>
          </div>
        )}
      </div>

      <ErrorBanner message={error} />
      {loading && <LoadingState />}

      {!loading && (
        <>
          <h2 style={{ fontSize: 15 }}>Training</h2>
          {training.length === 0 ? <EmptyState label="No training records yet." /> : (
            <table className="data-table" style={{ marginBottom: 24 }}>
              <thead><tr>{isHr ? <th>Title</th> : <th>Title</th>}<th>Provider</th><th>Start</th><th>End</th><th>Type</th>{!isHr && <th>Status</th>}{isHr && <th></th>}</tr></thead>
              <tbody>
                {training.map((t) => (
                  <tr key={t.trainingId}>
                    <td>{t.trainingTitle}</td><td>{t.provider}</td><td>{t.startDate}</td><td>{t.endDate || "-"}</td><td>{t.trainingType}</td>
                    {!isHr && <td><StatusBadge status={t.completionStatus} /></td>}
                    {isHr && <td><button className="btn btn-secondary" onClick={() => setAssignFor(t.trainingId)}>Assign Employees</button></td>}
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <h2 style={{ fontSize: 15 }}>Certifications</h2>
          {certifications.length === 0 ? <EmptyState label="No certifications yet." /> : (
            <table className="data-table">
              <thead><tr>{isHr && <th>Employee</th>}<th>Name</th><th>Issuer</th><th>Issue Date</th><th>Expiry Date</th><th>Status</th></tr></thead>
              <tbody>
                {certifications.map((c) => (
                  <tr key={c.certificationId}>
                    {isHr && <td>{c.employeeName}</td>}
                    <td>{c.certificationName}</td><td>{c.issuingOrganization}</td><td>{c.issueDate}</td><td>{c.expiryDate || "-"}</td>
                    <td><StatusBadge status={c.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {showTraining && <TrainingModal onClose={() => setShowTraining(false)} onSaved={() => { setShowTraining(false); load(); }} />}
      {showCert && <CertModal onClose={() => setShowCert(false)} onSaved={() => { setShowCert(false); load(); }} />}
      {assignFor && <AssignModal trainingId={assignFor} onClose={() => setAssignFor(null)} onSaved={() => setAssignFor(null)} />}
    </div>
  );
}

function TrainingModal({ onClose, onSaved }) {
  const [form, setForm] = useState({ trainingTitle: "", provider: "", startDate: "", endDate: "", trainingType: "TECHNICAL" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setSaving(true); setError("");
    try { await trainingService.create(form); onSaved(); } catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  return (
    <Modal title="New Training Program" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={submit}>
        <div className="form-field"><label>Title</label><input required value={form.trainingTitle} onChange={(e) => setForm({ ...form, trainingTitle: e.target.value })} /></div>
        <div className="form-field"><label>Provider</label><input value={form.provider} onChange={(e) => setForm({ ...form, provider: e.target.value })} /></div>
        <div className="form-row">
          <div className="form-field"><label>Start Date</label><input type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></div>
          <div className="form-field"><label>End Date</label><input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></div>
        </div>
        <div className="form-field"><label>Type</label><input value={form.trainingType} onChange={(e) => setForm({ ...form, trainingType: e.target.value })} /></div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Training"}</button>
        </div>
      </form>
    </Modal>
  );
}

function CertModal({ onClose, onSaved }) {
  const [employees, setEmployees] = useState([]);
  const [form, setForm] = useState({ employeeId: "", certificationName: "", issuingOrganization: "", issueDate: "", expiryDate: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => { employeeService.list().then(setEmployees).catch(() => {}); }, []);

  async function submit(e) {
    e.preventDefault();
    setSaving(true); setError("");
    try { await trainingService.createCertification({ ...form, employeeId: Number(form.employeeId) }); onSaved(); }
    catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  return (
    <Modal title="Add Certification" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={submit}>
        <div className="form-field">
          <label>Employee</label>
          <select required value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
            <option value="">Select...</option>
            {employees.map((emp) => <option key={emp.employeeId} value={emp.employeeId}>{emp.fullName}</option>)}
          </select>
        </div>
        <div className="form-field"><label>Certification Name</label><input required value={form.certificationName} onChange={(e) => setForm({ ...form, certificationName: e.target.value })} /></div>
        <div className="form-field"><label>Issuing Organization</label><input value={form.issuingOrganization} onChange={(e) => setForm({ ...form, issuingOrganization: e.target.value })} /></div>
        <div className="form-row">
          <div className="form-field"><label>Issue Date</label><input type="date" required value={form.issueDate} onChange={(e) => setForm({ ...form, issueDate: e.target.value })} /></div>
          <div className="form-field"><label>Expiry Date</label><input type="date" value={form.expiryDate} onChange={(e) => setForm({ ...form, expiryDate: e.target.value })} /></div>
        </div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Certification"}</button>
        </div>
      </form>
    </Modal>
  );
}

function AssignModal({ trainingId, onClose, onSaved }) {
  const [employees, setEmployees] = useState([]);
  const [selected, setSelected] = useState([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => { employeeService.list().then(setEmployees).catch(() => {}); }, []);

  function toggle(id) {
    setSelected((s) => s.includes(id) ? s.filter((x) => x !== id) : [...s, id]);
  }

  async function submit(e) {
    e.preventDefault();
    if (selected.length === 0) { setError("Select at least one employee."); return; }
    setSaving(true); setError("");
    try { await trainingService.assign(trainingId, selected); onSaved(); } catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  return (
    <Modal title="Assign Employees to Training" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={submit}>
        <div style={{ maxHeight: 260, overflowY: "auto", marginBottom: 14 }}>
          {employees.map((emp) => (
            <label key={emp.employeeId} style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0", fontSize: 13.5 }}>
              <input type="checkbox" checked={selected.includes(emp.employeeId)} onChange={() => toggle(emp.employeeId)} />
              {emp.fullName} ({emp.employeeNumber})
            </label>
          ))}
        </div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Assigning..." : "Assign Selected"}</button>
        </div>
      </form>
    </Modal>
  );
}
