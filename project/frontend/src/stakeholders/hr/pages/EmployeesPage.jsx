
import { useEffect, useState } from "react";
import { useAuth } from "../../../context/AuthContext";
import { employeeService } from "../../../services/employeeService";
import { lookupService } from "../../../services/miscServices";
import {
  LoadingState,
  EmptyState,
  ErrorBanner,
  StatusBadge,
  Modal,
} from "../../../components/Common";

const EDIT_ROLES = ["HR_MANAGER", "ADMINISTRATIVE_OFFICER"];

// Returns today's date in local YYYY-MM-DD format.
function getTodayDate() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

// Validates a YYYY-MM-DD date without converting it to UTC.
function isValidDateString(value) {
  if (typeof value !== "string" ||
      !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }

  const [year, month, day] = value.split("-").map(Number);

  const date = new Date(year, month - 1, day);

  return (
    date.getFullYear() === year &&
    date.getMonth() === month - 1 &&
    date.getDate() === day
  );
}

// Calculates age using the employee's local calendar date.
function calculateAge(dateOfBirth) {
  const [year, month, day] = dateOfBirth.split("-").map(Number);
  const today = new Date();

  let age = today.getFullYear() - year;

  const birthdayNotReached =
    today.getMonth() + 1 < month ||
    (today.getMonth() + 1 === month && today.getDate() < day);

  if (birthdayNotReached) {
    age--;
  }

  return age;
}

