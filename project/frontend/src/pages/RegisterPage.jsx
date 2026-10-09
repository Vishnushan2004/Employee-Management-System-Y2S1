import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { register } from "../services/authService";
import { ErrorBanner } from "../components/Common";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;
const PHONE_RE = /^[0-9+\-\s]{7,20}$/;

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: "",
    email: "",
    phoneNumber: "",
    username: "",
    password: "",
    confirmPassword: "",
  });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function validate() {
    if (!form.name.trim()) return "Name is required.";
    if (!EMAIL_RE.test(form.email)) return "Enter a valid email address.";
    if (!PHONE_RE.test(form.phoneNumber)) return "Enter a valid phone number.";
    if (!form.username.trim() || form.username.trim().length < 3) return "Username must be at least 3 characters.";
    if (!PASSWORD_RE.test(form.password)) return "Password must be at least 8 characters and include a letter and a number.";
    if (form.password !== form.confirmPassword) return "Passwords do not match.";
    return "";
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      await register({
        name: form.name.trim(),
        email: form.email.trim(),
        phoneNumber: form.phoneNumber.trim(),
        username: form.username.trim(),
        password: form.password,
      });
      setSuccess(true);
      setTimeout(() => navigate("/login", { replace: true }), 1500);
    } catch (err) {
      setError(err.message || "Registration failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-brand">
          <div className="login-brand-title">Create a Job Applicant Account</div>
          <div className="login-brand-subtitle">Browse vacancies and apply online</div>
        </div>

        <ErrorBanner message={error} />
        {success && <div className="form-error" style={{ color: "var(--color-success, green)" }}>
          Account created. Redirecting to login…
        </div>}

        <form onSubmit={handleSubmit}>
          <div className="form-field">
            <label htmlFor="name">Full name</label>
            <input id="name" type="text" value={form.name} onChange={(e) => update("name", e.target.value)} required />
          </div>
          <div className="form-field">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" value={form.email} onChange={(e) => update("email", e.target.value)} required />
          </div>
          <div className="form-field">
            <label htmlFor="phoneNumber">Phone number</label>
            <input id="phoneNumber" type="text" value={form.phoneNumber} onChange={(e) => update("phoneNumber", e.target.value)} required />
          </div>
          <div className="form-field">
            <label htmlFor="username">Username</label>
            <input id="username" type="text" value={form.username} onChange={(e) => update("username", e.target.value)} required />
          </div>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="password">Password</label>
              <input id="password" type="password" value={form.password} onChange={(e) => update("password", e.target.value)} required />
            </div>
            <div className="form-field">
              <label htmlFor="confirmPassword">Confirm password</label>
              <input id="confirmPassword" type="password" value={form.confirmPassword} onChange={(e) => update("confirmPassword", e.target.value)} required />
            </div>
          </div>
          <button className="btn" type="submit" disabled={submitting} style={{ width: "100%", justifyContent: "center" }}>
            {submitting ? "Creating account..." : "Create account"}
          </button>
        </form>

        <div className="login-hint">
          Already have an account? <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  );
}
