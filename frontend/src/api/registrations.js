import http from './http';

export function getExperimentRegistrations(experimentId) {
  return http.get(`/registrations/experiment/${experimentId}`);
}

export function approveRegistration(id) {
  return http.post(`/registrations/${id}/approve`);
}

export function rejectRegistration(id) {
  return http.post(`/registrations/${id}/reject`);
}

export function signInRegistration(id) {
  return http.post(`/sign-ins/registrations/${id}`);
}

export function completeRegistration(id) {
  return http.post(`/sign-ins/registrations/${id}/complete`);
}
