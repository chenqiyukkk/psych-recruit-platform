export const REGISTRATION_STATUS_OPTIONS = [
  { label: '待审核', value: 'PENDING', type: 'warning' },
  { label: '已通过', value: 'APPROVED', type: 'success' },
  { label: '已拒绝', value: 'REJECTED', type: 'danger' },
  { label: '已取消', value: 'CANCELLED', type: 'info' },
];

export const registrationStatusMap = Object.fromEntries(
  REGISTRATION_STATUS_OPTIONS.map((item) => [item.value, item]),
);
