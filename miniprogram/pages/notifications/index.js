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

  onShow() {
    this.loadNotifications(true);
  },

  onPullDownRefresh() {
    this.loadNotifications(true).finally(() => wx.stopPullDownRefresh());
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

  onTapNotification(event) {
    const { id, isread, relatedtype, relatedid } = event.currentTarget.dataset;

    // 标记为已读
    if (!isread) {
      api.markNotificationRead(id).catch(() => {});
      // 乐观更新本地状态
      const list = this.data.notifications.map((item) => {
        if (item.id === id) {
          return Object.assign({}, item, { isRead: true });
        }
        return item;
      });
      this.setData({ notifications: list });
    }

    // 跳转到关联页面
    if (relatedtype && relatedid) {
      const routeMap = {
        experiment: '/pages/experiment-detail/index',
        // 更多关联页面路由可按业务扩展
      };
      const targetPath = routeMap[relatedtype];
      if (targetPath) {
        wx.navigateTo({ url: `${targetPath}?id=${relatedid}` });
      }
    }
  },

  markAllRead() {
    api
      .markAllNotificationsRead()
      .then(() => this.loadNotifications(true))
      .then(() => wx.showToast({ title: '已全部标记', icon: 'success' }))
      .catch((error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }));
  },
});
