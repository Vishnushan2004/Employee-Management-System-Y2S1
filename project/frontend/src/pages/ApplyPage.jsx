import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { applicantService } from "../services/applicantService";
import { ErrorBanner } from "../components/Common";

const MAX_CV_BYTES = 5 * 1024 * 1024; // 5MB, mirrors backend limit

function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result.split(",")[1]);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

export default function ApplyPage() {
  const { vacancyId } = useParams();
  const navigate = useNavigate();
  const [coverLetter, setCoverLetter] = useState("");
  const [expectedSalary, setExpectedSalary] = useState("");
  const [cvFile, setCvFile] = useState(null);
  const [cvError, setCvError] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function handleFileChange(e) {
    const file = e.target.files[0];
    setCvFile(null);
    setCvError("");
    if (!file) return;
    if (!file.name.toLowerCase().endsWith(".pdf")) {
      setCvError("CV must be a PDF file.");
      return;
    }
    if (file.size > MAX_CV_BYTES) {
      setCvError("CV must be 5MB or smaller.");
      return;
    }
    setCvFile(file);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (expectedSalary && Number(expectedSalary) < 0) {
      setError("Expected salary cannot be negative.");
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      const { applicationId } = await applicantService.apply({
        vacancyId: Number(vacancyId),
        coverLetter: coverLetter || null,
        expectedSalary: expectedSalary ? Number(expectedSalary) : null,
      });

      if (cvFile) {
        const base64 = await fileToBase64(cvFile);
        await applicantService.uploadCv(applicationId, cvFile.name, base64);
      }

      navigate("/applicant/applications", { replace: true });
    } catch (err) {
      setError(err.message || "Could not submit application.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Apply for this role</h1>
          <div className="page-subtitle">Cover letter and CV are optional but recommended</div>
        </div>
      </div>

      <ErrorBanner message={error} />

      <form className="card" onSubmit={handleSubmit} style={{ maxWidth: 560 }}>
        <div className="form-field">
          <label>Cover letter</label>
          <textarea rows={5} maxLength={1000} value={coverLetter} onChange={(e) => setCoverLetter(e.target.value)} />
        </div>
        <div className="form-field">
          <label>Expected salary (optional)</label>
          <input type="number" min="0" step="0.01" value={expectedSalary} onChange={(e) => setExpectedSalary(e.target.value)} />
        </div>
        <div className="form-field">
          <label>CV (PDF only, max 5MB)</label>
          <input type="file" accept=".pdf,application/pdf" onChange={handleFileChange} />
          {cvFile && <div className="page-subtitle">{cvFile.name}</div>}
          <ErrorBanner message={cvError} />
        </div>
        <button className="btn" type="submit" disabled={submitting || !!cvError}>
          {submitting ? "Submitting..." : "Submit application"}
        </button>
      </form>
    </div>
  );
}
