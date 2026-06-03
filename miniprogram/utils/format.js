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

function getExperimentStatusMeta(status) {
  return experimentStatusMap[status] || { text: status || '未知', className: 'muted' };
}

function getRegistrationStatusMeta(status) {
  return registrationStatusMap[status] || { text: status || '未知', className: 'muted' };
}

function formatExperiment(item) {
  const statusMeta = getExperimentStatusMeta(item.status);
  const tags = Array.isArray(item.tags) ? item.tags : [];

  return Object.assign({}, item, {
    paymentText: formatPayment(item.paymentAmount),
    timeText: `${formatDateTime(item.startTime)} - ${formatDateTime(item.endTime)}`,
    statusText: statusMeta.text,
    statusClassName: statusMeta.className,
    tagTexts: tags.map((tag) => tag.name || tag.tagName || tag.label).filter(Boolean),
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

module.exports = {
  formatDateTime,
  formatExperiment,
  formatPayment,
  formatRegistration,
  getExperimentStatusMeta,
  getRegistrationStatusMeta,
};
