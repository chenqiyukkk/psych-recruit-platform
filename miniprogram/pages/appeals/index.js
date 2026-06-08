const { api } = require('../../utils/request');
const { formatAppeal } = require('../../utils/format');

const appealTypes = [
  { label: '信誉扣分', value: 'REPUTATION_DEDUCTION' },
  { label: '低评分', value: 'LOW_RATING' },
  { label: '支付争议', value: 'PAYMENT_DISPUTE' },
];

Page({
  data: {
    appealTypes,
    typeIndex: 0,
    targetId: '',
    reason: '',
    evidenceUrls: '',
    appeals: [],
    loading: false,
    submitting: false,
    error: '',
  },

  onLoad() {
    if (this.ensureLogin()) {
      this.loadAppeals();
    }
  },

  onPullDownRefresh() {
    this.loadAppeals().finally(() => wx.stopPullDownRefresh());
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

  onTargetInput(event) {
    this.setData({ targetId: event.detail.value });
  },

  onReasonInput(event) {
    this.setData({ reason: event.detail.value });
  },

  onEvidenceInput(event) {
    this.setData({ evidenceUrls: event.detail.value });
  },

  loadAppeals() {
    if (!this.ensureLogin()) {
      return Promise.resolve();
    }

    this.setData({ loading: true, error: '' });
    return api
      .getMyAppeals()
      .then((appeals) => {
        this.setData({ appeals: (Array.isArray(appeals) ? appeals : []).map(formatAppeal) });
      })
      .catch((error) => {
        this.setData({ error: error.message || '申诉记录加载失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
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

    const targetId = Number(this.data.targetId);
    const reason = this.data.reason.trim();
    if (!targetId) {
      wx.showToast({ title: '请输入关联记录ID', icon: 'none' });
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
        targetId,
        reason,
        evidenceUrls: this.buildEvidenceUrls(),
      })
      .then(() => {
        wx.showToast({ title: '申诉已提交', icon: 'success' });
        this.setData({ targetId: '', reason: '', evidenceUrls: '' });
        return this.loadAppeals();
      })
      .catch((error) => {
        wx.showToast({ title: error.message || '提交失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  },
});
