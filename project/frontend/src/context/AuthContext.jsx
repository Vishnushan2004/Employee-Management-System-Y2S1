import {
  createContext,
  useEffect,
  useState,
} from "react";

import {
  login as loginRequest,
  logout as logoutRequest,
} from "../services/authService";

// ============================================================
// CONSTANTS
// ============================================================

const TOKEN_KEY = "ems_token";
const USER_KEY = "ems_user";

// ============================================================
// CONTEXT
// ============================================================

export const AuthContext = createContext(null);

// ============================================================
// AUTH PROVIDER
// ============================================================

export function AuthProvider({ children }) {
  // ----------------------------------------------------------
  // Load stored user
  // ----------------------------------------------------------

  const [user, setUser] = useState(() => {
    try {
      const storedUser =
        localStorage.getItem(USER_KEY);

      if (!storedUser) {
        return null;
      }

      return JSON.parse(storedUser);
    } catch (error) {
      console.error(
        "[AUTH] Invalid stored user:",
        error
      );

      localStorage.removeItem(USER_KEY);

      return null;
    }
  });

  // ----------------------------------------------------------
  // Loading state
  // ----------------------------------------------------------

  const [loading, setLoading] = useState(false);

  // ----------------------------------------------------------
  // Keep localStorage synchronized
  // ----------------------------------------------------------

  useEffect(() => {
    if (user) {
      localStorage.setItem(
        USER_KEY,
        JSON.stringify(user)
      );
    } else {
      localStorage.removeItem(USER_KEY);
    }
  }, [user]);

  // ----------------------------------------------------------
  // LOGIN
  // ----------------------------------------------------------

  async function login(username, password) {
    setLoading(true);

    try {
      const data = await loginRequest(
        username,
        password
      );

      // Make sure backend returned something.
      if (!data) {
        throw new Error(
          "The server returned an empty login response."
        );
      }

      // Make sure token exists.
      if (!data.token) {
        throw new Error(
          "Login response does not contain an authentication token."
        );
      }

      // Make sure user exists.
      if (!data.user) {
        throw new Error(
          "Login response does not contain user information."
        );
      }

      // Store authentication information.
      localStorage.setItem(
        TOKEN_KEY,
        data.token
      );

      localStorage.setItem(
        USER_KEY,
        JSON.stringify(data.user)
      );

      // Update React state.
      setUser(data.user);

      return data.user;
    } catch (error) {
      console.error(
        "[AUTH] Login failed:",
        error
      );

      throw error;
    } finally {
      setLoading(false);
    }
  }

  // ----------------------------------------------------------
  // LOGOUT
  // ----------------------------------------------------------

  async function logout() {
    try {
      const token =
        localStorage.getItem(TOKEN_KEY);

      if (token) {
        await logoutRequest();
      }
    } catch (error) {
      /*
       * Even if backend logout fails,
       * we still log the user out locally.
       */
      console.warn(
        "[AUTH] Backend logout failed:",
        error
      );
    } finally {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);

      setUser(null);
    }
  }

  // ----------------------------------------------------------
  // Authentication state
  // ----------------------------------------------------------

  const isAuthenticated =
    Boolean(
      user &&
      localStorage.getItem(TOKEN_KEY)
    );

  // ----------------------------------------------------------
  // Context value
  // ----------------------------------------------------------

  const value = {
    user,
    login,
    logout,
    loading,
    isAuthenticated,
  };

  // ----------------------------------------------------------
  // Provider
  // ----------------------------------------------------------

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
