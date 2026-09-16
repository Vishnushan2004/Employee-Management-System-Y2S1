import { useEffect, useState } from "react";
import { useAuth } from "../context/useAuth";
import { payrollService } from "../services/moduleServices";
import { employeeService } from "../services/employeeService";
import { LoadingState, EmptyState, ErrorBanner, StatusBadge, Modal, StatCard } from "../components/Common";

export default function PayrollPage() {
  const { user } = useAuth();
  const isFinance = user?.role === "FINANCE_PAYROLL_MANAGER";
  const [records, setRecords] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [showProcess, setShowProcess] = useState(false);

  function load() {
    setLoading(true);
    const calls = [payrollService.list()];
    if (isFinance) calls.push(payrollService.summary());
    Promise.all(calls)
      .then(([list, sum]) => { setRecords(list); if (sum) setSummary(sum); })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleVerify(id) {
    setActionError("");
    try { await payrollService.verify(id); load(); } catch (err) { setActionError(err.message); }
  }
  async function handlePay(id) {
    setActionError("");
    try { await payrollService.markPaid(id); load(); } catch (err) { setActionError(err.message); }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">{isFinance ? "Payroll" : "My Payslips"}</h1>
          <div className="page-subtitle">{isFinance ? "Process, verify and pay monthly payroll" : "View your payslip history"}</div>
        </div>
        {isFinance && <button className="btn" onClick={() => setShowProcess(true)}>Process Payroll</button>}
      </div>

      {isFinance && summary && (
        <div className="card-grid">
          <StatCard label="Payroll Records" value={summary.payrollCount} />
          <StatCard label="Total Gross Salary" value={Number(summary.totalGross).toLocaleString()} />
          <StatCard label="Total Net Salary" value={Number(summary.totalNet).toLocaleString()} />
        </div>
      )}

      <ErrorBanner message={error || actionError} />
      {loading && <LoadingState />}
      {!loading && records.length === 0 && <EmptyState label="No payroll records yet." />}

      {!loading && records.length > 0 && (
        <table className="data-table">
          <thead>
            <tr>
              {isFinance && <th>Employee</th>}
              <th>Month/Year</th><th>Basic</th><th>Gross</th><th>Net</th><th>Status</th>{isFinance && <th></th>}
            </tr>
          </thead>
          <tbody>
            {records.map((p) => (
              <tr key={p.payrollId}>
                {isFinance && <td>{p.employeeName}</td>}
                <td>{p.payrollMonth}/{p.payrollYear}</td>
                <td>{Number(p.basicSalary).toLocaleString()}</td>
                <td>{Number(p.grossSalary).toLocaleString()}</td>
                <td>{Number(p.netSalary).toLocaleString()}</td>
                <td><StatusBadge status={p.payrollStatus} /></td>
                {isFinance && (
                  <td style={{ whiteSpace: "nowrap" }}>
                    {p.payrollStatus === "DRAFT" && <button className="btn btn-accent" onClick={() => handleVerify(p.payrollId)}>Verify</button>}
                    {p.payrollStatus === "VERIFIED" && <button className="btn" onClick={() => handlePay(p.payrollId)}>Mark Paid</button>}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {showProcess && <ProcessPayrollModal onClose={() => setShowProcess(false)} onSaved={() => { setShowProcess(false); load(); }} />}
    </div>
  );
}

function ProcessPayrollModal({ onClose, onSaved }) {
  const [employees, setEmployees] = useState([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const now = new Date();
  const [form, setForm] = useState({
    employeeId: "", payrollMonth: now.getMonth() + 1, payrollYear: now.getFullYear(),
    basicSalary: "", overtimeRatePerHour: "", allowanceType: "Transport", allowanceAmount: "",
    deductionType: "EPF Employee Share", deductionAmount: "",
  });

  useEffect(() => { employeeService.list().then(setEmployees).catch(() => {}); }, []);

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      const allowances = form.allowanceAmount ? [{ type: form.allowanceType, amount: Number(form.allowanceAmount) }] : [];
      const deductions = form.deductionAmount ? [{ type: form.deductionType, amount: Number(form.deductionAmount) }] : [];
      await payrollService.process({
        employeeId: Number(form.employeeId),
        payrollMonth: Number(form.payrollMonth),
        payrollYear: Number(form.payrollYear),
        basicSalary: Number(form.basicSalary),
        overtimeRatePerHour: form.overtimeRatePerHour ? Number(form.overtimeRatePerHour) : 0,
        allowances,
        deductions,
      });
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Process Payroll" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={handleSubmit}>
        <div className="form-field">
          <label>Employee</label>
          <select required value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
            <option value="">Select...</option>
            {employees.map((e) => <option key={e.employeeId} value={e.employeeId}>{e.fullName} ({e.employeeNumber})</option>)}
          </select>
        </div>
        <div className="form-row">
          <div className="form-field"><label>Month</label><input type="number" min="1" max="12" required value={form.payrollMonth} onChange={(e) => setForm({ ...form, payrollMonth: e.target.value })} /></div>
          <div className="form-field"><label>Year</label><input type="number" required value={form.payrollYear} onChange={(e) => setForm({ ...form, payrollYear: e.target.value })} /></div>
        </div>
        <div className="form-row">
          <div className="form-field"><label>Basic Salary</label><input type="number" step="0.01" required value={form.basicSalary} onChange={(e) => setForm({ ...form, basicSalary: e.target.value })} /></div>
          <div className="form-field"><label>Overtime Rate / hr</label><input type="number" step="0.01" value={form.overtimeRatePerHour} onChange={(e) => setForm({ ...form, overtimeRatePerHour: e.target.value })} /></div>
        </div>
        <div className="form-row">
          <div className="form-field"><label>Allowance Type</label><input value={form.allowanceType} onChange={(e) => setForm({ ...form, allowanceType: e.target.value })} /></div>
          <div className="form-field"><label>Allowance Amount</label><input type="number" step="0.01" value={form.allowanceAmount} onChange={(e) => setForm({ ...form, allowanceAmount: e.target.value })} /></div>
        </div>
        <div className="form-row">
          <div className="form-field"><label>Deduction Type</label><input value={form.deductionType} onChange={(e) => setForm({ ...form, deductionType: e.target.value })} /></div>
          <div className="form-field"><label>Deduction Amount</label><input type="number" step="0.01" value={form.deductionAmount} onChange={(e) => setForm({ ...form, deductionAmount: e.target.value })} /></div>
        </div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Processing..." : "Process Payroll"}</button>
        </div>
      </form>
    </Modal>
  );
}
