import http from './http';

export function getPlatformSummary() {
  return http.get('/statistics/summary');
}

export function getNotifications(params) {
  return http.get('/notifications/my', { params });
}

export function getUnreadCount() {
  return http.get('/notifications/unread-count');
}

export function markNotificationRead(id) {
  return http.put(`/notifications/${id}/read`);
}

export function markAllNotificationsRead() {
  return http.put('/notifications/read-all');
}
