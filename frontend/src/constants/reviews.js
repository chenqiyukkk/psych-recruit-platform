export const REVIEW_TYPE_OPTIONS = [
  { label: '被试评价研究者', value: 'SUBJECT_TO_RESEARCHER' },
  { label: '研究者评价被试', value: 'RESEARCHER_TO_SUBJECT' },
];

export const reviewTypeMap = Object.fromEntries(
  REVIEW_TYPE_OPTIONS.map((item) => [item.value, item]),
);
