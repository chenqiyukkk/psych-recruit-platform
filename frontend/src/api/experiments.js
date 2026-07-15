import http from './http';

export function getExperimentOptions() {
  return Promise.all([
    http.get('/config/experiment-types'),
    http.get('/config/locations'),
    http.get('/config/tags'),
  ]).then(([experimentTypes, locations, tags]) => ({
    experimentTypes,
    locations,
    tags,
  }));
}

export function getExperiments(params) {
  return http.get('/experiments', { params });
}

export function getExperimentById(id) {
  return http.get(`/experiments/${id}`);
}

export function createExperiment(data) {
  return http.post('/experiments', data);
}

export function updateExperiment(id, data) {
  return http.put(`/experiments/${id}`, data);
}

export function publishExperiment(id) {
  return http.post(`/experiments/${id}/publish`);
}

export function submitForReview(id) {
  return http.post(`/experiments/${id}/submit-review`);
}

export function approveExperiment(id) {
  return http.post(`/experiments/${id}/approve`);
}

export function rejectExperiment(id, reason) {
  return http.post(`/experiments/${id}/reject`, { reason });
}

export function cancelExperiment(id) {
  return http.post(`/experiments/${id}/cancel`);
}

export function deleteExperiment(id) {
  return http.delete(`/experiments/${id}`);
}

export function getExperimentStatistics(id) {
  return http.get(`/statistics/experiments/${id}`);
}
