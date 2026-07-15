export const EXPERIMENT_STATUSES = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '待审核', value: 'PENDING_REVIEW', type: 'warning' },
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

export const SCREENING_GENDER_OPTIONS = [
  { label: '不限', value: 'ANY' },
  { label: '仅限男性', value: 'MALE' },
  { label: '仅限女性', value: 'FEMALE' },
];

export const HANDEDNESS_OPTIONS = [
  { label: '不限', value: 'ANY' },
  { label: '左利手', value: 'LEFT' },
  { label: '右利手', value: 'RIGHT' },
  { label: '混合利手', value: 'MIXED' },
];

export const MAJOR_CATEGORY_OPTIONS = [
  { label: '不限', value: '不限' },
  { label: '心理学类', value: '心理学类' },
  { label: '计算机类', value: '计算机类' },
  { label: '文学类', value: '文学类' },
  { label: '理学类', value: '理学类' },
  { label: '工学类', value: '工学类' },
  { label: '医学类', value: '医学类' },
  { label: '经管类', value: '经管类' },
];

export const statusMap = Object.fromEntries(
  EXPERIMENT_STATUSES.map((item) => [item.value, item]),
);
