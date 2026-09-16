import { api } from "./api";

export async function login(username, password) {
  return api.post("/auth/login", { username, password });
}

export async function logout() {
  try {
    await api.post("/auth/logout");
  } catch {
    // ignore network errors on logout - we clear local state regardless
  }
}