// Validates employee dates and phone number before saving.
function validateEmployee(form) {
  const today = getTodayDate();

  // Date of Birth validation
  if (!form.dateOfBirth) {
    return "Please enter the employee's Date of Birth.";
  }

  if (!isValidDateString(form.dateOfBirth)) {
    return "Please enter a valid Date of Birth.";
  }

  if (form.dateOfBirth > today) {
    return "Date of Birth cannot be in the future.";
  }

  if (calculateAge(form.dateOfBirth) < 18) {
    return "The employee must be at least 18 years old.";
  }

  // Phone Number validation
  const phone = (form.phoneNumber || "").trim().replace(/[\s()-]/g, "");

  if (!/^(?:0\d{9}|\+94\d{9})$/.test(phone)) {
    return (
      "Enter a valid Sri Lankan phone number, " +
      "for example 0712345678 or +94712345678."
    );
  }

  // Employment Start Date validation
  if (!form.employmentStartDate) {
    return "Please enter the Employment Start Date.";
  }

  if (!isValidDateString(form.employmentStartDate)) {
    return "Please enter a valid Employment Start Date.";
  }

  if (form.employmentStartDate > today) {
    return "Employment Start Date cannot be in the future.";
  }

  if (form.employmentStartDate < form.dateOfBirth) {
    return "Employment Start Date cannot be before Date of Birth.";
  }

  return "";
}

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
    setError("");

    employeeService
      .list(search)
      .then(setEmployees)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    const timeout = setTimeout(load, 250);
    return () => clearTimeout(timeout);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Employee Directory</h1>
          <div className="page-subtitle">
            {employees.length} employees
          </div>
        </div>

        {canEdit && (
          <button
            className="btn"
            onClick={() => {
              setError("");
              setShowAdd(true);
            }}
          >
            Add Employee
          </button>
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

      {!loading && employees.length === 0 && (
        <EmptyState label="No employees found." />
      )}

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
            {employees.map((employee) => (
              <tr key={employee.employeeId}>
                <td>{employee.employeeNumber}</td>
                <td>{employee.fullName}</td>
                <td>{employee.departmentName}</td>
                <td>{employee.designationName}</td>
                <td>{employee.phoneNumber}</td>
                <td>
                  <StatusBadge status={employee.employmentStatus} />
                </td>
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
    employeeNumber: "",
    firstName: "",
    lastName: "",
    dateOfBirth: "",
    gender: "MALE",
    phoneNumber: "",
    street: "",
    city: "",
    postalCode: "",
    departmentId: "",
    designationId: "",
    employmentType: "PERMANENT",
    employmentStartDate: "",
    workLocation: "",
  });

  useEffect(() => {
    lookupService.departments()
      .then(setDepartments)
      .catch((e) => setError(e.message));

    lookupService.designations()
      .then(setDesignations)
      .catch((e) => setError(e.message));
  }, []);

  function update(field, value) {
    setForm((previous) => ({
      ...previous,
      [field]: value,
    }));

    setError("");
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");

    const validationError = validateEmployee(form);

    if (validationError) {
      setError(validationError);
      return;
    }

    if (!form.departmentId || !form.designationId) {
      setError("Please select a Department and Designation.");
      return;
    }

    setSaving(true);

    try {
      await employeeService.create({
        ...form,
        phoneNumber: form.phoneNumber.trim(),
        departmentId: Number(form.departmentId),
        designationId: Number(form.designationId),
      });

      onSaved();
    } catch (err) {
      setError(err.message || "Failed to save employee.");
    } finally {
      setSaving(false);
    }
  }

  const today = getTodayDate();

  return (
    <Modal title="Add Employee" onClose={onClose}>
      <ErrorBanner message={error} />

      <form onSubmit={handleSubmit}>
        <div className="form-row">
          <div className="form-field">
            <label htmlFor="employeeNumber">Employee Number</label>
            <input
              id="employeeNumber"
              required
              value={form.employeeNumber}
              onChange={(e) =>
                update("employeeNumber", e.target.value)
              }
            />
          </div>

          <div className="form-field">
            <label htmlFor="employmentType">Employment Type</label>
            <select
              id="employmentType"
              value={form.employmentType}
              onChange={(e) =>
                update("employmentType", e.target.value)
              }
            >
              <option value="PERMANENT">Permanent</option>
              <option value="CONTRACT">Contract</option>
            </select>
          </div>
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="firstName">First Name</label>
            <input
              id="firstName"
              required
              value={form.firstName}
              onChange={(e) =>
                update("firstName", e.target.value)
              }
            />
          </div>

          <div className="form-field">
            <label htmlFor="lastName">Last Name</label>
            <input
              id="lastName"
              required
              value={form.lastName}
              onChange={(e) =>
                update("lastName", e.target.value)
              }
            />
          </div>
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="dateOfBirth">Date of Birth</label>
            <input
              id="dateOfBirth"
              type="date"
              required
              max={today}
              value={form.dateOfBirth}
              onChange={(e) =>
                update("dateOfBirth", e.target.value)
              }
            />
          </div>

          <div className="form-field">
            <label htmlFor="gender">Gender</label>
            <select
              id="gender"
              value={form.gender}
              onChange={(e) => update("gender", e.target.value)}
            >
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
        </div>

        <div className="form-field">
          <label htmlFor="phoneNumber">Phone Number</label>
          <input
            id="phoneNumber"
            type="tel"
            required
            maxLength={16}
            placeholder="0712345678"
            value={form.phoneNumber}
            onChange={(e) =>
              update("phoneNumber", e.target.value)
            }
          />
          <small>
            Enter 10 digits, e.g. 0712345678, or +94712345678.
          </small>
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="departmentId">Department</label>
            <select
              id="departmentId"
              required
              value={form.departmentId}
              onChange={(e) =>
                update("departmentId", e.target.value)
              }
            >
              <option value="">Select...</option>
              {departments.map((department) => (
                <option key={department.id} value={department.id}>
                  {department.name}
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label htmlFor="designationId">Designation</label>
            <select
              id="designationId"
              required
              value={form.designationId}
              onChange={(e) =>
                update("designationId", e.target.value)
              }
            >
              <option value="">Select...</option>
              {designations.map((designation) => (
                <option key={designation.id} value={designation.id}>
                  {designation.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="form-field">
          <label htmlFor="employmentStartDate">
            Employment Start Date
          </label>
          <input
            id="employmentStartDate"
            type="date"
            required
            min={form.dateOfBirth || undefined}
            max={today}
            value={form.employmentStartDate}
            onChange={(e) =>
              update("employmentStartDate", e.target.value)
            }
          />
        </div>

        <div className="form-field">
          <label htmlFor="workLocation">Work Location</label>
          <input
            id="workLocation"
            value={form.workLocation}
            onChange={(e) =>
              update("workLocation", e.target.value)
            }
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

          <button type="submit" className="btn" disabled={saving}>
            {saving ? "Saving..." : "Save Employee"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
