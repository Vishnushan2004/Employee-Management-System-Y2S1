import { useEffect, useState } from "react";
import { adminService } from "../../../services/moduleServices";
import { employeeService } from "../../../services/employeeService";
import { lookupService } from "../../../services/miscServices";
import { LoadingState, EmptyState, ErrorBanner, StatusBadge, Modal } from "../../../components/Common";

export default function UsersPage() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showNew, setShowNew] = useState(false);

  function load() {
    setLoading(true);
    Promise.all([adminService.listUsers(), lookupService.roles()])
      .then(([u, r]) => { setUsers(u); setRoles(r); })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  async function toggleStatus(user) {
    const next = user.accountStatus === "ACTIVE" ? "DISABLED" : "ACTIVE";
    try { await adminService.setAccountStatus(user.userId, next); load(); } catch (e) { setError(e.message); }
  }

  async function changeRole(userId, roleId) {
    try { await adminService.changeRole(userId, Number(roleId)); load(); } catch (e) { setError(e.message); }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">User Accounts</h1>
          <div className="page-subtitle">Manage logins, roles, and account status</div>
        </div>
        <button className="btn" onClick={() => setShowNew(true)}>Create Account</button>
      </div>

      <ErrorBanner message={error} />
      {loading && <LoadingState />}
      {!loading && users.length === 0 && <EmptyState label="No user accounts yet." />}

      {!loading && users.length > 0 && (
        <table className="data-table">
          <thead><tr><th>Username</th><th>Employee</th><th>Role</th><th>Status</th><th>Last Login</th><th></th></tr></thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.userId}>
                <td>{u.username}</td>
                <td>{u.employeeName}</td>
                <td>
                  <select value={u.roleId} onChange={(e) => changeRole(u.userId, e.target.value)}>
                    {roles.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                  </select>
                </td>
                <td><StatusBadge status={u.accountStatus} /></td>
                <td>{u.lastLogin ? u.lastLogin.substring(0, 16) : "Never"}</td>
                <td>
                  <button className="btn btn-secondary" onClick={() => toggleStatus(u)}>
                    {u.accountStatus === "ACTIVE" ? "Disable" : "Activate"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {showNew && <NewUserModal roles={roles} onClose={() => setShowNew(false)} onSaved={() => { setShowNew(false); load(); }} />}
    </div>
  );
}

function NewUserModal({ roles, onClose, onSaved }) {
  const [employees, setEmployees] = useState([]);
  const [form, setForm] = useState({ employeeId: "", username: "", password: "", roleId: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => { employeeService.list().then(setEmployees).catch(() => {}); }, []);

  async function submit(e) {
    e.preventDefault();
    setSaving(true); setError("");
    try {
      await adminService.createUser({ ...form, employeeId: Number(form.employeeId), roleId: Number(form.roleId) });
      onSaved();
    } catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  return (
    <Modal title="Create User Account" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={submit}>
        <div className="form-field">
          <label>Employee (must not already have a login)</label>
          <select required value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
            <option value="">Select...</option>
            {employees.map((emp) => <option key={emp.employeeId} value={emp.employeeId}>{emp.fullName} ({emp.employeeNumber})</option>)}
          </select>
        </div>
        <div className="form-field"><label>Username</label><input required value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></div>
        <div className="form-field"><label>Temporary Password</label><input type="password" required minLength={6} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></div>
        <div className="form-field">
          <label>Role</label>
          <select required value={form.roleId} onChange={(e) => setForm({ ...form, roleId: e.target.value })}>
            <option value="">Select...</option>
            {roles.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
          </select>
        </div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Creating..." : "Create Account"}</button>
        </div>
      </form>
    </Modal>
  );
}
