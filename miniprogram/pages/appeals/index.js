const { api } = require('../../utils/request');
const { formatAppeal, formatRegistration } = require('../../utils/format');

const appealTypes = [
  { label: '信誉扣分', value: 'REPUTATION_DEDUCTION' },
  { label: '低评分', value: 'LOW_RATING' },
  { label: '支付争议', value: 'PAYMENT_DISPUTE' },
];

Page({
  data: {
    appealTypes,
    typeIndex: 0,
    targetIndex: -1,
    // 根据申诉类型动态切换的目标列表
    targets: [],
    targetOptions: [],
    // 已完成实验（用于 PAYMENT_DISPUTE）
    completedRegistrations: [],
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

  onShow() {
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
    const idx = Number(event.detail.value);
    this.setData({ typeIndex: idx, targetIndex: -1 });
    this.loadTargets();
  },

  onTargetChange(event) {
    this.setData({ targetIndex: Number(event.detail.value) });
  },

  onReasonInput(event) {
    this.setData({ reason: event.detail.value });
  },

  onEvidenceInput(event) {
    this.setData({ evidenceUrls: event.detail.value });
  },

  loadPageData() {
    if (!this.ensureLogin()) return Promise.resolve();
    this.setData({ loading: true, error: '' });
    return Promise.all([this.loadAppeals(), this.loadTargets()])
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

  loadTargets() {
    const type = this.data.appealTypes[this.data.typeIndex].value;
    if (type === 'REPUTATION_DEDUCTION') {
      return this.loadReputationTargets();
    } else if (type === 'LOW_RATING') {
      return this.loadReviewTargets();
    } else {
      return this.loadPaymentTargets();
    }
  },

  // 信誉扣分：加载信誉变动日志，选扣分的
  loadReputationTargets() {
    return api.getMyReputationLogs().then((logs) => {
      const list = (Array.isArray(logs) ? logs : [])
        .filter((l) => l.scoreDelta < 0)
        .map((l) => ({
          id: l.id,
          text: `${l.changeType} · ${l.scoreDelta > 0 ? '+' : ''}${l.scoreDelta}分 · ${l.reason || '无说明'}`,
        }));
      this.setData({
        targets: list,
        targetOptions: list.map((t) => t.text),
        targetIndex: list.length ? Math.max(0, Math.min(this.data.targetIndex, list.length - 1)) : -1,
      });
    }).catch(() => {
      this.setData({ targets: [], targetOptions: [], targetIndex: -1 });
    });
  },

  // 低评分：加载收到的评价
  loadReviewTargets() {
    return api.getReceivedReviews().then((reviews) => {
      const list = (Array.isArray(reviews) ? reviews : []).map((r) => ({
        id: r.id,
        text: `评分${r.rating}分 · ${r.comment || '无评论'} · ${r.createdAt || ''}`,
      }));
      this.setData({
        targets: list,
        targetOptions: list.map((t) => t.text),
        targetIndex: list.length ? Math.max(0, Math.min(this.data.targetIndex, list.length - 1)) : -1,
      });
    }).catch(() => {
      this.setData({ targets: [], targetOptions: [], targetIndex: -1 });
    });
  },

  // 支付争议：加载支付记录
  loadPaymentTargets() {
    return api.getMyPaymentRecords().then((records) => {
      const list = (Array.isArray(records) ? records : []).map((r) => ({
        id: r.id,
        text: `金额: ${r.amount || '--'}元 · 状态: ${r.status} · 报名#${r.registrationId}`,
      }));
      this.setData({
        targets: list,
        targetOptions: list.map((t) => t.text),
        targetIndex: list.length ? Math.max(0, Math.min(this.data.targetIndex, list.length - 1)) : -1,
      });
    }).catch(() => {
      this.setData({ targets: [], targetOptions: [], targetIndex: -1 });
    });
  },

  buildEvidenceUrls() {
    const value = this.data.evidenceUrls.trim();
    if (!value) return '';
    return JSON.stringify(
      value.split(/\n|,|，/).map((url) => url.trim()).filter(Boolean)
    );
  },

  submitAppeal() {
    if (!this.ensureLogin() || this.data.submitting) return;

    const target = this.data.targets[this.data.targetIndex];
    const reason = this.data.reason.trim();
    if (!target) {
      wx.showToast({ title: '请选择申诉对象', icon: 'none' });
      return;
    }
    if (reason.length < 10) {
      wx.showToast({ title: '申诉理由至少10字', icon: 'none' });
      return;
    }

    const appealType = this.data.appealTypes[this.data.typeIndex].value;
    this.setData({ submitting: true });
    api.createAppeal({
      appealType,
      targetId: target.id,
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
      `关联ID：#${appeal.targetId}`,
      `提交时间：${appeal.createdAtText}`,
      `申诉理由：${appeal.reason || '无'}`,
      `当前状态：${appeal.statusText}`,
      appeal.reviewComment ? `审核意见：${appeal.reviewComment}` : '',
    ].filter(Boolean).join('\n');

    wx.showModal({
      title: '申诉详情',
      content,
      showCancel: false,
      confirmText: '知道了',
    });
  },
});
