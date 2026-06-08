import http from './http';

export function getAllConfigs() {
  return http.get('/config');
}

export function updateConfig(key, data) {
  return http.put(`/config/${key}`, data);
}
