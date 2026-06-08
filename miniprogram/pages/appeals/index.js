const { api } = require('../../utils/request');
const { formatDateTime } = require('../../utils/format');

// 申诉类型文本映射（与后端 ENUM 对齐）
var APPEAL_TYPE_MAP = {
  REPUTATION_DEDUCTION: '信誉扣分申诉',
  LOW_RATING: '低评分申诉',
  PAYMENT_DISPUTE: '支付争议申诉',
};

// 申诉状态文本及样式映射
var APPEAL_STATUS_MAP = {
  PENDING: { text: '待审核', className: 'status-warning' },
  UNDER_REVIEW: { text: '审核中', className: 'status-warning' },
  APPROVED: { text: '已通过', className: 'status-success' },
  REJECTED: { text: '已拒绝', className: 'status-danger' },
};

function formatAppeal(item) {
  var typeText = APPEAL_TYPE_MAP[item.appealType] || item.appealType || '未知类型';
  var statusMeta = APPEAL_STATUS_MAP[item.status] || { text: item.status || '未知', className: 'status-muted' };

  return Object.assign({}, item, {
    createdAtText: formatDateTime(item.createdAt),
    reviewedAtText: formatDateTime(item.reviewedAt),
    typeText: typeText,
    statusText: statusMeta.text,
    statusClassName: statusMeta.className,
  });
}

Page({
  data: {
    appeals: [],
    loading: false,
    error: '',
  },

  onShow() {
    this.loadAppeals();
  },

  onPullDownRefresh() {
    this.loadAppeals().finally(function () {
      wx.stopPullDownRefresh();
    });
  },

  loadAppeals() {
    this.setData({ loading: true, error: '' });

    return api
      .getMyAppeals()
      .then(function (list) {
        var appeals = (Array.isArray(list) ? list : []).map(formatAppeal);
        this.setData({ appeals: appeals });
      }.bind(this))
      .catch(function (error) {
        this.setData({ error: error.message || '申诉记录加载失败' });
      }.bind(this))
      .finally(function () {
        this.setData({ loading: false });
      }.bind(this));
  },

  onTapAppeal(event) {
    var appeal = event.currentTarget.dataset.appeal;
    // 点击查看申诉详情 — 显示审核意见等更多信息
    var statusMeta = APPEAL_STATUS_MAP[appeal.status] || { text: appeal.status };
    var typeText = APPEAL_TYPE_MAP[appeal.appealType] || appeal.appealType;

    var content = '申诉类型：' + typeText;
    content += '\n提交时间：' + appeal.createdAtText;
    content += '\n申诉理由：' + (appeal.reason || '无');
    content += '\n当前状态：' + statusMeta.text;

    if (appeal.reviewComment) {
      content += '\n审核意见：' + appeal.reviewComment;
    }
    if (appeal.reviewedAtText) {
      content += '\n审核时间：' + appeal.reviewedAtText;
    }

    wx.showModal({
      title: '申诉详情',
      content: content,
      showCancel: false,
      confirmText: '知道了',
    });
  },

  onNewAppeal() {
    // 跳转到发起申诉页面（由成员4实现）
    wx.navigateTo({ url: '/pages/appeal/index' });
  },
});
