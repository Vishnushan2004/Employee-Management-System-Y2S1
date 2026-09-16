import { useEffect, useState } from "react";
import { adminService } from "../services/moduleServices";
import { lookupService } from "../services/miscServices";
import { LoadingState, EmptyState, ErrorBanner, Modal } from "../components/Common";

export default function RolesPermissionsPage() {
  const [roles, setRoles] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [selectedRoleId, setSelectedRoleId] = useState(null);
  const [rolePermissions, setRolePermissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showNewRole, setShowNewRole] = useState(false);
  const [showNewPermission, setShowNewPermission] = useState(false);

  function load() {
    setLoading(true);
    Promise.all([lookupService.roles(), adminService.listPermissions()])
      .then(([r, p]) => {
        setRoles(r);
        setPermissions(p);
        setSelectedRoleId((current) => current || (r.length > 0 ? r[0].id : null));
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  useEffect(() => {
    if (selectedRoleId) {
      adminService.permissionsForRole(selectedRoleId).then(setRolePermissions).catch((e) => setError(e.message));
    }
  }, [selectedRoleId]);

  const grantedIds = new Set(rolePermissions.map((p) => p.permissionId));

  async function grantPermission(permissionId) {
    setError("");
    try {
      await adminService.assignPermission(selectedRoleId, permissionId);
      const updated = await adminService.permissionsForRole(selectedRoleId);
      setRolePermissions(updated);
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Roles & Permissions</h1>
          <div className="page-subtitle">Manage roles and assign permissions to each role</div>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <button className="btn btn-secondary" onClick={() => setShowNewRole(true)}>New Role</button>
          <button className="btn" onClick={() => setShowNewPermission(true)}>New Permission</button>
        </div>
      </div>

      <ErrorBanner message={error} />
      {loading && <LoadingState />}

      {!loading && (
        <div style={{ display: "flex", gap: 24 }}>
          <div style={{ minWidth: 200 }}>
            <h2 style={{ fontSize: 14 }}>Roles</h2>
            {roles.map((r) => (
              <div
                key={r.id}
                onClick={() => setSelectedRoleId(r.id)}
                className="card"
                style={{
                  marginBottom: 8,
                  cursor: "pointer",
                  padding: "10px 14px",
                  borderColor: r.id === selectedRoleId ? "var(--color-primary)" : undefined,
                  fontWeight: r.id === selectedRoleId ? 600 : 400,
                }}
              >
                {r.name}
              </div>
            ))}
          </div>

          <div style={{ flex: 1 }}>
            <h2 style={{ fontSize: 14 }}>Permissions for selected role</h2>
            {permissions.length === 0 ? (
              <EmptyState label="No permissions defined yet." />
            ) : (
              <table className="data-table">
                <thead>
                  <tr><th>Permission</th><th>Module</th><th>Action</th><th>Granted</th></tr>
                </thead>
                <tbody>
                  {permissions.map((p) => (
                    <tr key={p.permissionId}>
                      <td>{p.permissionName}</td>
                      <td>{p.module}</td>
                      <td>{p.action}</td>
                      <td>
                        <input
                          type="checkbox"
                          checked={grantedIds.has(p.permissionId)}
                          onChange={() => grantPermission(p.permissionId)}
                          disabled={grantedIds.has(p.permissionId)}
                          title={grantedIds.has(p.permissionId) ? "Already granted" : "Grant this permission"}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {showNewRole && (
        <NewRoleModal onClose={() => setShowNewRole(false)} onSaved={() => { setShowNewRole(false); load(); }} />
      )}
      {showNewPermission && (
        <NewPermissionModal onClose={() => setShowNewPermission(false)} onSaved={() => { setShowNewPermission(false); load(); }} />
      )}
    </div>
  );
}

function NewRoleModal({ onClose, onSaved }) {
  const [form, setForm] = useState({ roleName: "", description: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await adminService.createRole(form);
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="New Role" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={submit}>
        <div className="form-field">
          <label>Role Name</label>
          <input required value={form.roleName} onChange={(e) => setForm({ ...form, roleName: e.target.value })} />
        </div>
        <div className="form-field">
          <label>Description</label>
          <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        </div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Role"}</button>
        </div>
      </form>
    </Modal>
  );
}

function NewPermissionModal({ onClose, onSaved }) {
  const [form, setForm] = useState({ permissionName: "", module: "", action: "VIEW" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await adminService.createPermission(form);
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="New Permission" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={submit}>
        <div className="form-field">
          <label>Permission Name</label>
          <input required value={form.permissionName} onChange={(e) => setForm({ ...form, permissionName: e.target.value })} />
        </div>
        <div className="form-field">
          <label>Module</label>
          <input required value={form.module} onChange={(e) => setForm({ ...form, module: e.target.value })} />
        </div>
        <div className="form-field">
          <label>Action</label>
          <select value={form.action} onChange={(e) => setForm({ ...form, action: e.target.value })}>
            <option>VIEW</option>
            <option>CREATE</option>
            <option>UPDATE</option>
            <option>DELETE</option>
            <option>APPROVE</option>
            <option>EXPORT</option>
          </select>
        </div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Permission"}</button>
        </div>
      </form>
    </Modal>
  );
}
