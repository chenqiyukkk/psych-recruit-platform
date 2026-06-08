export const PAYMENT_STATUS_OPTIONS = [
  { label: '未确认', value: 'PENDING', type: 'info' },
  { label: '已付款', value: 'PAID', type: 'warning' },
  { label: '已确认', value: 'CONFIRMED', type: 'success' },
  { label: '争议中', value: 'DISPUTED', type: 'danger' },
];

export const paymentStatusMap = Object.fromEntries(
  PAYMENT_STATUS_OPTIONS.map((item) => [item.value, item]),
);
