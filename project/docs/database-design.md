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
