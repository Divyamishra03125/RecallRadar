import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => authApi.getToken());
  const [user, setUser] = useState(() => authApi.getUser());
  const [loading, setLoading] = useState(true);

  const normalizeRole = (r) => {
    if (!r) return 'VIEWER';
    const s = String(r).toUpperCase();
    return s.startsWith('ROLE_') ? s.substring(5) : s;
  };

  const logout = useCallback(() => {
    authApi.logout();
    setToken(null);
    setUser(null);
    setLoading(false);
  }, []);

  // Verify and sync current user on initial mount
  useEffect(() => {
    let isMounted = true;

    async function verifySession() {
      const storedToken = authApi.getToken();
      if (!storedToken) {
        if (isMounted) {
          setUser(null);
          setLoading(false);
        }
        return;
      }

      try {
        const res = await authApi.me();
        if (isMounted && res.data?.authenticated) {
          const freshUser = {
            id: res.data.id,
            username: res.data.username,
            email: res.data.email,
            fullName: res.data.fullName || res.data.username,
            role: normalizeRole(res.data.role),
            enabled: res.data.enabled !== false,
            createdAt: res.data.createdAt,
            lastLoginAt: res.data.lastLoginAt
          };
          authApi.setUser(freshUser);
          setUser(freshUser);
        }
      } catch (err) {
        // If 401 or token invalid, clear session
        if (isMounted) {
          logout();
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    verifySession();

    // Listen for unauthorized events dispatched by API interceptors
    const handleUnauthorized = () => {
      logout();
    };
    window.addEventListener('recallradar:unauthorized', handleUnauthorized);

    return () => {
      isMounted = false;
      window.removeEventListener('recallradar:unauthorized', handleUnauthorized);
    };
  }, [logout]);

  const login = async (credentials) => {
    const res = await authApi.login(credentials);
    if (res.data?.token) {
      const role = normalizeRole(res.data.role);
      const userObj = {
        id: res.data.id,
        username: res.data.username,
        email: res.data.email,
        fullName: res.data.fullName || res.data.username,
        role,
        enabled: true
      };
      authApi.setToken(res.data.token);
      authApi.setUser(userObj);
      setToken(res.data.token);
      setUser(userObj);
      return userObj;
    }
    throw new Error('Authentication failed: no token returned.');
  };

  const register = async (userData) => {
    const res = await authApi.register(userData);
    if (res.data?.token) {
      const role = normalizeRole(res.data.role);
      const userObj = {
        id: res.data.id,
        username: res.data.username,
        email: res.data.email,
        fullName: res.data.fullName || res.data.username,
        role,
        enabled: true
      };
      authApi.setToken(res.data.token);
      authApi.setUser(userObj);
      setToken(res.data.token);
      setUser(userObj);
      return userObj;
    }
    return res.data;
  };

  const refreshUser = async () => {
    try {
      const res = await authApi.me();
      if (res.data?.authenticated) {
        const freshUser = {
          id: res.data.id,
          username: res.data.username,
          email: res.data.email,
          fullName: res.data.fullName || res.data.username,
          role: normalizeRole(res.data.role),
          enabled: res.data.enabled !== false,
          createdAt: res.data.createdAt,
          lastLoginAt: res.data.lastLoginAt
        };
        authApi.setUser(freshUser);
        setUser(freshUser);
        return freshUser;
      }
    } catch {}
    return null;
  };

  const role = normalizeRole(user?.role);
  const isAdmin = role === 'ADMIN';
  const isAnalyst = role === 'ANALYST';
  const isViewer = role === 'VIEWER';
  const isAuthenticated = !!token && !!user;

  const value = {
    user,
    token,
    role,
    isAdmin,
    isAnalyst,
    isViewer,
    isAuthenticated,
    loading,
    login,
    register,
    logout,
    refreshUser
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
