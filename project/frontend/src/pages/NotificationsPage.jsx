import { useEffect, useState } from "react";
import { notificationService } from "../services/miscServices";
import { LoadingState, EmptyState, ErrorBanner } from "../components/Common";

export default function NotificationsPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  function load() {
    setLoading(true);
    notificationService
      .list()
      .then(setItems)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function markRead(id) {
    try {
      await notificationService.markRead(id);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Notifications</h1>
        </div>
      </div>

      <ErrorBanner message={error} />
      {loading && <LoadingState />}
      {!loading && items.length === 0 && <EmptyState label="No notifications yet." />}

      {!loading && items.length > 0 && (
        <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          {items.map((n) => (
            <div
              key={n.notificationId}
              className="card"
              style={{ display: "flex", justifyContent: "space-between", alignItems: "center", opacity: n.status === "READ" ? 0.6 : 1 }}
            >
              <div>
                <div style={{ fontWeight: 600, fontSize: 13.5 }}>{n.notificationType.replaceAll("_", " ")}</div>
                <div style={{ fontSize: 13, color: "var(--color-text-muted)" }}>{n.message}</div>
              </div>
              {n.status === "UNREAD" && (
                <button className="btn btn-secondary" onClick={() => markRead(n.notificationId)}>Mark as read</button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
