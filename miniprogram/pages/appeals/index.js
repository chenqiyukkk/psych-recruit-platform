const { api } = require('../../utils/request');
const { formatDateTime, formatRegistration } = require('../../utils/format');

const appealTypes = [
  { label: '信誉扣分申诉', value: 'REPUTATION_DEDUCTION' },
  { label: '低评分申诉', value: 'LOW_RATING' },
  { label: '支付争议申诉', value: 'PAYMENT_DISPUTE' },
];

const appealTypeMap = {
  REPUTATION_DEDUCTION: '信誉扣分申诉',
  LOW_RATING: '低评分申诉',
  PAYMENT_DISPUTE: '支付争议申诉',
};

const appealStatusMap = {
  PENDING: { text: '待审核', className: 'status-warning' },
  UNDER_REVIEW: { text: '审核中', className: 'status-warning' },
  APPROVED: { text: '已通过', className: 'status-success' },
  REJECTED: { text: '已拒绝', className: 'status-danger' },
};

function formatAppeal(item) {
  const statusMeta = appealStatusMap[item.status] || { text: item.status || '未知', className: 'status-muted' };

  return Object.assign({}, item, {
    createdAtText: formatDateTime(item.createdAt),
    reviewedAtText: formatDateTime(item.reviewedAt),
    typeText: appealTypeMap[item.appealType] || item.appealType || '未知类型',
    statusText: statusMeta.text,
    statusClassName: statusMeta.className,
  });
}

function formatCompletedExperiment(registration, experiment) {
  const title = experiment ? experiment.title : `实验 #${registration.experimentId}`;
  return Object.assign({}, registration, {
    experimentTitle: title,
    pickerText: `${title} · #${registration.experimentId}`,
  });
}

Page({
  data: {
    appealTypes,
    typeIndex: 0,
    experimentIndex: -1,
    completedExperiments: [],
    completedExperimentOptions: [],
    reason: '',
    evidenceUrls: '',
    appeals: [],
    loading: false,
    submitting: false,
    error: '',
  },

  onShow() {
    this.loadPageData();
  },

  onPullDownRefresh() {
    this.loadPageData().finally(() => wx.stopPullDownRefresh());
  },

  ensureLogin() {
    if (!getApp().getToken()) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      wx.switchTab({ url: '/pages/profile/index' });
      return false;
    }
    return true;
  },

  onTypeChange(event) {
    this.setData({ typeIndex: Number(event.detail.value) });
  },

  onExperimentChange(event) {
    this.setData({ experimentIndex: Number(event.detail.value) });
  },

  onReasonInput(event) {
    this.setData({ reason: event.detail.value });
  },

  onEvidenceInput(event) {
    this.setData({ evidenceUrls: event.detail.value });
  },

  loadPageData() {
    if (!this.ensureLogin()) {
      return Promise.resolve();
    }

    this.setData({ loading: true, error: '' });
    return Promise.all([this.loadAppeals(), this.loadCompletedExperiments()])
      .catch((error) => {
        this.setData({ error: error.message || '申诉页面加载失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  loadAppeals() {
    return api.getMyAppeals().then((list) => {
      const appeals = (Array.isArray(list) ? list : []).map(formatAppeal);
      this.setData({ appeals });
    });
  },

  loadCompletedExperiments() {
    return api.getRegistrations().then((registrations) => {
      const completed = (Array.isArray(registrations) ? registrations : [])
        .map(formatRegistration)
        .filter((item) => item.isCompleted && item.experimentId);
      const ids = Array.from(new Set(completed.map((item) => item.experimentId)));

      return Promise.all(
        ids.map((id) =>
          api
            .getExperiment(id)
            .then((experiment) => [id, experiment])
            .catch(() => [id, null])
        )
      ).then((pairs) => {
        const experimentMap = pairs.reduce((map, pair) => {
          const [id, experiment] = pair;
          map[id] = experiment;
          return map;
        }, {});
        const completedExperiments = completed.map((item) =>
          formatCompletedExperiment(item, experimentMap[item.experimentId])
        );
        this.setData({
          completedExperiments,
          completedExperimentOptions: completedExperiments.map((item) => item.pickerText),
          experimentIndex: completedExperiments.length ? Math.max(this.data.experimentIndex, 0) : -1,
        });
      });
    });
  },

  buildEvidenceUrls() {
    const value = this.data.evidenceUrls.trim();
    if (!value) {
      return '';
    }
    return JSON.stringify(
      value
        .split(/\n|,|，/)
        .map((url) => url.trim())
        .filter(Boolean)
    );
  },

  submitAppeal() {
    if (!this.ensureLogin() || this.data.submitting) {
      return;
    }

    const selected = this.data.completedExperiments[this.data.experimentIndex];
    const reason = this.data.reason.trim();
    if (!selected) {
      wx.showToast({ title: '请选择已完成实验', icon: 'none' });
      return;
    }
    if (reason.length < 10) {
      wx.showToast({ title: '申诉理由至少10字', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    api
      .createAppeal({
        appealType: this.data.appealTypes[this.data.typeIndex].value,
        targetId: selected.experimentId,
        reason,
        evidenceUrls: this.buildEvidenceUrls(),
      })
      .then(() => {
        wx.showToast({ title: '申诉已提交', icon: 'success' });
        this.setData({ reason: '', evidenceUrls: '' });
        return this.loadAppeals();
      })
      .catch((error) => {
        wx.showToast({ title: error.message || '提交失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  },

  onTapAppeal(event) {
    const appeal = event.currentTarget.dataset.appeal;
    let content = `申诉类型：${appeal.typeText}`;
    content += `\n提交时间：${appeal.createdAtText}`;
    content += `\n申诉理由：${appeal.reason || '无'}`;
    content += `\n当前状态：${appeal.statusText}`;

    if (appeal.reviewComment) {
      content += `\n审核意见：${appeal.reviewComment}`;
    }
    if (appeal.reviewedAtText) {
      content += `\n审核时间：${appeal.reviewedAtText}`;
    }

    wx.showModal({
      title: '申诉详情',
      content,
      showCancel: false,
      confirmText: '知道了',
    });
  },
});
