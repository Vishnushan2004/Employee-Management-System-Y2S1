import { useEffect, useState } from "react";
import { useAuth } from "../../../context/AuthContext";
import { payrollService } from "../../../services/moduleServices";
import { employeeService } from "../../../services/employeeService";
import {
  LoadingState,
  EmptyState,
  ErrorBanner,
  StatusBadge,
  Modal,
  StatCard,
} from "../../../components/Common";

export default function PayrollPage() {
  const { user } = useAuth();

  const isFinance = user?.role === "FINANCE_PAYROLL_MANAGER";

  const [records, setRecords] = useState([]);
  const [summary, setSummary] = useState(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");

  const [showProcess, setShowProcess] = useState(false);

  async function load() {
    setLoading(true);
    setError("");

    try {
      const list = await payrollService.list();

      setRecords(Array.isArray(list) ? list : []);

      if (isFinance) {
        const sum = await payrollService.summary();
        setSummary(sum);
      }
    } catch (err) {
      setError(
        err?.message || "Unable to load payroll records."
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [isFinance]);

  async function handleVerify(id) {
    setActionError("");

    try {
      await payrollService.verify(id);
      await load();
    } catch (err) {
      setActionError(
        err?.message || "Unable to verify payroll."
      );
    }
  }

  async function handlePay(id) {
    setActionError("");

    try {
      await payrollService.markPaid(id);
      await load();
    } catch (err) {
      setActionError(
        err?.message || "Unable to mark payroll as paid."
      );
    }
  }

  return (
    <div>
      {/* =========================
          PAGE HEADER
      ========================== */}
      <div className="page-header">
        <div>
          <h1 className="page-title">
            {isFinance ? "Payroll" : "My Payslips"}
          </h1>

          <div className="page-subtitle">
            {isFinance
              ? "Process, verify and pay monthly payroll"
              : "View your payslip history"}
          </div>
        </div>

        {isFinance && (
          <button
            type="button"
            className="btn"
            onClick={() => {
              setActionError("");
              setShowProcess(true);
            }}
          >
            Process Payroll
          </button>
        )}
      </div>

      {/* =========================
          SUMMARY
      ========================== */}
      {isFinance && summary && (
        <div className="card-grid">
          <StatCard
            label="Payroll Records"
            value={summary.payrollCount ?? 0}
          />

          <StatCard
            label="Total Gross Salary"
            value={Number(
              summary.totalGross ?? 0
            ).toLocaleString()}
          />

          <StatCard
            label="Total Net Salary"
            value={Number(
              summary.totalNet ?? 0
            ).toLocaleString()}
          />
        </div>
      )}

      {/* =========================
          ERRORS
      ========================== */}
      <ErrorBanner
        message={error || actionError}
      />

      {/* =========================
          LOADING
      ========================== */}
      {loading && <LoadingState />}

      {/* =========================
          EMPTY
      ========================== */}
      {!loading && records.length === 0 && (
        <EmptyState label="No payroll records yet." />
      )}

      {/* =========================
          PAYROLL TABLE
      ========================== */}
      {!loading && records.length > 0 && (
        <table className="data-table">
          <thead>
            <tr>
              {isFinance && <th>Employee</th>}

              <th>Month/Year</th>
              <th>Basic</th>
              <th>Gross</th>
              <th>Net</th>
              <th>Status</th>

              {isFinance && <th>Action</th>}
            </tr>
          </thead>

          <tbody>
            {records.map((p) => (
              <tr key={p.payrollId}>
                {isFinance && (
                  <td>
                    {p.employeeName || "Unknown"}
                  </td>
                )}

                <td>
                  {p.payrollMonth}/{p.payrollYear}
                </td>

                <td>
                  {Number(
                    p.basicSalary ?? 0
                  ).toLocaleString()}
                </td>

                <td>
                  {Number(
                    p.grossSalary ?? 0
                  ).toLocaleString()}
                </td>

                <td>
                  {Number(
                    p.netSalary ?? 0
                  ).toLocaleString()}
                </td>

                <td>
                  <StatusBadge
                    status={p.payrollStatus}
                  />
                </td>

                {isFinance && (
                  <td
                    style={{
                      whiteSpace: "nowrap",
                    }}
                  >
                    {p.payrollStatus === "DRAFT" && (
                      <button
                        type="button"
                        className="btn btn-accent"
                        onClick={() =>
                          handleVerify(p.payrollId)
                        }
                      >
                        Verify
                      </button>
                    )}

                    {p.payrollStatus === "VERIFIED" && (
                      <button
                        type="button"
                        className="btn"
                        onClick={() =>
                          handlePay(p.payrollId)
                        }
                      >
                        Mark Paid
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* =========================
          PROCESS PAYROLL MODAL
      ========================== */}
      {showProcess && (
        <ProcessPayrollModal
          onClose={() => setShowProcess(false)}
          onSaved={async () => {
            setShowProcess(false);
            await load();
          }}
        />
      )}
    </div>
  );
}


/* ============================================================
   PROCESS PAYROLL MODAL
============================================================ */

function ProcessPayrollModal({
  onClose,
  onSaved,
}) {
  const [employees, setEmployees] = useState([]);

  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const now = new Date();

  const [form, setForm] = useState({
    employeeId: "",

    payrollMonth:
      now.getMonth() + 1,

    payrollYear:
      now.getFullYear(),

    basicSalary: "",

    overtimeRatePerHour: "",

    allowanceType: "Transport",

    allowanceAmount: "",

    deductionType:
      "EPF Employee Share",

    deductionAmount: "",
  });

  /* ==========================================================
     LOAD EMPLOYEES
  ========================================================== */

  useEffect(() => {
    async function loadEmployees() {
      try {
        const data = await employeeService.list();

        setEmployees(
          Array.isArray(data)
            ? data
            : []
        );
      } catch (err) {
        setError(
          err?.message ||
            "Unable to load employees."
        );
      }
    }

    loadEmployees();
  }, []);


  /* ==========================================================
     INPUT HANDLER
  ========================================================== */

  function updateField(field, value) {
    setForm((previous) => ({
      ...previous,
      [field]: value,
    }));
  }


  /* ==========================================================
     FORM VALIDATION
  ========================================================== */

  function validateForm() {
    if (!form.employeeId) {
      return "Please select an employee.";
    }

    if (!form.payrollMonth) {
      return "Please enter the payroll month.";
    }

    if (
      Number(form.payrollMonth) < 1 ||
      Number(form.payrollMonth) > 12
    ) {
      return "Payroll month must be between 1 and 12.";
    }

    if (!form.payrollYear) {
      return "Please enter the payroll year.";
    }

    if (
      Number(form.payrollYear) < 2000 ||
      Number(form.payrollYear) > 2100
    ) {
      return "Payroll year must be between 2000 and 2100.";
    }

    if (
      form.basicSalary === "" ||
      Number(form.basicSalary) < 0
    ) {
      return "Please enter a valid basic salary.";
    }

    if (
      form.overtimeRatePerHour !== "" &&
      Number(form.overtimeRatePerHour) < 0
    ) {
      return "Overtime rate cannot be negative.";
    }

    if (
      form.allowanceAmount !== "" &&
      Number(form.allowanceAmount) < 0
    ) {
      return "Allowance amount cannot be negative.";
    }

    if (
      form.deductionAmount !== "" &&
      Number(form.deductionAmount) < 0
    ) {
      return "Deduction amount cannot be negative.";
    }

    return "";
  }


  /* ==========================================================
     SUBMIT PAYROLL
  ========================================================== */

  async function handleSubmit(e) {
    e.preventDefault();

    setError("");

    const validationError =
      validateForm();

    if (validationError) {
      setError(validationError);
      return;
    }

    setSaving(true);

    try {
      /* ------------------------------------------------------
         Convert form values to correct Java-compatible types
      ------------------------------------------------------ */

      const employeeId =
        Number(form.employeeId);

      const payrollMonth =
        Number(form.payrollMonth);

      const payrollYear =
        Number(form.payrollYear);

      const basicSalary =
        Number(form.basicSalary);

      const overtimeRatePerHour =
        form.overtimeRatePerHour === ""
          ? 0
          : Number(form.overtimeRatePerHour);

      /* ------------------------------------------------------
         IMPORTANT:
         Backend expects allowances as a LIST
         ------------------------------------------------------ */

      const allowances = [];

      if (
        form.allowanceAmount !== "" &&
        Number(form.allowanceAmount) > 0
      ) {
        allowances.push({
          type: String(
            form.allowanceType || "Allowance"
          ).trim(),

          amount: Number(
            form.allowanceAmount
          ),
        });
      }


      /* ------------------------------------------------------
         Backend expects deductions as a LIST
         ------------------------------------------------------ */

      const deductions = [];

      if (
        form.deductionAmount !== "" &&
        Number(form.deductionAmount) > 0
      ) {
        deductions.push({
          type: String(
            form.deductionType || "Deduction"
          ).trim(),

          amount: Number(
            form.deductionAmount
          ),
        });
      }


      /* ------------------------------------------------------
         FINAL PAYLOAD
      ------------------------------------------------------ */

      const payload = {
        employeeId,

        payrollMonth,

        payrollYear,

        basicSalary,

        overtimeRatePerHour,

        allowances,

        deductions,
      };


      /* ------------------------------------------------------
         DEBUG
         Open browser console and verify this object.
      ------------------------------------------------------ */

      console.log(
        "PAYROLL REQUEST:",
        JSON.stringify(
          payload,
          null,
          2
        )
      );


      /* ------------------------------------------------------
         SEND TO JAVA BACKEND
      ------------------------------------------------------ */

      await payrollService.process(
        payload
      );

      /* ------------------------------------------------------
         SUCCESS
      ------------------------------------------------------ */

      await onSaved();

    } catch (err) {
      console.error(
        "PAYROLL PROCESS ERROR:",
        err
      );

      setError(
        err?.message ||
          "Unable to process payroll."
      );

    } finally {
      setSaving(false);
    }
  }


  /* ==========================================================
     UI
  ========================================================== */

  return (
    <Modal
      title="Process Payroll"
      onClose={onClose}
    >
      <ErrorBanner message={error} />

      <form onSubmit={handleSubmit}>

        {/* EMPLOYEE */}
        <div className="form-field">
          <label>
            Employee
          </label>

          <select
            required
            value={form.employeeId}
            onChange={(e) =>
              updateField(
                "employeeId",
                e.target.value
              )
            }
          >
            <option value="">
              Select...
            </option>

            {employees.map((employee) => (
              <option
                key={employee.employeeId}
                value={employee.employeeId}
              >
                {employee.fullName} (
                {employee.employeeNumber}
                )
              </option>
            ))}
          </select>
        </div>


        {/* MONTH / YEAR */}
        <div className="form-row">

          <div className="form-field">
            <label>
              Month
            </label>

            <input
              type="number"
              min="1"
              max="12"
              required
              value={form.payrollMonth}
              onChange={(e) =>
                updateField(
                  "payrollMonth",
                  e.target.value
                )
              }
            />
          </div>


          <div className="form-field">
            <label>
              Year
            </label>

            <input
              type="number"
              min="2000"
              max="2100"
              required
              value={form.payrollYear}
              onChange={(e) =>
                updateField(
                  "payrollYear",
                  e.target.value
                )
              }
            />
          </div>

        </div>


        {/* BASIC / OVERTIME */}
        <div className="form-row">

          <div className="form-field">
            <label>
              Basic Salary
            </label>

            <input
              type="number"
              min="0"
              step="0.01"
              required
              value={form.basicSalary}
              onChange={(e) =>
                updateField(
                  "basicSalary",
                  e.target.value
                )
              }
            />
          </div>


          <div className="form-field">
            <label>
              Overtime Rate / hr
            </label>

            <input
              type="number"
              min="0"
              step="0.01"
              value={
                form.overtimeRatePerHour
              }
              onChange={(e) =>
                updateField(
                  "overtimeRatePerHour",
                  e.target.value
                )
              }
            />
          </div>

        </div>


        {/* ALLOWANCE */}
        <div className="form-row">

          <div className="form-field">
            <label>
              Allowance Type
            </label>

            <input
              value={
                form.allowanceType
              }
              onChange={(e) =>
                updateField(
                  "allowanceType",
                  e.target.value
                )
              }
            />
          </div>


          <div className="form-field">
            <label>
              Allowance Amount
            </label>

            <input
              type="number"
              min="0"
              step="0.01"
              value={
                form.allowanceAmount
              }
              onChange={(e) =>
                updateField(
                  "allowanceAmount",
                  e.target.value
                )
              }
            />
          </div>

        </div>


        {/* DEDUCTION */}
        <div className="form-row">

          <div className="form-field">
            <label>
              Deduction Type
            </label>

            <input
              value={
                form.deductionType
              }
              onChange={(e) =>
                updateField(
                  "deductionType",
                  e.target.value
                )
              }
            />
          </div>


          <div className="form-field">
            <label>
              Deduction Amount
            </label>

            <input
              type="number"
              min="0"
              step="0.01"
              value={
                form.deductionAmount
              }
              onChange={(e) =>
                updateField(
                  "deductionAmount",
                  e.target.value
                )
              }
            />
          </div>

        </div>


        {/* BUTTONS */}
        <div
          style={{
            display: "flex",
            gap: 10,
            justifyContent: "flex-end",
          }}
        >

          <button
            type="button"
            className="btn btn-secondary"
            onClick={onClose}
            disabled={saving}
          >
            Cancel
          </button>


          <button
            type="submit"
            className="btn"
            disabled={saving}
          >
            {saving
              ? "Processing..."
              : "Process Payroll"}
          </button>

        </div>

      </form>
    </Modal>
  );
}