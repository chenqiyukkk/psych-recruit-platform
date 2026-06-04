const { api } = require('../../utils/request');
const { formatExperiment } = require('../../utils/format');

Page({
  data: {
    id: null,
    experiment: null,
    loading: true,
    applying: false,
    applied: false,
    error: '',
  },

  onLoad(options) {
    this.setData({ id: options.id });
    this.loadDetail(options.id);
  },

  loadDetail(id) {
    this.setData({ loading: true, error: '', applied: false });
    api
      .getExperiment(id)
      .then((experiment) => {
        this.setData({ experiment: formatExperiment(experiment) });
      })
      .catch((error) => {
        this.setData({ error: error.message || '实验详情加载失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  applyExperiment() {
    if (!this.data.id || this.data.applying) {
      return;
    }

    if (!getApp().getToken()) {
      wx.switchTab({ url: '/pages/profile/index' });
      return;
    }

    this.setData({ applying: true });
    api
      .applyExperiment(this.data.id)
      .then(() => {
        this.setData({ applied: true });
        wx.showToast({ title: '报名成功', icon: 'success' });
      })
      .catch((error) => {
        if (error.message && error.message.indexOf('重复报名') !== -1) {
          this.setData({ applied: true });
        }
        wx.showToast({ title: error.message || '报名失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ applying: false });
      });
  },
});
