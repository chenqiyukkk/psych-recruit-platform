const { api } = require('../../utils/request');
const { formatAppeal, formatRegistration } = require('../../utils/format');

const appealTypes = [
  { label: '信誉扣分', value: 'REPUTATION_DEDUCTION' },
  { label: '低评分', value: 'LOW_RATING' },
  { label: '支付争议', value: 'PAYMENT_DISPUTE' },
];

function formatCompletedExperiment(registration, experiment) {
  const title = experiment ? experiment.title : `实验 #${registration.experimentId}`;
  return Object.assign({}, registration, {
    experimentTitle: title,
    experimentLocation: experiment ? experiment.location : '',
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

  onLoad() {
    if (this.ensureLogin()) {
      this.loadPageData();
    }
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
    return api.getMyAppeals().then((appeals) => {
      this.setData({ appeals: (Array.isArray(appeals) ? appeals : []).map(formatAppeal) });
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

    const appealType = this.data.appealTypes[this.data.typeIndex].value;
    this.setData({ submitting: true });
    api
      .createAppeal({
        appealType,
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
    const content = [
      `申诉类型：${appeal.appealTypeText}`,
      `关联实验：#${appeal.targetId}`,
      `提交时间：${appeal.createdAtText}`,
      `申诉理由：${appeal.reason || '无'}`,
      `当前状态：${appeal.statusText}`,
      appeal.reviewComment ? `审核意见：${appeal.reviewComment}` : '',
    ]
      .filter(Boolean)
      .join('\n');

    wx.showModal({
      title: '申诉详情',
      content,
      showCancel: false,
      confirmText: '知道了',
    });
  },
});
