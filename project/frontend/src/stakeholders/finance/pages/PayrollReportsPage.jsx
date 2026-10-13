
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../../context/AuthContext";
import { payrollService } from "../../../services/moduleServices";

const money = (value) =>
  new Intl.NumberFormat("en-LK", {
    style: "currency",
    currency: "LKR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value ?? 0));

const field = (row, ...keys) => {
  for (const key of keys) {
    const value = row?.[key];
    if (value !== undefined && value !== null && value !== "") {
      return value;
    }
  }
  return "";
};

// Employee number must be the business identifier, e.g. EMS009.
// Do not substitute Employee_ID, which may be only a numeric database key.
const getEmployeeNumber = (row) =>
  field(
    row,
    "employeeNumber",
    "Employee_Number",
    "employee_number",
    "employeeNo",
    "Employee_No",
    "employee_no",
    "employeeCode",
    "Employee_Code",
    "employee_code",
    "empNumber",
    "Emp_Number",
    "EmployeeNo",
    "employee_number_value"
  ) ||
  field(
    row?.employee,
    "employeeNumber",
    "Employee_Number",
    "employee_number",
    "employeeNo",
    "Employee_No",
    "employeeCode"
  ) ||
  field(
    row?.Employee,
    "employeeNumber",
    "Employee_Number",
    "employee_number",
    "employeeNo",
    "Employee_No",
    "employeeCode"
  );

const getEmployeeName = (row) => {
  const directName = field(
    row,
    "employeeName",
    "Employee_Name",
    "employee_name",
    "fullName",
    "Full_Name",
    "full_name"
  );

  if (directName) return directName;

  const employee = row?.employee ?? row?.Employee ?? {};

  const firstName = field(employee, "firstName", "First_Name", "first_name");
  const lastName = field(employee, "lastName", "Last_Name", "last_name");

  return [firstName, lastName].filter(Boolean).join(" ") || "—";
};

const getMonth = (row) =>
  Number(field(row, "payrollMonth", "Payroll_Month", "payroll_month") || 0);

const getYear = (row) =>
  Number(field(row, "payrollYear", "Payroll_Year", "payroll_year") || 0);

const getStatus = (row) =>
  String(
    field(row, "payrollStatus", "Payroll_Status", "payroll_status") ||
      "UNKNOWN"
  ).toUpperCase();

const getBasicSalary = (row) =>
  Number(field(row, "basicSalary", "Basic_Salary", "basic_salary") || 0);

const getGrossSalary = (row) =>
  Number(field(row, "grossSalary", "Gross_Salary", "gross_salary") || 0);

const getNetSalary = (row) =>
  Number(field(row, "netSalary", "Net_Salary", "net_salary") || 0);

const getPaymentDate = (row) =>
  field(row, "paymentDate", "Payment_Date", "payment_date");

const getPayrollId = (row) =>
  field(row, "payrollId", "Payroll_ID", "payroll_id");

function csvCell(value) {
  let text = String(value ?? "");

  // Prevent spreadsheet formula injection.
  if (/^[=+\-@\t\r]/.test(text)) {
    text = "'" + text;
  }

  return `"${text.replace(/"/g, '""')}"`;
}

function downloadCsv(records) {
  if (!records.length) {
    window.alert("There are no payroll records to export.");
    return;
  }

  const columns = [
    ["Employee Number", getEmployeeNumber],
    ["Employee Name", getEmployeeName],
    ["Department", (row) =>
      field(row, "departmentName", "Department_Name", "department_name")],
    ["Payroll Month", getMonth],
    ["Payroll Year", getYear],
    ["Basic Salary (LKR)", getBasicSalary],
    ["Gross Salary (LKR)", getGrossSalary],
    ["Net Salary (LKR)", getNetSalary],
    ["Payment Date", (row) => {
      const date = getPaymentDate(row);
      return date ? String(date).slice(0, 10) : "Not paid yet";
    }],
    ["Status", getStatus],
  ];

  const content = [
    columns.map(([title]) => csvCell(title)).join(","),
    ...records.map((row) =>
      columns.map(([, getter]) => csvCell(getter(row))).join(",")
    ),
  ].join("\r\n");

  const blob = new Blob(["\uFEFF", content], {
    type: "text/csv;charset=utf-8;",
  });

  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = `payroll-report-${new Date()
    .toISOString()
    .slice(0, 10)}.csv`;

  document.body.appendChild(link);
  link.click();
  link.remove();

  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;",
  })[character]);
}

