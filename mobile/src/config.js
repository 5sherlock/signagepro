const savedUrl = typeof window !== 'undefined' ? localStorage.getItem('SIGNAGE_SERVER_URL') : null;
const originUrl = typeof window !== 'undefined' ? window.location.origin : null;
// 모바일 앱은 사용자가 제공한 3300포트 백엔드에 접속합니다.
export const API_URL = savedUrl || originUrl?.replace(':5174', ':3300') || 'http://localhost:3300';
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
