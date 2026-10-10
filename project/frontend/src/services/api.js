
const API_BASE = "http://localhost:8080/api";

function getToken() {
  return localStorage.getItem("ems_token");
}

async function request(path, { method = "GET", body, params } = {}) {
  let url = `${API_BASE}${path}`;

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

  const headers = {
    Accept: "application/json",
  };

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const token = getToken();

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  let response;

  try {
    response = await fetch(url, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (error) {
    console.error("API connection failed:", url, error);
    throw new Error(
      "Cannot connect to the backend. Check that the Java server is running."
    );
  }

  const text = await response.text();
  let data = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    const message =
      data && typeof data === "object" && data.error
        ? data.error
        : `Request failed (${response.status})`;

    console.error("API error:", {
      url,
      status: response.status,
      response: data,
    });

    if (response.status === 401) {
      throw new Error("Session expired or invalid. Please log in again.");
    }

    throw new Error(message);
  }

  return data;
}

export const api = {
  get: (path, params) => request(path, { method: "GET", params }),
  post: (path, body) => request(path, { method: "POST", body }),
  put: (path, body) => request(path, { method: "PUT", body }),
  del: (path) => request(path, { method: "DELETE" }),
};