function printReport(records, totals, filters) {
  if (!records.length) {
    window.alert("There are no payroll records to print.");
    return;
  }

  const printWindow = window.open("", "_blank");

  if (!printWindow) {
    window.alert("Please allow pop-ups to print or save the payroll report.");
    return;
  }

  const rows = records
    .map((row) => {
      const employeeNumber = getEmployeeNumber(row);
      const employeeName = getEmployeeName(row);
      const paymentDate = getPaymentDate(row);

      return `
        <tr>
          <td>${escapeHtml(employeeNumber || "Not available")}</td>
          <td>${escapeHtml(employeeName)}</td>
          <td>${escapeHtml(`${getMonth(row)}/${getYear(row)}`)}</td>
          <td>${escapeHtml(money(getBasicSalary(row)))}</td>
          <td>${escapeHtml(money(getGrossSalary(row)))}</td>
          <td>${escapeHtml(money(getNetSalary(row)))}</td>
          <td>${escapeHtml(paymentDate ? String(paymentDate).slice(0, 10) : "—")}</td>
          <td>${escapeHtml(getStatus(row))}</td>
        </tr>`;
    })
    .join("");

  printWindow.document.write(`
    <!doctype html>
    <html>
      <head>
        <meta charset="utf-8">
        <title>Payroll Report</title>
        <style>
          body {
            font-family: Arial, sans-serif;
            padding: 24px;
            color: #172b45;
          }

          h1 { margin-bottom: 6px; }

          .meta {
            color: #555;
            margin-bottom: 18px;
            line-height: 1.6;
          }

          .summary {
            display: flex;
            gap: 24px;
            margin-bottom: 20px;
            flex-wrap: wrap;
          }

          table {
            border-collapse: collapse;
            width: 100%;
            font-size: 12px;
          }

          th, td {
            border: 1px solid #ccc;
            padding: 8px;
            text-align: left;
          }

          th { background: #eef2f7; }

          @page { size: landscape; margin: 12mm; }

          @media print {
            body { padding: 0; }
            tr { break-inside: avoid; }
          }
        </style>
      </head>
      <body>
        <h1>Payroll Report</h1>

        <div class="meta">
          Generated: ${escapeHtml(new Date().toLocaleString())}<br>
          Filters: ${escapeHtml(filters)}
        </div>

        <div class="summary">
          <div>Records: ${records.length}</div>
          <div>Total basic: ${escapeHtml(money(totals.basic))}</div>
          <div>Total gross: ${escapeHtml(money(totals.gross))}</div>
          <div>Total net: ${escapeHtml(money(totals.net))}</div>
        </div>

        <table>
          <thead>
            <tr>
              <th>Employee No.</th>
              <th>Employee</th>
              <th>Period</th>
              <th>Basic Salary</th>
              <th>Gross Salary</th>
              <th>Net Salary</th>
              <th>Payment Date</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>
      </body>
    </html>
  `);

  printWindow.document.close();

  // Print after the new window has finished loading.
  printWindow.onload = () => {
    printWindow.focus();
    printWindow.print();
  };
}

