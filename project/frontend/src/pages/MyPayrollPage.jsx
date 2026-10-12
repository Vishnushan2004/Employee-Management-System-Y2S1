
import { useEffect, useMemo, useState } from "react";
import { payrollService } from "../services/moduleServices";

// Format salary in Sri Lankan rupees.
const money = (value) =>
  new Intl.NumberFormat("en-LK", {
    style: "currency",
    currency: "LKR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value ?? 0));

// Support camelCase and database-style property names.
const getMonth = (row) =>
  Number(row.payrollMonth ?? row.Payroll_Month ?? 0);

const getYear = (row) =>
  Number(row.payrollYear ?? row.Payroll_Year ?? 0);

const getStatus = (row) =>
  String(
    row.payrollStatus ?? row.Payroll_Status ?? "UNKNOWN"
  ).toUpperCase();

const getValue = (row, camelCase, databaseName) =>
  Number(row[camelCase] ?? row[databaseName] ?? 0);

const getPaymentDate = (row) =>
  row.paymentDate ?? row.Payment_Date ?? null;

const getPayrollId = (row) =>
  row.payrollId ?? row.Payroll_ID;

// Escape values safely for CSV files.
function escapeCSV(value) {
  let text = String(value ?? "");

  // Prevent spreadsheet formula injection in text fields.
  if (/^[=+\-@\t\r]/.test(text)) {
    text = "'" + text;
  }

  return `"${text.replace(/"/g, '""')}"`;
}

// Export the currently filtered payroll records.
function exportPayrollCSV(records) {
  if (!records.length) {
    window.alert("There are no payroll records to export.");
    return;
  }

  const headers = [
    "Month",
    "Year",
    "Basic Salary (LKR)",
    "Gross Salary (LKR)",
    "Net Salary (LKR)",
    "Payment Date",
    "Status",
  ];

  const rows = records.map((row) => [
    getMonth(row),
    getYear(row),
    getValue(row, "basicSalary", "Basic_Salary"),
    getValue(row, "grossSalary", "Gross_Salary"),
    getValue(row, "netSalary", "Net_Salary"),
    getPaymentDate(row)
      ? String(getPaymentDate(row)).slice(0, 10)
      : "Not paid yet",
    getStatus(row),
  ]);

  const csv = [headers, ...rows]
    .map((row) => row.map(escapeCSV).join(","))
    .join("\r\n");

  const blob = new Blob(["\uFEFF", csv], {
    type: "text/csv;charset=utf-8;",
  });

  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = "my-payroll-summary.csv";
  document.body.appendChild(link);
  link.click();
  link.remove();

  // Revoke after the browser has had a moment to start the download.
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export default function MyPayrollPage() {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [month, setMonth] = useState("");
  const [year, setYear] = useState("");

  useEffect(() => {
    let active = true;

    async function loadPayroll() {
      setLoading(true);
      setError("");

      try {
        // The backend should return only the authenticated employee's records.
        const result = await payrollService.list();

        if (active) {
          if (!Array.isArray(result)) {
            throw new Error("Unexpected payroll response from the server.");
          }

          setRecords(result);
        }
      } catch (err) {
        if (active) {
          setError(
            err.message || "Unable to load your payroll records."
          );
          setRecords([]);
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    loadPayroll();

    return () => {
      active = false;
    };
  }, []);

  const years = useMemo(
    () =>
      [...new Set(records.map(getYear).filter((value) => value > 0))]
        .sort((a, b) => b - a),
    [records]
  );

  const filteredRecords = useMemo(
    () =>
      records.filter((row) => {
        const matchesMonth =
          !month || getMonth(row) === Number(month);
        const matchesYear =
          !year || getYear(row) === Number(year);

        return matchesMonth && matchesYear;
      }),
    [records, month, year]
  );

  const totals = useMemo(
    () =>
      filteredRecords.reduce(
        (sum, row) => ({
          basic:
            sum.basic + getValue(row, "basicSalary", "Basic_Salary"),
          gross:
            sum.gross + getValue(row, "grossSalary", "Gross_Salary"),
          net:
            sum.net + getValue(row, "netSalary", "Net_Salary"),
        }),
        { basic: 0, gross: 0, net: 0 }
      ),
    [filteredRecords]
  );

  function handlePrint() {
    if (filteredRecords.length === 0) {
      window.alert("There are no payroll records to print.");
      return;
    }

    window.print();
  }

  return (
    <div className="payroll-report-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">My Payroll Summary</h1>
          <p className="page-subtitle">
            View, filter and export your personal salary history.
          </p>
        </div>

        <div className="payroll-report-actions">
          <button
            type="button"
            className="btn"
            onClick={() => exportPayrollCSV(filteredRecords)}
            disabled={
              loading ||
              Boolean(error) ||
              filteredRecords.length === 0
            }
          >
            Export CSV
          </button>

          <button
            type="button"
            className="btn btn-secondary"
            onClick={handlePrint}
            disabled={
              loading ||
              Boolean(error) ||
              filteredRecords.length === 0
            }
          >
            Print / Save PDF
          </button>
        </div>
      </div>

      {error && (
        <div className="error-banner" role="alert">
          {error}
        </div>
      )}

      <div className="payroll-filter-row">
        <label>
          Month
          <select
            value={month}
            onChange={(e) => setMonth(e.target.value)}
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
          <select
            value={year}
            onChange={(e) => setYear(e.target.value)}
          >
            <option value="">All years</option>
            {years.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
        </label>
      </div>

      {loading ? (
        <p>Loading your payroll records...</p>
      ) : !error ? (
        <>
          <div className="payroll-summary-grid">
            <div className="payroll-summary-card">
              <span>Payroll records</span>
              <strong>{filteredRecords.length}</strong>
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

          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Month/Year</th>
                  <th>Basic Salary</th>
                  <th>Gross Salary</th>
                  <th>Net Salary</th>
                  <th>Payment Date</th>
                  <th>Status</th>
                </tr>
              </thead>

              <tbody>
                {filteredRecords.map((row, index) => {
                  const id = getPayrollId(row);
                  const paymentDate = getPaymentDate(row);

                  return (
                    <tr key={id ?? `${getYear(row)}-${getMonth(row)}-${index}`}>
                      <td>
                        {getMonth(row)}/{getYear(row)}
                      </td>
                      <td>
                        {money(
                          getValue(row, "basicSalary", "Basic_Salary")
                        )}
                      </td>
                      <td>
                        {money(
                          getValue(row, "grossSalary", "Gross_Salary")
                        )}
                      </td>
                      <td>
                        {money(
                          getValue(row, "netSalary", "Net_Salary")
                        )}
                      </td>
                      <td>
                        {paymentDate
                          ? String(paymentDate).slice(0, 10)
                          : "Not paid yet"}
                      </td>
                      <td>
                        <span className="payroll-status">
                          {getStatus(row)}
                        </span>
                      </td>
                    </tr>
                  );
                })}

                {filteredRecords.length === 0 && (
                  <tr>
                    <td colSpan={6}>
                      No payroll records found for this period.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </div>
  );
}
