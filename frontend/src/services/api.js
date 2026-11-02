import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export const http = axios.create({
  baseURL,
  timeout: 45000,
  headers: { 'Content-Type': 'application/json', Accept: 'application/json' }
});

http.interceptors.request.use(
  (config) => {
    const token = authApi.getToken();
    if (token) {
      config.headers = config.headers || {};
      config.headers['Authorization'] = `Bearer ${token}`;
      if (typeof config.headers.set === 'function') {
        config.headers.set('Authorization', `Bearer ${token}`);
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || '';

    // If 401 Unauthorized occurs on a non-auth endpoint, trigger session expiration
    if (status === 401 && !url.includes('/auth/login') && !url.includes('/auth/register')) {
      window.dispatchEvent(new CustomEvent('recallradar:unauthorized'));
    }

    const serverMessage =
      error.response?.data?.message || error.response?.data?.error;
    let message = serverMessage || error.message || 'Request failed';
    if (status === 401) {
      message = serverMessage || 'Invalid credentials or session expired. Please sign in.';
    } else if (status === 403) {
      message = serverMessage || 'Access forbidden (403): You do not have permission for this operation.';
    } else if (status === 404) {
      message = serverMessage || 'API endpoint or record not found (404).';
    } else if (status === 409) {
      message = serverMessage || 'Record conflicts with existing data.';
    } else if (status === 400) {
      message = serverMessage || 'The submitted data was not accepted. Please check the fields.';
    } else if (status === 429) {
      message = serverMessage || 'AI quota limit reached (429). Please wait a moment or try again later.';
    } else if (status === 503) {
      message = serverMessage || 'AI Service is temporarily unavailable (503). Ensure an active Gemini API key is configured.';
    } else if (status >= 500 && status !== 503) {
      message = serverMessage || 'The backend encountered an error. Check server logs.';
    }
    return Promise.reject(new Error(message));
  }
);

export function unwrapList(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data?.content)) return payload.data.content;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.results)) return payload.results;
  return [];
}

/**
 * Normalizes any severity string to standard uppercase categories:
 * 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN'
 */
export function normalizeSeverity(val) {
  if (!val) return 'UNKNOWN';
  const s = String(val).trim().toUpperCase();
  if (s === 'MODERATE') return 'MEDIUM';
  if (['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].includes(s)) {
    return s;
  }
  return s || 'UNKNOWN';
}

export const SEVERITY_COLORS = {
  CRITICAL: '#dc2626', // Red
  HIGH: '#ea580c',     // Orange
  MEDIUM: '#ca8a04',   // Yellow / Amber
  LOW: '#16a34a',      // Green
  UNKNOWN: '#64748b'   // Slate / Grey
};

export function resolveProductName(recall, products = []) {
  if (!recall) return 'Product not linked';
  if (recall.product?.name) return recall.product.name;
  if (recall.productName) return recall.productName;
  if (recall.productId && Array.isArray(products)) {
    const match = products.find((p) => p.id === recall.productId);
    if (match?.name) return match.name;
  }
  return 'Product not linked';
}

export function resolveProductCategory(recall, products = []) {
  if (!recall) return 'Uncategorized';
  if (recall.product?.category) return recall.product.category;
  if (recall.productCategory) return recall.productCategory;
  if (recall.productId && Array.isArray(products)) {
    const match = products.find((p) => p.id === recall.productId);
    if (match?.category) return match.category;
  }
  return 'Uncategorized';
}

export function resolveProductEntity(recall, products = []) {
  if (!recall) return null;
  if (recall.product && typeof recall.product === 'object') return recall.product;
  if (recall.productId && Array.isArray(products)) {
    return products.find((p) => p.id === recall.productId) || null;
  }
  return null;
}

export function getApiBaseUrl() {
  return baseURL;
}

export const systemApi = {
  ping: () => http.get('/ai/health'),
  health: () => http.get('/ai/health')
};

export const productApi = {
  getAll: (params) => http.get('/products', { params }),
  getById: (id) => http.get(`/products/${id}`),
  create: (data) => http.post('/products', data),
  update: (id, data) => http.put(`/products/${id}`, data),
  remove: (id) => http.delete(`/products/${id}`)
};

export const recallApi = {
  getAll: (params) => http.get('/recalls', { params }),
  getById: (id) => http.get(`/recalls/${id}`),
  create: (data) => http.post('/recalls', data),
  update: (id, data) => http.put(`/recalls/${id}`, data),
  remove: (id) => http.delete(`/recalls/${id}`),
  updateSeverity: (id, payload) => http.patch(`/recalls/${id}/severity`, payload),
  getSeverityAudit: (id) => http.get(`/recalls/${id}/severity-audit`),
  getCandidateMatches: (id) => http.get(`/recalls/${id}/candidate-matches`),
  associateProduct: (id, payload) => http.post(`/recalls/${id}/associate`, payload),
  ingest: (limit = 10) => http.post(`/recalls/ingest?limit=${limit}`)
};

export const assistantApi = {
  ask: (question, conversationHistory = [], conversationContextId = null) =>
    http.post('/ai/assistant', { question, conversationHistory, conversationContextId }),
  health: () => http.get('/ai/health'),
  checkHealth: () => http.get('/ai/health')
};

export const analyticsApi = {
  getSummary: () => http.get('/analytics/summary')
};

export const authApi = {
  login: (credentials) => http.post('/auth/login', credentials),
  register: (data) => http.post('/auth/register', data),
  registerAnalyst: (data) => http.post('/auth/register/analyst', data),
  me: () => http.get('/auth/me'),
  getUser: () => {
    try {
      const u = localStorage.getItem('rr_user');
      return u ? JSON.parse(u) : null;
    } catch {
      return null;
    }
  },
  setUser: (user) => {
    try {
      if (user) {
        localStorage.setItem('rr_user', JSON.stringify(user));
      } else {
        localStorage.removeItem('rr_user');
      }
    } catch {}
  },
  getToken: () => localStorage.getItem('rr_token'),
  setToken: (token) => {
    if (token) {
      localStorage.setItem('rr_token', token);
    } else {
      localStorage.removeItem('rr_token');
    }
  },
  logout: () => {
    localStorage.removeItem('rr_token');
    localStorage.removeItem('rr_user');
  }
};

export const adminApi = {
  getUsers: (params) => http.get('/admin/users', { params }),
  getStats: () => http.get('/admin/users/stats'),
  getUserById: (id) => http.get(`/admin/users/${id}`),
  createUser: (payload) => http.post('/admin/users', payload),
  updateUser: (id, payload) => http.put(`/admin/users/${id}`, payload),
  updateRole: (id, role) => http.patch(`/admin/users/${id}/role`, { role }),
  updateStatus: (id, enabled) => http.patch(`/admin/users/${id}/status`, { enabled }),
  getAuditLogs: (id) => http.get(`/admin/users/${id}/audit`),
  getAllAuditLogs: (params) => http.get('/admin/users/audit', { params }),
  getAnalystRequests: (params) => http.get('/admin/analyst-requests', { params }),
  getAnalystRequestById: (id) => http.get(`/admin/analyst-requests/${id}`),
  approveAnalystRequest: (id) => http.post(`/admin/analyst-requests/${id}/approve`),
  rejectAnalystRequest: (id, payload) => http.post(`/admin/analyst-requests/${id}/reject`, payload)
};
