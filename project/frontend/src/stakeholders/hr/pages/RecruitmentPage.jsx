import { useEffect, useState } from "react";
import { recruitmentService } from "../../../services/moduleServices";
import { LoadingState, EmptyState, ErrorBanner, StatusBadge, Modal } from "../../../components/Common";

const TABS = ["Vacancies", "Applicants", "Applications", "Interviews"];

export default function RecruitmentPage() {
  const [tab, setTab] = useState("Vacancies");

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Recruitment</h1>
          <div className="page-subtitle">Vacancy → Applicant → Application → Interview</div>
        </div>
      </div>

      <div style={{ display: "flex", gap: 6, marginBottom: 18 }}>
        {TABS.map((t) => (
          <button
            key={t}
            className={tab === t ? "btn" : "btn btn-secondary"}
            onClick={() => setTab(t)}
          >
            {t}
          </button>
        ))}
      </div>

      {tab === "Vacancies" && <VacanciesTab />}
      {tab === "Applicants" && <ApplicantsTab />}
      {tab === "Applications" && <ApplicationsTab />}
      {tab === "Interviews" && <InterviewsTab />}
    </div>
  );
}

function VacanciesTab() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showNew, setShowNew] = useState(false);

  function load() {
    setLoading(true);
    recruitmentService.listVacancies().then(setItems).catch((e) => setError(e.message)).finally(() => setLoading(false));
  }
  useEffect(load, []);

  async function close(id) {
    try { await recruitmentService.updateVacancyStatus(id, "CLOSED"); load(); } catch (e) { setError(e.message); }
  }

  return (
    <div>
      <div style={{ textAlign: "right", marginBottom: 12 }}>
        <button className="btn" onClick={() => setShowNew(true)}>New Vacancy</button>
      </div>
      <ErrorBanner message={error} />
      {loading && <LoadingState />}
      {!loading && items.length === 0 && <EmptyState label="No vacancies yet." />}
      {!loading && items.length > 0 && (
        <table className="data-table">
          <thead><tr><th>Job Title</th><th>Opening</th><th>Closing</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {items.map((v) => (
              <tr key={v.vacancyId}>
                <td>{v.jobTitle}</td><td>{v.openingDate}</td><td>{v.closingDate}</td>
                <td><StatusBadge status={v.vacancyStatus} /></td>
                <td>{v.vacancyStatus === "OPEN" && <button className="btn btn-secondary" onClick={() => close(v.vacancyId)}>Close</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {showNew && (
        <Modal title="New Vacancy" onClose={() => setShowNew(false)}>
          <NewVacancyForm onSaved={() => { setShowNew(false); load(); }} onCancel={() => setShowNew(false)} />
        </Modal>
      )}
    </div>
  );
}

function NewVacancyForm({ onSaved, onCancel }) {
  const [form, setForm] = useState({ jobTitle: "", jobDescription: "", requiredQualifications: "", requiredSkills: "", openingDate: "", closingDate: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setSaving(true); setError("");
    try { await recruitmentService.createVacancy(form); onSaved(); } catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  return (
    <form onSubmit={submit}>
      <ErrorBanner message={error} />
      <div className="form-field"><label>Job Title</label><input required value={form.jobTitle} onChange={(e) => setForm({ ...form, jobTitle: e.target.value })} /></div>
      <div className="form-field"><label>Description</label><textarea rows={2} value={form.jobDescription} onChange={(e) => setForm({ ...form, jobDescription: e.target.value })} /></div>
      <div className="form-field"><label>Required Qualifications</label><input value={form.requiredQualifications} onChange={(e) => setForm({ ...form, requiredQualifications: e.target.value })} /></div>
      <div className="form-field"><label>Required Skills</label><input value={form.requiredSkills} onChange={(e) => setForm({ ...form, requiredSkills: e.target.value })} /></div>
      <div className="form-row">
        <div className="form-field"><label>Opening Date</label><input type="date" required value={form.openingDate} onChange={(e) => setForm({ ...form, openingDate: e.target.value })} /></div>
        <div className="form-field"><label>Closing Date</label><input type="date" required value={form.closingDate} onChange={(e) => setForm({ ...form, closingDate: e.target.value })} /></div>
      </div>
      <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Vacancy"}</button>
      </div>
    </form>
  );
}

function ApplicantsTab() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showNew, setShowNew] = useState(false);

  function load() {
    setLoading(true);
    recruitmentService.listApplicants().then(setItems).catch((e) => setError(e.message)).finally(() => setLoading(false));
  }
  useEffect(load, []);

  return (
    <div>
      <div style={{ textAlign: "right", marginBottom: 12 }}>
        <button className="btn" onClick={() => setShowNew(true)}>Add Applicant</button>
      </div>
      <ErrorBanner message={error} />
      {loading && <LoadingState />}
      {!loading && items.length === 0 && <EmptyState label="No applicants yet." />}
      {!loading && items.length > 0 && (
        <table className="data-table">
          <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Qualifications</th></tr></thead>
          <tbody>
            {items.map((a) => (
              <tr key={a.applicantId}><td>{a.name}</td><td>{a.email}</td><td>{a.phoneNumber}</td><td>{a.qualifications}</td></tr>
            ))}
          </tbody>
        </table>
      )}
      {showNew && (
        <Modal title="Add Applicant" onClose={() => setShowNew(false)}>
          <NewApplicantForm onSaved={() => { setShowNew(false); load(); }} onCancel={() => setShowNew(false)} />
        </Modal>
      )}
    </div>
  );
}

function NewApplicantForm({ onSaved, onCancel }) {
  const [form, setForm] = useState({ name: "", email: "", phoneNumber: "", qualifications: "", skills: "", workExperience: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setSaving(true); setError("");
    try { await recruitmentService.createApplicant(form); onSaved(); } catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  return (
    <form onSubmit={submit}>
      <ErrorBanner message={error} />
      <div className="form-field"><label>Name</label><input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
      <div className="form-row">
        <div className="form-field"><label>Email</label><input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></div>
        <div className="form-field"><label>Phone</label><input required value={form.phoneNumber} onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} /></div>
      </div>
      <div className="form-field"><label>Qualifications</label><input value={form.qualifications} onChange={(e) => setForm({ ...form, qualifications: e.target.value })} /></div>
      <div className="form-field"><label>Skills</label><input value={form.skills} onChange={(e) => setForm({ ...form, skills: e.target.value })} /></div>
      <div className="form-field"><label>Work Experience</label><textarea rows={2} value={form.workExperience} onChange={(e) => setForm({ ...form, workExperience: e.target.value })} /></div>
      <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Applicant"}</button>
      </div>
    </form>
  );
}

function ApplicationsTab() {
  const [items, setItems] = useState([]);
  const [applicants, setApplicants] = useState([]);
  const [vacancies, setVacancies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showNew, setShowNew] = useState(false);

  function load() {
    setLoading(true);
    Promise.all([recruitmentService.listApplications(), recruitmentService.listApplicants(), recruitmentService.listVacancies()])
      .then(([apps, apts, vacs]) => { setItems(apps); setApplicants(apts); setVacancies(vacs); })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  const STATUSES = ["SUBMITTED", "SHORTLISTED", "INTERVIEWED", "OFFERED", "REJECTED", "HIRED"];

  async function updateStatus(id, status) {
    try { await recruitmentService.updateApplicationStatus(id, status); load(); } catch (e) { setError(e.message); }
  }

  async function viewCv(id, cvResume) {
    if (!cvResume) { setError("This applicant has not uploaded a CV yet."); return; }
    try {
      const { filename, fileContentBase64 } = await recruitmentService.downloadCv(id);
      const byteChars = atob(fileContentBase64);
      const bytes = new Uint8Array(byteChars.length);
      for (let i = 0; i < byteChars.length; i++) bytes[i] = byteChars.charCodeAt(i);
      const blob = new Blob([bytes], { type: "application/pdf" });
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener,noreferrer");
      setTimeout(() => URL.revokeObjectURL(url), 30000);
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <div>
      <div style={{ textAlign: "right", marginBottom: 12 }}>
        <button className="btn" onClick={() => setShowNew(true)}>New Application</button>
      </div>
      <ErrorBanner message={error} />
      {loading && <LoadingState />}
      {!loading && items.length === 0 && <EmptyState label="No applications yet." />}
      {!loading && items.length > 0 && (
        <table className="data-table">
          <thead><tr><th>Applicant</th><th>Vacancy</th><th>Date</th><th>Expected Salary</th><th>CV</th><th>Status</th></tr></thead>
          <tbody>
            {items.map((a) => (
              <tr key={a.applicationId}>
                <td>{a.applicantName}</td><td>{a.jobTitle}</td><td>{a.applicationDate}</td>
                <td>{a.expectedSalary != null ? Number(a.expectedSalary).toLocaleString() : "-"}</td>
                <td>
                  {a.cvResume
                    ? <button className="btn btn-secondary" onClick={() => viewCv(a.applicationId, a.cvResume)}>View CV</button>
                    : <span className="page-subtitle">Not uploaded</span>}
                </td>
                <td>
                  <select value={a.applicationStatus} onChange={(e) => updateStatus(a.applicationId, e.target.value)}>
                    {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {showNew && (
        <Modal title="New Application" onClose={() => setShowNew(false)}>
          <NewApplicationForm applicants={applicants} vacancies={vacancies} onSaved={() => { setShowNew(false); load(); }} onCancel={() => setShowNew(false)} />
        </Modal>
      )}
    </div>
  );
}

function NewApplicationForm({ applicants, vacancies, onSaved, onCancel }) {
  const [form, setForm] = useState({ applicantId: "", vacancyId: "", cvResume: "", coverLetter: "", expectedSalary: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setSaving(true); setError("");
    try {
      await recruitmentService.createApplication({
        ...form,
        applicantId: Number(form.applicantId),
        vacancyId: Number(form.vacancyId),
        expectedSalary: form.expectedSalary ? Number(form.expectedSalary) : null,
      });
      onSaved();
    } catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  return (
    <form onSubmit={submit}>
      <ErrorBanner message={error} />
      <div className="form-field">
        <label>Applicant</label>
        <select required value={form.applicantId} onChange={(e) => setForm({ ...form, applicantId: e.target.value })}>
          <option value="">Select...</option>
          {applicants.map((a) => <option key={a.applicantId} value={a.applicantId}>{a.name}</option>)}
        </select>
      </div>
      <div className="form-field">
        <label>Vacancy</label>
        <select required value={form.vacancyId} onChange={(e) => setForm({ ...form, vacancyId: e.target.value })}>
          <option value="">Select...</option>
          {vacancies.filter((v) => v.vacancyStatus === "OPEN").map((v) => <option key={v.vacancyId} value={v.vacancyId}>{v.jobTitle}</option>)}
        </select>
      </div>
      <div className="form-field"><label>Expected Salary</label><input type="number" value={form.expectedSalary} onChange={(e) => setForm({ ...form, expectedSalary: e.target.value })} /></div>
      <div className="form-field"><label>CV / Resume Reference</label><input value={form.cvResume} onChange={(e) => setForm({ ...form, cvResume: e.target.value })} /></div>
      <div className="form-field"><label>Cover Letter</label><textarea rows={2} value={form.coverLetter} onChange={(e) => setForm({ ...form, coverLetter: e.target.value })} /></div>
      <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Save Application"}</button>
      </div>
    </form>
  );
}

function InterviewsTab() {
  const [items, setItems] = useState([]);
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showNew, setShowNew] = useState(false);

  function load() {
    setLoading(true);
    Promise.all([recruitmentService.listInterviews(), recruitmentService.listApplications()])
      .then(([ivs, apps]) => { setItems(ivs); setApplications(apps); })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  async function recordResult(id, result) {
    try { await recruitmentService.recordInterviewResult(id, result, ""); load(); } catch (e) { setError(e.message); }
  }

  return (
    <div>
      <div style={{ textAlign: "right", marginBottom: 12 }}>
        <button className="btn" onClick={() => setShowNew(true)}>Schedule Interview</button>
      </div>
      <ErrorBanner message={error} />
      {loading && <LoadingState />}
      {!loading && items.length === 0 && <EmptyState label="No interviews scheduled yet." />}
      {!loading && items.length > 0 && (
        <table className="data-table">
          <thead><tr><th>Applicant</th><th>Vacancy</th><th>Date</th><th>Time</th><th>Location</th><th>Result</th></tr></thead>
          <tbody>
            {items.map((i) => (
              <tr key={i.interviewId}>
                <td>{i.applicantName}</td><td>{i.jobTitle}</td><td>{i.interviewDate}</td><td>{i.interviewTime}</td><td>{i.location}</td>
                <td>
                  <select value={i.result || "PENDING"} onChange={(e) => recordResult(i.interviewId, e.target.value)}>
                    <option value="PENDING">Pending</option><option value="PASSED">Passed</option><option value="FAILED">Failed</option>
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {showNew && (
        <Modal title="Schedule Interview" onClose={() => setShowNew(false)}>
          <NewInterviewForm applications={applications} onSaved={() => { setShowNew(false); load(); }} onCancel={() => setShowNew(false)} />
        </Modal>
      )}
    </div>
  );
}

function NewInterviewForm({ applications, onSaved, onCancel }) {
  const [form, setForm] = useState({ applicationId: "", interviewDate: "", interviewTime: "", location: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setSaving(true); setError("");
    try {
      await recruitmentService.scheduleInterview({ ...form, applicationId: Number(form.applicationId) });
      onSaved();
    } catch (err) { setError(err.message); } finally { setSaving(false); }
  }

  return (
    <form onSubmit={submit}>
      <ErrorBanner message={error} />
      <div className="form-field">
        <label>Application</label>
        <select required value={form.applicationId} onChange={(e) => setForm({ ...form, applicationId: e.target.value })}>
          <option value="">Select...</option>
          {applications.map((a) => <option key={a.applicationId} value={a.applicationId}>{a.applicantName} - {a.jobTitle}</option>)}
        </select>
      </div>
      <div className="form-row">
        <div className="form-field"><label>Date</label><input type="date" required value={form.interviewDate} onChange={(e) => setForm({ ...form, interviewDate: e.target.value })} /></div>
        <div className="form-field"><label>Time</label><input type="time" required value={form.interviewTime} onChange={(e) => setForm({ ...form, interviewTime: e.target.value })} /></div>
      </div>
      <div className="form-field"><label>Location</label><input value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} /></div>
      <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn" disabled={saving}>{saving ? "Saving..." : "Schedule"}</button>
      </div>
    </form>
  );
}
