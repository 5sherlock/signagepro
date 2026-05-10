// API 및 소켓 서버 설정
// 개발 시에는 localhost, 배포 시에는 사용자가 설정한 주소나 localStorage 값을 사용합니다.
const isDev = import.meta.env.DEV;
const savedUrl = typeof window !== 'undefined' ? localStorage.getItem('SIGNAGE_SERVER_URL') : null;

export const API_URL = savedUrl || 'http://localhost:3000';

export const SOCKET_URL = API_URL;
