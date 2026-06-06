export const EXPERIMENT_STATUSES = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '已发布', value: 'PUBLISHED', type: 'success' },
  { label: '招募中', value: 'RECRUITING', type: 'warning' },
  { label: '已满员', value: 'FULL', type: 'danger' },
  { label: '进行中', value: 'ONGOING', type: '' },
  { label: '已完成', value: 'COMPLETED', type: 'success' },
];

export const RISK_LEVEL_OPTIONS = [
  { label: '低风险', value: 'LOW' },
  { label: '中风险', value: 'MEDIUM' },
  { label: '高风险', value: 'HIGH' },
];

export const PAYMENT_METHOD_OPTIONS = [
  { label: '线下支付', value: 'OFFLINE' },
  { label: '线上支付', value: 'ONLINE' },
];

export const statusMap = Object.fromEntries(
  EXPERIMENT_STATUSES.map((item) => [item.value, item]),
);
