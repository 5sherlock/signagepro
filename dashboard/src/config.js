const savedUrl = typeof window !== 'undefined' ? localStorage.getItem('SIGNAGE_SERVER_URL') : null;
const originUrl = typeof window !== 'undefined' ? window.location.origin : null;

// 패키지 배포(NAS 등)에서는 서버가 대시보드를 같은 오리진으로 서빙하므로 origin이 곧 API다.
// 수동 override(SIGNAGE_SERVER_URL)는 대시보드를 API와 분리해 띄우는 Vite dev(:3000)/file:// 에서만 의미가 있다.
// → 프로덕션에서 stale한 override가 origin을 덮어써 로그인 불가가 되는 함정을 차단.
const isStandaloneDev = !originUrl || originUrl.startsWith('file') || /:3000$/.test(originUrl);
export const API_URL = isStandaloneDev
  ? (savedUrl || 'http://localhost:3001')
  : originUrl;
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
