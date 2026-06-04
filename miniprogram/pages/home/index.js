const { api } = require('../../utils/request');
const { formatExperiment } = require('../../utils/format');

Page({
  data: {
    keyword: '',
    experiments: [],
    page: 0,
    size: 6,
    total: 0,
    hasMore: true,
    loading: false,
    loadingMore: false,
    error: '',
    loadMoreError: '',
    lastToken: '',
  },

  onLoad() {
    const token = getApp().getToken();
    this.setData({ lastToken: token });
    if (token) {
      this.loadExperiments(true);
    } else {
      this.setData({ error: '请先在个人页登录测试账号，再查看实验招募列表。' });
    }
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 });
    }
    const token = getApp().getToken();
    if (token && token !== this.data.lastToken) {
      this.setData({ lastToken: token });
      this.loadExperiments(true);
    } else if (!token && this.data.lastToken) {
      this.ensureToken();
    }
  },

  onPullDownRefresh() {
    this.loadExperiments(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) {
      this.loadExperiments(false);
    }
  },

  onKeywordInput(event) {
    this.setData({ keyword: event.detail.value });
  },

  onSearch() {
    if (!this.ensureToken()) {
      return;
    }
    this.loadExperiments(true);
  },

  onClearSearch() {
    this.setData({ keyword: '' });
    if (!this.ensureToken()) {
      return;
    }
    this.loadExperiments(true);
  },

  loadExperiments(reset) {
    if (!this.ensureToken()) {
      return Promise.resolve();
    }
    const nextPage = reset ? 0 : this.data.page + 1;
    this.setData({
      loading: reset,
      loadingMore: !reset,
      error: reset ? '' : this.data.error,
      loadMoreError: '',
    });

    return api
      .getExperiments({
        keyword: this.data.keyword.trim(),
        status: 'PUBLISHED',
        page: nextPage,
        size: this.data.size,
      })
      .then((pageData) => {
        const list = pageData.list.map(formatExperiment);
        const experiments = reset ? list : this.data.experiments.concat(list);
        this.setData({
          experiments,
          page: pageData.page,
          total: pageData.total,
          hasMore: experiments.length < pageData.total,
        });
      })
      .catch((error) => {
        const message = error.message || '实验列表加载失败';
        this.setData(reset ? { error: message } : { loadMoreError: message });
      })
      .finally(() => {
        this.setData({ loading: false, loadingMore: false });
      });
  },

  openDetail(event) {
    const { id } = event.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/experiment-detail/index?id=${id}` });
  },

  ensureToken() {
    const token = getApp().getToken();
    if (token) {
      this.setData({ lastToken: token });
      return true;
    }

    this.setData({
      experiments: [],
      total: 0,
      hasMore: false,
      loading: false,
      loadingMore: false,
      error: '请先在个人页登录测试账号，再查看实验招募列表。',
      loadMoreError: '',
      lastToken: '',
    });
    return false;
  },
});
