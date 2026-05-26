const savedUrl = typeof window !== 'undefined' ? localStorage.getItem('SIGNAGE_SERVER_URL') : null;
export const API_URL = savedUrl || 'http://localhost:3300';
export const SOCKET_URL = API_URL;

export function getToken() {
  return localStorage.getItem('SIGNAGE_TOKEN') || '';
}

export function apiFetch(url, options = {}) {
  const token = getToken();
  const isFormData = options.body instanceof FormData;
  return fetch(url, {
    ...options,
    headers: {
      ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
      ...options.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
}
