import { useEffect, useState } from "react";
import { applicantService } from "../services/applicantService";
import { LoadingState, EmptyState, ErrorBanner, StatusBadge } from "../components/Common";

export default function MyApplicationsPage() {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [interviews, setInterviews] = useState({});

  useEffect(() => {
    applicantService
      .listMyApplications()
      .then(async (apps) => {
        setApplications(apps);
        const interviewed = apps.filter((a) => a.applicationStatus === "INTERVIEWED" || a.applicationStatus === "OFFERED" || a.applicationStatus === "HIRED");
        const entries = await Promise.all(
          interviewed.map(async (a) => {
            try {
              const iv = await applicantService.getInterview(a.applicationId);
              return [a.applicationId, iv];
            } catch {
              return [a.applicationId, null];
            }
          })
        );
        setInterviews(Object.fromEntries(entries));
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingState />;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">My Applications</h1>
          <div className="page-subtitle">Track the status of every job you've applied to</div>
        </div>
      </div>

      <ErrorBanner message={error} />
      {applications.length === 0 && <EmptyState label="You haven't applied to any vacancies yet." />}

      {applications.length > 0 && (
        <table className="data-table">
          <thead>
            <tr>
              <th>Job Title</th>
              <th>Applied</th>
              <th>Status</th>
              <th>CV</th>
              <th>Interview</th>
            </tr>
          </thead>
          <tbody>
            {applications.map((a) => {
              const iv = interviews[a.applicationId];
              return (
                <tr key={a.applicationId}>
                  <td>{a.jobTitle}</td>
                  <td>{a.applicationDate}</td>
                  <td><StatusBadge status={a.applicationStatus} /></td>
                  <td>{a.cvResume ? "Uploaded" : "—"}</td>
                  <td>
                    {iv ? `${iv.interviewDate} ${iv.interviewTime} — ${iv.location || "TBD"}` : "—"}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}
