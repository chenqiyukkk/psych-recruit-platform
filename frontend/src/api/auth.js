import http from './http';

export function register(data) {
  return http.post('/auth/register', data);
}

export function login(data) {
  return http.post('/auth/login', data);
}

export function logout() {
  return http.post('/auth/logout');
}

export function getProfile() {
  return http.get('/users/profile');
}

export function updateProfile(data) {
  return http.put('/users/profile', data);
}