export default function PayrollReportsPage() {
  const { user } = useAuth();

  const [records, setRecords] = useState([]);
  const [month, setMonth] = useState("");
  const [year, setYear] = useState(String(new Date().getFullYear()));
  const [employeeId, setEmployeeId] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const isFinanceManager = user?.role === "FINANCE_PAYROLL_MANAGER";

  useEffect(() => {
    if (!isFinanceManager) return undefined;

    let active = true;

    async function loadReports() {
      setLoading(true);
      setError("");

      try {
        const params = {};

        if (month) params.month = Number(month);
        if (year) params.year = Number(year);
        if (employeeId.trim()) {
          params.employeeId = Number(employeeId);
        }
        if (status) params.status = status;

        const result = await payrollService.list(params);

        if (active) {
          setRecords(Array.isArray(result) ? result : []);
        }
      } catch (err) {
        if (active) {
          setError(err.message || "Unable to load payroll reports.");
          setRecords([]);
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    loadReports();

    return () => {
      active = false;
    };
  }, [isFinanceManager, month, year, employeeId, status]);

  const totals = useMemo(
    () =>
      records.reduce(
        (sum, row) => ({
          basic: sum.basic + getBasicSalary(row),
          gross: sum.gross + getGrossSalary(row),
          net: sum.net + getNetSalary(row),
        }),
        { basic: 0, gross: 0, net: 0 }
      ),
    [records]
  );

  if (!isFinanceManager) {
    return (
      <div className="page-content">
        <h1>Access denied</h1>
        <p>
          Only the Finance &amp; Payroll Manager can access payroll reports.
        </p>
      </div>
    );
  }

  const filterDescription = [
    month ? `Month ${month}` : "All months",
    year || "All years",
    employeeId ? `Employee ID ${employeeId}` : "All employees",
    status || "All statuses",
  ].join(" | ");

  return (
    <div className="payroll-report-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Payroll Reports</h1>
          <p className="page-subtitle">
            Filter, review and export payroll processing records.
          </p>
        </div>

        <div className="payroll-report-actions">
          <button
            type="button"
            className="btn"
            disabled={loading || records.length === 0}
            onClick={() => downloadCsv(records)}
          >
            Export CSV
          </button>

          <button
            type="button"
            className="btn btn-secondary"
            disabled={loading || records.length === 0}
            onClick={() => printReport(records, totals, filterDescription)}
          >
            Print / Save PDF
          </button>
        </div>
      </div>

      <div className="payroll-filter-row">
        <label>
          Month
          <select
            value={month}
            onChange={(event) => setMonth(event.target.value)}
          >
            <option value="">All months</option>
            {Array.from({ length: 12 }, (_, index) => (
              <option key={index + 1} value={index + 1}>
                {new Date(2000, index, 1).toLocaleString("en", {
                  month: "long",
                })}
              </option>
            ))}
          </select>
        </label>

        <label>
          Year
          <input
            type="number"
            min="2000"
            max="2100"
            value={year}
            onChange={(event) => setYear(event.target.value)}
          />
        </label>

        <label>
          Employee ID
          <input
            type="number"
            min="1"
            placeholder="All employees"
            value={employeeId}
            onChange={(event) => setEmployeeId(event.target.value)}
          />
        </label>

        <label>
          Payroll status
          <select
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          >
            <option value="">All statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="VERIFIED">Verified</option>
            <option value="PAID">Paid</option>
          </select>
        </label>
      </div>

      {error && (
        <div className="error-banner" role="alert">
          {error}
        </div>
      )}

      <div className="payroll-summary-grid">
        <div className="payroll-summary-card">
          <span>Payroll records</span>
          <strong>{records.length}</strong>
        </div>

        <div className="payroll-summary-card">
          <span>Total basic salary</span>
          <strong>{money(totals.basic)}</strong>
        </div>

        <div className="payroll-summary-card">
          <span>Total gross salary</span>
          <strong>{money(totals.gross)}</strong>
        </div>

        <div className="payroll-summary-card">
          <span>Total net salary</span>
          <strong>{money(totals.net)}</strong>
        </div>
      </div>

      {loading ? (
        <p>Loading payroll report...</p>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Employee No.</th>
                <th>Employee</th>
                <th>Period</th>
                <th>Basic Salary</th>
                <th>Gross Salary</th>
                <th>Net Salary</th>
                <th>Payment Date</th>
                <th>Status</th>
              </tr>
            </thead>

            <tbody>
              {records.map((row, index) => (
                <tr
                  key={
                    getPayrollId(row) ||
                    `${getEmployeeNumber(row)}-${getYear(row)}-${getMonth(row)}-${index}`
                  }
                >
                  <td>{getEmployeeNumber(row) || "Not available"}</td>
                  <td>{getEmployeeName(row)}</td>
                  <td>
                    {getMonth(row)}/{getYear(row)}
                  </td>
                  <td>{money(getBasicSalary(row))}</td>
                  <td>{money(getGrossSalary(row))}</td>
                  <td>{money(getNetSalary(row))}</td>
                  <td>
                    {getPaymentDate(row)
                      ? String(getPaymentDate(row)).slice(0, 10)
                      : "—"}
                  </td>
                  <td>{getStatus(row)}</td>
                </tr>
              ))}

              {!records.length && (
                <tr>
                  <td colSpan={8}>
                    {error
                      ? "Unable to display payroll records."
                      : "No payroll records match these filters."}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
