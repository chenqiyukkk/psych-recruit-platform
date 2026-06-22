import http from './http';

export function getAppeals(params) {
  return http.get('/appeals', { params });
}

export function reviewAppeal(id, data) {
  return http.put(`/appeals/${id}/review`, data);
}
