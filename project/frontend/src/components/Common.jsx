export function StatCard({ label, value }) {
  return (
    <div className="card stat-card">
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}

const STATUS_BADGE = {
  APPROVED: "badge-success",
  PAID: "badge-success",
  ACTIVE: "badge-success",
  PRESENT: "badge-success",
  PENDING: "badge-warning",
  DRAFT: "badge-warning",
  LATE: "badge-warning",
  REJECTED: "badge-danger",
  ABSENT: "badge-danger",
  TERMINATED: "badge-danger",
  CANCELLED: "badge-neutral",
  ON_LEAVE: "badge-info",
};

export function StatusBadge({ status }) {
  const cls = STATUS_BADGE[status] || "badge-neutral";
  return <span className={`badge ${cls}`}>{status}</span>;
}

export function LoadingState({ label = "Loading..." }) {
  return <div className="loading-state">{label}</div>;
}

export function EmptyState({ label }) {
  return <div className="empty-state">{label}</div>;
}

export function ErrorBanner({ message }) {
  if (!message) return null;
  return <div className="form-error">{message}</div>;
}

export function Modal({ title, children, onClose }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-title">{title}</div>
        {children}
      </div>
    </div>
  );
}
