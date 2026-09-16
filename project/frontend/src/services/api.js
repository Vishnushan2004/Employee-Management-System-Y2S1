const API_BASE = "http://localhost:8080/api";

/**
 * Get the current authentication token.
 */
function getToken() {
  return localStorage.getItem("ems_token");
}

/**
 * Generic API request function.
 */
async function request(
  path,
  {
    method = "GET",
    body = undefined,
    params = undefined,
  } = {}
) {
  let url = `${API_BASE}${path}`;

  // ------------------------------------------------------------
  // Query parameters
  // ------------------------------------------------------------

  if (params) {
    const query = new URLSearchParams(
      Object.entries(params).filter(
        ([, value]) =>
          value !== undefined &&
          value !== null &&
          value !== ""
      )
    ).toString();

    if (query) {
      url += `?${query}`;
    }
  }

  // ------------------------------------------------------------
  // Headers
  // ------------------------------------------------------------

  const headers = {
    "Content-Type": "application/json",
  };

  const token = getToken();

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  // ------------------------------------------------------------
  // Send request
  // ------------------------------------------------------------

  let response;

  try {
    response = await fetch(url, {
      method,
      headers,
      body:
        body !== undefined
          ? JSON.stringify(body)
          : undefined,
    });
  } catch (error) {
    console.error(
      `[API] Cannot connect to backend: ${url}`,
      error
    );

    throw new Error(
      "Cannot connect to the backend server. Make sure the Java backend is running on http://localhost:8080."
    );
  }

  // ------------------------------------------------------------
  // Read response
  // ------------------------------------------------------------

  const text = await response.text();

  let data = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  // ------------------------------------------------------------
  // HTTP error
  // ------------------------------------------------------------

  if (!response.ok) {
    const message =
      data &&
      typeof data === "object" &&
      data.error
        ? data.error
        : `Request failed (${response.status})`;

    const error = new Error(message);

    error.status = response.status;

    throw error;
  }

  // ------------------------------------------------------------
  // IMPORTANT:
  // Return successful API response.
  // ------------------------------------------------------------

  return data;
}

/**
 * GET request.
 */
export async function get(path, params) {
  return request(path, {
    method: "GET",
    params,
  });
}

/**
 * POST request.
 */
export async function post(path, body) {
  return request(path, {
    method: "POST",
    body,
  });
}

/**
 * PUT request.
 */
export async function put(path, body) {
  return request(path, {
    method: "PUT",
    body,
  });
}

/**
 * DELETE request.
 */
export async function del(path, params) {
  return request(path, {
    method: "DELETE",
    params,
  });
}

/**
 * Export generic request function.
 */
export { request };

/**
 * Aggregated API object.
 * All service modules (authService, employeeService, moduleServices, etc.)
 * import this as `import { api } from "./api"` and call api.get/api.post/...
 * This export was previously missing, which made every service call in the
 * app throw "Cannot read properties of undefined" instead of hitting the
 * network - the root cause of the "Failed to fetch" symptom on every page,
 * including /performance.
 */
export const api = { get, post, put, del, request };
