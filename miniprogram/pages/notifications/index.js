const { api } = require('../../utils/request');
const { formatDateTime } = require('../../utils/format');

Page({
  data: {
    notifications: [],
    page: 0,
    size: 20,
    hasMore: true,
    loading: false,
    error: '',
  },

  onLoad() {
    this.loadNotifications(true);
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadNotifications(false);
    }
  },

  loadNotifications(reset) {
    const nextPage = reset ? 0 : this.data.page + 1;
    this.setData({ loading: true, error: '' });

    return api
      .getNotifications({ page: nextPage, size: this.data.size })
      .then((pageData) => {
        const list = pageData.list.map((item) =>
          Object.assign({}, item, { createdAtText: formatDateTime(item.createdAt) })
        );
        const notifications = reset ? list : this.data.notifications.concat(list);
        this.setData({
          notifications,
          page: pageData.page,
          hasMore: notifications.length < pageData.total,
        });
      })
      .catch((error) => {
        this.setData({ error: error.message || '通知加载失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  markAllRead() {
    api
      .markAllNotificationsRead()
      .then(() => this.loadNotifications(true))
      .then(() => wx.showToast({ title: '已全部标记', icon: 'success' }))
      .catch((error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }));
  },
});
