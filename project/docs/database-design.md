# Database Design Notes — Group 2026-Y2-S1-NU-07

## Source of truth
This schema implements the EER diagram submitted for IT2140
(`2026-Y2-S1-NU-07_Assignment01_EER-1.pdf`), with three additions agreed
by the group after cross-checking it against the requirement documents
and proposal. No other entity, relationship, or cardinality was changed.

## Additions made (with justification)

| Entity | Why it was added | Relationship | Cardinality / Participation |
|---|---|---|---|
| `LEAVE_REQUEST` | Leave Management is a detailed, explicit requirement (HR, Operations Manager, Administrative Officer, and Employee Representative all describe it), but no entity existed for it in the submitted EER. | `EMPLOYEE (1) ---- (N) LEAVE_REQUEST` | Employee: partial (not every employee has leave requests at a given time). LEAVE_REQUEST: total (every leave request belongs to exactly one employee). |
| `INTERVIEW` | Interview scheduling and candidate tracking is named explicitly in the HR Manager's requirements and the proposal ("Schedule interviews and track candidate status"), but was not modeled past `APPLICATION`. | `APPLICATION (1) ---- (N) INTERVIEW` | Application: partial (not all applications reach interview stage). Interview: total. |
| `ALLOWANCE` | Payroll allowances are named in the Finance & Payroll Manager's requirements and the Gross Salary formula (`Basic + Allowances + Overtime`), but `PAYROLL` only linked to `DEDUCTION` in the submitted EER. | `PAYROLL (1) ---- (N) ALLOWANCE` | Mirrors the existing `PAYROLL (1) ---- (N) DEDUCTION` relationship exactly. |

## Design choices kept deliberately simple
- **Leave balance** is tracked as three running-balance columns on `EMPLOYEE`
  (`Annual_Leave_Balance`, `Casual_Leave_Balance`, `Sick_Leave_Balance`)
  rather than a separate balance entity — avoids inventing an entity the
  EER didn't call for, while still satisfying "View Leave Balance".
- **Age** is a derived attribute (per the EER) and is intentionally *not*
  stored — compute it from `Date_of_Birth` in application code or a view.
- **ISA specialization** (`EMPLOYEE` → `PERMANENT_EMPLOYEE` / `CONTRACT_EMPLOYEE`)
  is disjoint and total: every employee row must have a matching row in
  exactly one of the two subclass tables. This is enforced by the
  `Employment_Type` check constraint plus service-layer validation on
  insert (SQL Server doesn't have a native "exactly one of" constraint
  across tables without triggers, which we're deliberately avoiding to
  keep the schema simple for a Year 2 project).
- **Recursive Supervises relationship** is a self-referencing FK on
  `EMPLOYEE` (`Supervisor_Employee_ID`): 0..1 supervisor per employee,
  N subordinates per supervisor.
- **ROLE M:N PERMISSION** is resolved with a standard `ROLE_PERMISSION`
  junction table (not shown as a separate box in the EER diagram itself,
  since M:N relationships are drawn as diamonds conceptually, but it
  requires a junction table in the relational schema).

## Files
- `database/schema.sql` — full DDL (tables, PKs, FKs, CHECK constraints, indexes)
- `database/seed.sql` — sample data for every module + 6 demo role accounts

## Demo accounts (for viva — see seed.sql)
All demo accounts use password `Pass@123` in the local dev environment.
`Password_Hash` values in `seed.sql` are placeholders — the Java backend
will generate real BCrypt hashes when accounts are created through the
API; never commit real hashes or passwords to seed scripts in a real
system.

| Username | Role | Employee |
|---|---|---|
| nadeesha.hr | HR_MANAGER | Nadeesha Perera |
| imesha.finance | FINANCE_PAYROLL_MANAGER | Imesha Wickramasinghe |
| chamara.ops | OPERATIONS_MANAGER | Chamara Wijesinghe |
| kavindu.it | IT_COORDINATOR | Kavindu Senanayake |
| ravindu.emp | EMPLOYEE | Ravindu Gunawardena |
| tharushi.admin | ADMINISTRATIVE_OFFICER | Tharushi Fernando |
| amali.applicant | OUTSIDE_USER | (none - linked via Applicant_ID 1, Amali Rathnayake) |

## Addendum: Outside-user / job-applicant portal (added after initial submission)

A later requirement asked for a public-facing portal so job applicants
("outside users" with no EMPLOYEE record) can register, browse open
vacancies, apply, upload a CV, and track their application/interview status
without staff creating their account for them. This is **additive to the
original EER**, not a redesign:

- **`USER_ACCOUNT.Employee_ID`** was relaxed from `NOT NULL` to `NULL`-able.
- **`USER_ACCOUNT.Applicant_ID`** (new) is a nullable, `UNIQUE` FK to
  `APPLICANT(Applicant_ID)`, mirroring the existing `Employee_ID` FK.
- **`CK_UserAccount_ExactlyOneOwner`** (new CHECK constraint) enforces that
  every `USER_ACCOUNT` row has *exactly one* of `Employee_ID` / `Applicant_ID`
  set - never both, never neither. A login still belongs to exactly one
  real-world person, it just may now be a staff member *or* an applicant.
- A new **`OUTSIDE_USER`** role was seeded in `ROLE`, scoped (in application
  code, via `Security.requireRole`) to: own applicant profile, open
  vacancies (read-only), own applications, own interview details, own
  notifications, own CV upload/download. It never sees other applicants'
  data, employee data, payroll, or any HR/admin screen - enforced server-side
  by explicit `Applicant_ID` ownership checks on every lookup, never by
  hiding frontend buttons alone.
- **CV storage decision**: `APPLICATION.CV_Resume VARCHAR(255)` is left
  unchanged. It was already just a filename/path string (the existing seed
  data stores values like `/cv/amali_rathnayake.pdf`), so storing the actual
  uploaded PDF's bytes on disk under `backend/uploads/cv/` (server-generated
  UUID filename, gitignored directory) and keeping only that generated
  filename string in this column is a minimal, schema-compatible choice -
  it avoids widening the column or adding a BLOB/VARBINARY column, and keeps
  the database holding only metadata as it already did. The upload endpoint
  validates the actual decoded byte content (PDF magic bytes, size cap),
  not just the client-supplied extension or MIME string.
- No other entity, relationship, or cardinality from the original EER was
  changed.
