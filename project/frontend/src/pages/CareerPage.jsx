import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { careerService } from "../services/moduleServices";
import { employeeService } from "../services/employeeService";
import { lookupService } from "../services/miscServices";
import { LoadingState, EmptyState, ErrorBanner, Modal } from "../components/Common";

export default function CareerPage() {
  const { user } = useAuth();
  const isHr = user?.role === "HR_MANAGER";
  const [promotions, setPromotions] = useState([]);
  const [transfers, setTransfers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showPromo, setShowPromo] = useState(false);
  const [showTransfer, setShowTransfer] = useState(false);

  function load() {
    setLoading(true);
    Promise.all([careerService.listPromotions(), careerService.listTransfers()])
      .then(([p, t]) => { setPromotions(p); setTransfers(t); })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">{isHr ? "Promotions & Transfers" : "My Career History"}</h1>
        </div>
        {isHr && (
          <div style={{ display: "flex", gap: 10 }}>
            <button className="btn btn-secondary" onClick={() => setShowPromo(true)}>Record Promotion</button>
            <button className="btn" onClick={() => setShowTransfer(true)}>Record Transfer</button>
          </div>
        )}
      </div>

      <ErrorBanner message={error} />
      {loading && <LoadingState />}

      {!loading && (
        <>
          <h2 style={{ fontSize: 15 }}>Promotions</h2>
          {promotions.length === 0 ? <EmptyState label="No promotions recorded yet." /> : (
            <table className="data-table" style={{ marginBottom: 24 }}>
              <thead><tr>{isHr && <th>Employee</th>}<th>Previous Designation</th><th>New Designation</th><th>Effective Date</th><th>Reason</th></tr></thead>
              <tbody>
                {promotions.map((p) => (
                  <tr key={p.promotionId}>
                    {isHr && <td>{p.employeeName}</td>}
                    <td>{p.previousDesignation}</td><td>{p.newDesignation}</td><td>{p.effectiveDate}</td><td>{p.reason}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <h2 style={{ fontSize: 15 }}>Transfers</h2>
          {transfers.length === 0 ? <EmptyState label="No transfers recorded yet." /> : (
            <table className="data-table">
              <thead><tr>{isHr && <th>Employee</th>}<th>Previous Department</th><th>New Department</th><th>Effective Date</th><th>Reason</th></tr></thead>
              <tbody>
                {transfers.map((t) => (
                  <tr key={t.transferId}>
                    {isHr && <td>{t.employeeName}</td>}
                    <td>{t.previousDepartment}</td><td>{t.newDepartment}</td><td>{t.effectiveDate}</td><td>{t.reason}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {showPromo && <PromotionModal onClose={() => setShowPromo(false)} onSaved={() => { setShowPromo(false); load(); }} />}
      {showTransfer && <TransferModal onClose={() => setShowTransfer(false)} onSaved={() => { setShowTransfer(false); load(); }} />}
    </div>
  );
}

function PromotionModal({ onClose, onSaved }) {
  const [employees, setEmployees] = useState([]);
  const [designations, setDesignations] = useState([]);
  const [form, setForm] = useState({ employeeId: "", previousDesignation: "", newDesignationId: "", effectiveDate: "", reason: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    employeeService.list().then(setEmployees).catch(() => {});
    lookupService.designations().then(setDesignations).catch(() => {});
  }, []);

  async function submit(e) {
    e.preventDefault();
    setSaving(true); setError("");
    try {
      const newDesignation = designations.find((d) => String(d.id) === String(form.newDesignationId));
      await careerService.createPromotion({
        ...form, employeeId: Number(form.employeeId), newDesignationId: Number(form.newDesignationId),
        newDesignationName: newDesignation?.name || "",
      });
      onSaved();
    } catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  return (
    <Modal title="Record Promotion" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={submit}>
        <div className="form-field">
          <label>Employee</label>
          <select required value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
            <option value="">Select...</option>
            {employees.map((emp) => <option key={emp.employeeId} value={emp.employeeId}>{emp.fullName} ({emp.designationName})</option>)}
          </select>
        </div>
        <div className="form-field"><label>Previous Designation (text)</label><input required value={form.previousDesignation} onChange={(e) => setForm({ ...form, previousDesignation: e.target.value })} /></div>
        <div className="form-field">
          <label>New Designation</label>
          <select required value={form.newDesignationId} onChange={(e) => setForm({ ...form, newDesignationId: e.target.value })}>
            <option value="">Select...</option>
            {designations.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select>
        </div>
        <div className="form-field"><label>Effective Date</label><input type="date" required value={form.effectiveDate} onChange={(e) => setForm({ ...form, effectiveDate: e.target.value })} /></div>
        <div className="form-field"><label>Reason</label><textarea rows={2} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Promotion"}</button>
        </div>
      </form>
    </Modal>
  );
}

function TransferModal({ onClose, onSaved }) {
  const [employees, setEmployees] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [form, setForm] = useState({ employeeId: "", previousDepartment: "", newDepartmentId: "", effectiveDate: "", reason: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    employeeService.list().then(setEmployees).catch(() => {});
    lookupService.departments().then(setDepartments).catch(() => {});
  }, []);

  async function submit(e) {
    e.preventDefault();
    setSaving(true); setError("");
    try {
      const newDept = departments.find((d) => String(d.id) === String(form.newDepartmentId));
      await careerService.createTransfer({
        ...form, employeeId: Number(form.employeeId), newDepartmentId: Number(form.newDepartmentId),
        newDepartmentName: newDept?.name || "",
      });
      onSaved();
    } catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  return (
    <Modal title="Record Transfer" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={submit}>
        <div className="form-field">
          <label>Employee</label>
          <select required value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
            <option value="">Select...</option>
            {employees.map((emp) => <option key={emp.employeeId} value={emp.employeeId}>{emp.fullName} ({emp.departmentName})</option>)}
          </select>
        </div>
        <div className="form-field"><label>Previous Department (text)</label><input required value={form.previousDepartment} onChange={(e) => setForm({ ...form, previousDepartment: e.target.value })} /></div>
        <div className="form-field">
          <label>New Department</label>
          <select required value={form.newDepartmentId} onChange={(e) => setForm({ ...form, newDepartmentId: e.target.value })}>
            <option value="">Select...</option>
            {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select>
        </div>
        <div className="form-field"><label>Effective Date</label><input type="date" required value={form.effectiveDate} onChange={(e) => setForm({ ...form, effectiveDate: e.target.value })} /></div>
        <div className="form-field"><label>Reason</label><textarea rows={2} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Transfer"}</button>
        </div>
      </form>
    </Modal>
  );
}
