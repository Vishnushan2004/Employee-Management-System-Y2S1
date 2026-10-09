import { useEffect, useState } from "react";
import { applicantService } from "../services/applicantService";
import { LoadingState, ErrorBanner } from "../components/Common";

export default function ApplicantProfilePage() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    applicantService
      .getProfile()
      .then((data) => {
        setProfile(data);
        setForm(data);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
    setSaved(false);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSaving(true);
    try {
      await applicantService.updateProfile({
        name: form.name,
        phoneNumber: form.phoneNumber,
        qualifications: form.qualifications,
        skills: form.skills,
        workExperience: form.workExperience,
      });
      setProfile(form);
      setSaved(true);
    } catch (err) {
      setError(err.message || "Could not save profile.");
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <LoadingState />;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">My Profile</h1>
          <div className="page-subtitle">Keep your details up to date before you apply</div>
        </div>
      </div>

      <ErrorBanner message={error} />
      {saved && <div className="form-error" style={{ color: "var(--color-success, green)" }}>Profile saved.</div>}

      {form && (
        <form className="card" onSubmit={handleSubmit} style={{ maxWidth: 560 }}>
          <div className="form-field">
            <label>Full name</label>
            <input value={form.name || ""} onChange={(e) => update("name", e.target.value)} required />
          </div>
          <div className="form-field">
            <label>Email</label>
            <input value={profile.email || ""} disabled />
          </div>
          <div className="form-field">
            <label>Phone number</label>
            <input value={form.phoneNumber || ""} onChange={(e) => update("phoneNumber", e.target.value)} required />
          </div>
          <div className="form-field">
            <label>Qualifications</label>
            <textarea rows={3} value={form.qualifications || ""} onChange={(e) => update("qualifications", e.target.value)} />
          </div>
          <div className="form-field">
            <label>Skills</label>
            <textarea rows={3} value={form.skills || ""} onChange={(e) => update("skills", e.target.value)} />
          </div>
          <div className="form-field">
            <label>Work experience</label>
            <textarea rows={3} value={form.workExperience || ""} onChange={(e) => update("workExperience", e.target.value)} />
          </div>
          <button className="btn" type="submit" disabled={saving}>
            {saving ? "Saving..." : "Save changes"}
          </button>
        </form>
      )}
    </div>
  );
}
