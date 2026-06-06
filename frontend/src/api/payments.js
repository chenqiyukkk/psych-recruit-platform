import http from './http';

export function getPaymentRecord(registrationId) {
  return http
    .get(`/payment/records/${registrationId}`, { silent: true })
    .catch((error) => {
      if (error?.code === 404 || error?.response?.data?.code === 404) {
        return null;
      }
      throw error;
    });
}

export function confirmPayer(data) {
  return http.post('/payment/records/confirm-payer', data);
}

export function confirmPayee(data) {
  return http.post('/payment/records/confirm-payee', data);
}

export function disputePayment(data) {
  return http.post('/payment/records/dispute', data);
}

export function getMyPaymentCodes() {
  return http.get('/payment/codes/my');
}
