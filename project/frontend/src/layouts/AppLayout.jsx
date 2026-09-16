import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { NAV_BY_ROLE, ROLE_LABELS } from "../utils/navConfig";

export default function AppLayout() {
  const { user, logout } = useAuth();
  const links = NAV_BY_ROLE[user?.role] || [];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-header">
          <div className="sidebar-title">Employee Management System</div>
          <div className="sidebar-subtitle">SLIIT · 2026-Y2-S1-NU-07</div>
        </div>
        <nav className="sidebar-nav">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) => "sidebar-link" + (isActive ? " active" : "")}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="main-column">
        <header className="topbar">
          <div className="topbar-role">{ROLE_LABELS[user?.role] || user?.role}</div>
          <div className="topbar-user">
            <span>{user?.username}</span>
            <button className="btn btn-secondary" onClick={logout}>Log out</button>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
