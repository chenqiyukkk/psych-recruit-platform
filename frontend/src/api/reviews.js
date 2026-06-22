import http from './http';

export function getMyReviews() {
  return http.get('/reviews/my');
}

export function getReceivedReviews() {
  return http.get('/reviews/received');
}

export function createReview(registrationId, data) {
  return http.post(`/reviews/registrations/${registrationId}`, data);
}
