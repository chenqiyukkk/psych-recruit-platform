function pad(value) {
  return String(value).padStart(2, '0');
}

function formatDateTime(value) {
  if (!value) {
    return '待定';
  }

  const normalized = String(value).replace(' ', 'T');
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(
    date.getMinutes()
  )}`;
}

function formatPayment(value) {
  const amount = Number(value || 0);
  return `¥${amount.toFixed(2)}`;
}

const genderMap = {
  FEMALE: '女',
  MALE: '男',
  OTHER: '其他',
  UNKNOWN: '不限',
  ANY: '不限',
  ALL: '不限',
  女: '女',
  男: '男',
  不限: '不限',
};

const experimentStatusMap = {
  DRAFT: { text: '草稿', className: 'muted' },
  PUBLISHED: { text: '招募中', className: 'success' },
  CANCELLED: { text: '已取消', className: 'danger' },
  CLOSED: { text: '已结束', className: 'muted' },
};

const registrationStatusMap = {
  PENDING: { text: '待审核', className: 'warning' },
  APPROVED: { text: '已通过', className: 'success' },
  REJECTED: { text: '未通过', className: 'danger' },
  CANCELLED: { text: '已取消', className: 'muted' },
};

const appealTypeMap = {
  REPUTATION_DEDUCTION: '信誉扣分',
  LOW_RATING: '低评分',
  PAYMENT_DISPUTE: '支付争议',
};

const appealStatusMap = {
  PENDING: { text: '待处理', className: 'warning' },
  UNDER_REVIEW: { text: '处理中', className: 'warning' },
  APPROVED: { text: '已通过', className: 'success' },
  REJECTED: { text: '已驳回', className: 'danger' },
};

function getExperimentStatusMeta(status) {
  return experimentStatusMap[status] || { text: status || '未知', className: 'muted' };
}

function getRegistrationStatusMeta(status) {
  return registrationStatusMap[status] || { text: status || '未知', className: 'muted' };
}

function parseJsonValue(value) {
  if (!value) {
    return null;
  }

  if (typeof value !== 'string') {
    return value;
  }

  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }

  if (!/^[{[]/.test(trimmed)) {
    return trimmed;
  }

  try {
    return JSON.parse(trimmed);
  } catch (error) {
    return trimmed;
  }
}

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value);
}

function firstPresent(source, keys) {
  if (!isPlainObject(source)) {
    return undefined;
  }

  for (const key of keys) {
    if (source[key] !== undefined && source[key] !== null && source[key] !== '') {
      return source[key];
    }
  }
  return undefined;
}

function formatGender(value) {
  const key = String(value || '').trim();
  return genderMap[key] || genderMap[key.toUpperCase()] || key;
}

function formatAgeRange(value) {
  if (Array.isArray(value)) {
    const [min, max] = value;
    if (min !== undefined && max !== undefined) {
      return `${min}-${max} 岁`;
    }
    if (min !== undefined) {
      return `${min} 岁以上`;
    }
  }

  if (isPlainObject(value)) {
    const min = firstPresent(value, ['min', 'from', 'start']);
    const max = firstPresent(value, ['max', 'to', 'end']);
    if (min !== undefined && max !== undefined) {
      return `${min}-${max} 岁`;
    }
    if (min !== undefined) {
      return `${min} 岁以上`;
    }
    if (max !== undefined) {
      return `${max} 岁以下`;
    }
  }

  return value ? String(value) : '';
}

function formatCriteriaValue(value) {
  if (Array.isArray(value)) {
    return value.filter(Boolean).join('、');
  }
  if (isPlainObject(value)) {
    return Object.keys(value)
      .map((key) => `${key}:${value[key]}`)
      .join('、');
  }
  return String(value || '');
}

function formatScreeningCriteria(value) {
  const parsed = parseJsonValue(value);
  if (!parsed) {
    return { items: [], text: '' };
  }

  if (typeof parsed === 'string') {
    return { items: [{ label: '要求', value: parsed }], text: parsed };
  }

  const source = isPlainObject(parsed.include) ? parsed.include : parsed;
  const items = [];
  const usedKeys = new Set();

  const gender = firstPresent(source, ['gender', 'sex']);
  if (gender !== undefined) {
    items.push({ label: '性别', value: formatGender(gender) });
    usedKeys.add('gender');
    usedKeys.add('sex');
  }

  const ageRange = firstPresent(source, ['age_range', 'ageRange', 'age']);
  if (ageRange !== undefined) {
    items.push({ label: '年龄', value: formatAgeRange(ageRange) });
    usedKeys.add('age_range');
    usedKeys.add('ageRange');
    usedKeys.add('age');
  }

  Object.keys(source || {}).forEach((key) => {
    if (usedKeys.has(key)) {
      return;
    }
    const readableValue = formatCriteriaValue(source[key]);
    if (readableValue) {
      items.push({ label: key, value: readableValue });
    }
  });

  return {
    items,
    text: items.map((item) => `${item.label}：${item.value}`).join('；'),
  };
}

function formatExcludeTags(value) {
  const parsed = parseJsonValue(value);
  if (!parsed) {
    return [];
  }

  if (Array.isArray(parsed)) {
    return parsed
      .map((tag) => {
        if (isPlainObject(tag)) {
          return tag.name || tag.tagName || tag.label;
        }
        return tag;
      })
      .filter(Boolean)
      .map(String);
  }

  if (typeof parsed === 'string') {
    return parsed
      .split(/[、,，]/)
      .map((tag) => tag.trim())
      .filter(Boolean);
  }

  return [];
}

function formatExperiment(item) {
  const statusMeta = getExperimentStatusMeta(item.status);
  const tags = Array.isArray(item.tags) ? item.tags : [];
  const criteria = formatScreeningCriteria(item.screeningCriteria);
  const excludeTagTexts = formatExcludeTags(item.excludeTags);

  return Object.assign({}, item, {
    paymentText: formatPayment(item.paymentAmount),
    timeText: `${formatDateTime(item.startTime)} - ${formatDateTime(item.endTime)}`,
    statusText: statusMeta.text,
    statusClassName: statusMeta.className,
    tagTexts: tags.map((tag) => tag.name || tag.tagName || tag.label).filter(Boolean),
    screeningCriteriaItems: criteria.items,
    screeningCriteriaText: criteria.text,
    excludeTagTexts,
    excludeTagsText: excludeTagTexts.join('、'),
  });
}

function formatRegistration(item) {
  const statusMeta = getRegistrationStatusMeta(item.status);

  return Object.assign({}, item, {
    appliedAtText: formatDateTime(item.appliedAt),
    statusText: statusMeta.text,
    statusClassName: statusMeta.className,
  });
}

function formatAppeal(item) {
  const statusMeta = appealStatusMap[item.status] || {
    text: item.status || '未知',
    className: 'muted',
  };

  return Object.assign({}, item, {
    appealTypeText: appealTypeMap[item.appealType] || item.appealType || '未知类型',
    statusText: statusMeta.text,
    statusClassName: statusMeta.className,
    createdAtText: formatDateTime(item.createdAt),
  });
}

module.exports = {
  formatAppeal,
  formatDateTime,
  formatExperiment,
  formatPayment,
  formatRegistration,
  getExperimentStatusMeta,
  getRegistrationStatusMeta,
};
