import { useEffect, useState } from "react";
import { employeeService } from "../services/employeeService";
import { LoadingState, ErrorBanner } from "../components/Common";

export default function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);
  const [form, setForm] = useState({ phoneNumber: "", street: "", city: "", postalCode: "" });

  useEffect(() => {
    employeeService
      .getMe()
      .then((data) => {
        setProfile(data);
        setForm({
          phoneNumber: data.phoneNumber || "",
          street: data.street || "",
          city: data.city || "",
          postalCode: data.postalCode || "",
        });
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  async function handleSave(e) {
    e.preventDefault();
    setError("");
    setSaved(false);
    try {
      await employeeService.updateOwnProfile(form);
      setSaved(true);
    } catch (err) {
      setError(err.message);
    }
  }

  if (loading) return <LoadingState />;
  if (!profile) return <ErrorBanner message={error || "Profile not found."} />;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">My Profile</h1>
          <div className="page-subtitle">View your details. Only contact information can be edited here.</div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="form-row">
          <Field label="Employee Number" value={profile.employeeNumber} />
          <Field label="Full Name" value={profile.fullName} />
          <Field label="Department" value={profile.departmentName} />
          <Field label="Designation" value={profile.designationName} />
          <Field label="Employment Type" value={profile.employmentType} />
          <Field label="Employment Status" value={profile.employmentStatus} />
          <Field label="Employment Start Date" value={profile.employmentStartDate} />
          <Field label="Work Location" value={profile.workLocation} />
          <Field label="Annual Leave Balance" value={profile.annualLeaveBalance} />
          <Field label="Casual Leave Balance" value={profile.casualLeaveBalance} />
          <Field label="Sick Leave Balance" value={profile.sickLeaveBalance} />
        </div>
      </div>

      <div className="card">
        <h2 style={{ fontSize: 15, marginTop: 0 }}>Update Contact Details</h2>
        <ErrorBanner message={error} />
        {saved && <div style={{ color: "#1c6b4e", fontSize: 13, marginBottom: 12 }}>Profile updated.</div>}
        <form onSubmit={handleSave}>
          <div className="form-field">
            <label>Phone Number</label>
            <input value={form.phoneNumber} onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />
          </div>
          <div className="form-row">
            <div className="form-field">
              <label>Street</label>
              <input value={form.street} onChange={(e) => setForm({ ...form, street: e.target.value })} />
            </div>
            <div className="form-field">
              <label>City</label>
              <input value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
            </div>
            <div className="form-field">
              <label>Postal Code</label>
              <input value={form.postalCode} onChange={(e) => setForm({ ...form, postalCode: e.target.value })} />
            </div>
          </div>
          <button className="btn" type="submit">Save Changes</button>
        </form>
      </div>
    </div>
  );
}

function Field({ label, value }) {
  return (
    <div className="form-field">
      <label>{label}</label>
      <div style={{ padding: "8px 0" }}>{value ?? "-"}</div>
    </div>
  );
}
