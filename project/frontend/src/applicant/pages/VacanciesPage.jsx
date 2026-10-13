import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { publicJobsService } from "../../services/applicantService";
import { LoadingState, EmptyState, ErrorBanner } from "../../components/Common";

export default function VacanciesPage() {
  const [vacancies, setVacancies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    publicJobsService
      .listOpenVacancies()
      .then(setVacancies)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Open Vacancies</h1>
          <div className="page-subtitle">Browse current openings and apply online</div>
        </div>
      </div>

      <ErrorBanner message={error} />
      {loading && <LoadingState />}
      {!loading && vacancies.length === 0 && <EmptyState label="No open vacancies right now." />}

      <div className="card-grid">
        {vacancies.map((v) => (
          <div className="card" key={v.vacancyId}>
            <h3 style={{ marginTop: 0 }}>{v.jobTitle}</h3>
            <p>{v.jobDescription}</p>
            {v.requiredQualifications && <p><strong>Qualifications:</strong> {v.requiredQualifications}</p>}
            {v.requiredSkills && <p><strong>Skills:</strong> {v.requiredSkills}</p>}
            <p className="page-subtitle">Closes {v.closingDate}</p>
            <Link className="btn" to={`/applicant/apply/${v.vacancyId}`}>Apply now</Link>
          </div>
        ))}
      </div>
    </div>
  );
}
