import { useEffect, useState } from "react";
import { useAuth } from "../context/useAuth";
import { employeeService } from "../services/employeeService";
import { lookupService } from "../services/miscServices";
import { LoadingState, EmptyState, ErrorBanner, StatusBadge, Modal } from "../components/Common";

const EDIT_ROLES = ["HR_MANAGER", "ADMINISTRATIVE_OFFICER"];

export default function EmployeesPage() {
  const { user } = useAuth();
  const [employees, setEmployees] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showAdd, setShowAdd] = useState(false);

  const canEdit = EDIT_ROLES.includes(user?.role);

  function load() {
    setLoading(true);
    employeeService
      .list(search)
      .then(setEmployees)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    const timeout = setTimeout(load, 250); // debounce search
    return () => clearTimeout(timeout);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Employee Directory</h1>
          <div className="page-subtitle">{employees.length} employees</div>
        </div>
        {canEdit && (
          <button className="btn" onClick={() => setShowAdd(true)}>Add Employee</button>
        )}
      </div>

      <div className="search-bar">
        <input
          placeholder="Search by number or name..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <ErrorBanner message={error} />
      {loading && <LoadingState />}
      {!loading && employees.length === 0 && <EmptyState label="No employees found." />}

      {!loading && employees.length > 0 && (
        <table className="data-table">
          <thead>
            <tr>
              <th>Employee #</th>
              <th>Name</th>
              <th>Department</th>
              <th>Designation</th>
              <th>Phone</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {employees.map((e) => (
              <tr key={e.employeeId}>
                <td>{e.employeeNumber}</td>
                <td>{e.fullName}</td>
                <td>{e.departmentName}</td>
                <td>{e.designationName}</td>
                <td>{e.phoneNumber}</td>
                <td><StatusBadge status={e.employmentStatus} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {showAdd && (
        <AddEmployeeModal
          onClose={() => setShowAdd(false)}
          onSaved={() => {
            setShowAdd(false);
            load();
          }}
        />
      )}
    </div>
  );
}

function AddEmployeeModal({ onClose, onSaved }) {
  const [departments, setDepartments] = useState([]);
  const [designations, setDesignations] = useState([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    employeeNumber: "", firstName: "", lastName: "", dateOfBirth: "", gender: "MALE",
    phoneNumber: "", street: "", city: "", postalCode: "", departmentId: "", designationId: "",
    employmentType: "PERMANENT", employmentStartDate: "", workLocation: "",
  });

  useEffect(() => {
    lookupService.departments().then(setDepartments).catch(() => {});
    lookupService.designations().then(setDesignations).catch(() => {});
  }, []);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSaving(true);
    try {
      await employeeService.create({
        ...form,
        departmentId: Number(form.departmentId),
        designationId: Number(form.designationId),
      });
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Add Employee" onClose={onClose}>
      <ErrorBanner message={error} />
      <form onSubmit={handleSubmit}>
        <div className="form-row">
          <div className="form-field">
            <label>Employee Number</label>
            <input required value={form.employeeNumber} onChange={(e) => update("employeeNumber", e.target.value)} />
          </div>
          <div className="form-field">
            <label>Employment Type</label>
            <select value={form.employmentType} onChange={(e) => update("employmentType", e.target.value)}>
              <option value="PERMANENT">Permanent</option>
              <option value="CONTRACT">Contract</option>
            </select>
          </div>
        </div>
        <div className="form-row">
          <div className="form-field">
            <label>First Name</label>
            <input required value={form.firstName} onChange={(e) => update("firstName", e.target.value)} />
          </div>
          <div className="form-field">
            <label>Last Name</label>
            <input required value={form.lastName} onChange={(e) => update("lastName", e.target.value)} />
          </div>
        </div>
        <div className="form-row">
          <div className="form-field">
            <label>Date of Birth</label>
            <input type="date" required value={form.dateOfBirth} onChange={(e) => update("dateOfBirth", e.target.value)} />
          </div>
          <div className="form-field">
            <label>Gender</label>
            <select value={form.gender} onChange={(e) => update("gender", e.target.value)}>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
        </div>
        <div className="form-field">
          <label>Phone Number</label>
          <input required value={form.phoneNumber} onChange={(e) => update("phoneNumber", e.target.value)} />
        </div>
        <div className="form-row">
          <div className="form-field">
            <label>Department</label>
            <select required value={form.departmentId} onChange={(e) => update("departmentId", e.target.value)}>
              <option value="">Select...</option>
              {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </select>
          </div>
          <div className="form-field">
            <label>Designation</label>
            <select required value={form.designationId} onChange={(e) => update("designationId", e.target.value)}>
              <option value="">Select...</option>
              {designations.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </select>
          </div>
        </div>
        <div className="form-field">
          <label>Employment Start Date</label>
          <input type="date" required value={form.employmentStartDate} onChange={(e) => update("employmentStartDate", e.target.value)} />
        </div>
        <div className="form-field">
          <label>Work Location</label>
          <input value={form.workLocation} onChange={(e) => update("workLocation", e.target.value)} />
        </div>
        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 8 }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Employee"}</button>
        </div>
      </form>
    </Modal>
  );
}
