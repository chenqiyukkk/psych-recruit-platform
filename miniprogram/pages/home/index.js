const { api } = require('../../utils/request');
const { formatExperiment } = require('../../utils/format');

Page({
  data: {
    keyword: '',
    experiments: [],
    page: 0,
    size: 8,
    total: 0,
    hasMore: true,
    loading: false,
    error: '',
  },

  onLoad() {
    this.loadExperiments(true);
  },

  onPullDownRefresh() {
    this.loadExperiments(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadExperiments(false);
    }
  },

  onKeywordInput(event) {
    this.setData({ keyword: event.detail.value });
  },

  onSearch() {
    this.loadExperiments(true);
  },

  onClearSearch() {
    this.setData({ keyword: '' });
    this.loadExperiments(true);
  },

  loadExperiments(reset) {
    const nextPage = reset ? 0 : this.data.page + 1;
    this.setData({ loading: true, error: '' });

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
        this.setData({ error: error.message || '实验列表加载失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  openDetail(event) {
    const { id } = event.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/experiment-detail/index?id=${id}` });
  },
});
