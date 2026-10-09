import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { ErrorBanner } from "../components/Common";

export default function LoginPage() {
  const { login, loading } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    try {
      await login(username, password);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(err.message || "Login failed. Please try again.");
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-brand">
          <div className="login-brand-title">Employee Management System</div>
          <div className="login-brand-subtitle">SLIIT · Group 2026-Y2-S1-NU-07</div>
        </div>

        <ErrorBanner message={error} />

        <form onSubmit={handleSubmit}>
          <div className="form-field">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
            />
          </div>
          <div className="form-field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </div>
          <button className="btn" type="submit" disabled={loading} style={{ width: "100%", justifyContent: "center" }}>
            {loading ? "Signing in..." : "Sign in"}
          </button>
        </form>

        <div className="login-hint">
          Demo accounts (password: <strong>Pass@123</strong>): nadeesha.hr, imesha.finance,
          chamara.ops, kavindu.it, ravindu.emp, tharushi.admin, amali.applicant
        </div>
        <div className="login-hint">
          Looking for a job? <Link to="/register">Create an applicant account</Link>
        </div>
      </div>
    </div>
  );
}
