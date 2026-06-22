export const APPEAL_STATUS_OPTIONS = [
  { label: '待处理', value: 'PENDING', type: 'warning' },
  { label: '审核中', value: 'UNDER_REVIEW', type: 'info' },
  { label: '已通过', value: 'APPROVED', type: 'success' },
  { label: '已拒绝', value: 'REJECTED', type: 'danger' },
];

export const APPEAL_TYPE_OPTIONS = [
  { label: '信誉分申诉', value: 'REPUTATION_DEDUCTION' },
  { label: '低评分申诉', value: 'LOW_RATING' },
  { label: '支付争议', value: 'PAYMENT_DISPUTE' },
];

export const appealStatusMap = Object.fromEntries(
  APPEAL_STATUS_OPTIONS.map((item) => [item.value, item]),
);

export const appealTypeMap = Object.fromEntries(
  APPEAL_TYPE_OPTIONS.map((item) => [item.value, item]),
);
