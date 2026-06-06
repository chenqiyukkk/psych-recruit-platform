import dayjs from 'dayjs';

export function formatDateTime(value, fallback = '--') {
  if (!value) {
    return fallback;
  }
  return dayjs(value).format('YYYY-MM-DD HH:mm');
}

export function formatCurrency(value, fallback = '--') {
  if (value === null || value === undefined || value === '') {
    return fallback;
  }
  const number = Number(value);
  if (Number.isNaN(number)) {
    return fallback;
  }
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
  }).format(number);
}

export function prettyJson(value) {
  if (!value) {
    return '--';
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch (_error) {
    return value;
  }
}
