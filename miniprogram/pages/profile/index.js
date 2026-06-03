const { api } = require('../../utils/request');
const { formatRegistration } = require('../../utils/format');

Page({
  data: {
    username: '',
    password: '',
    token: '',
    profile: null,
    registrations: [],
    unreadCount: 0,
    loading: false,
    loginLoading: false,
    error: '',
  },

  onShow() {
    const app = getApp();
    this.setData({ token: app.getToken() });
    if (this.data.token) {
      this.loadDashboard();
    }
  },

  onPullDownRefresh() {
    this.loadDashboard().finally(() => wx.stopPullDownRefresh());
  },

  onUsernameInput(event) {
    this.setData({ username: event.detail.value });
  },

  onPasswordInput(event) {
    this.setData({ password: event.detail.value });
  },

  login() {
    if (!this.data.username || !this.data.password) {
      wx.showToast({ title: '请输入账号和密码', icon: 'none' });
      return;
    }

    this.setData({ loginLoading: true });
    api
      .login({ username: this.data.username, password: this.data.password })
      .then((result) => {
        getApp().setToken(result.token);
        this.setData({ token: result.token, password: '' });
        return this.loadDashboard();
      })
      .then(() => {
        wx.showToast({ title: '登录成功', icon: 'success' });
      })
      .catch((error) => {
        wx.showToast({ title: error.message || '登录失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ loginLoading: false });
      });
  },

  logout() {
    getApp().clearToken();
    this.setData({
      token: '',
      profile: null,
      registrations: [],
      unreadCount: 0,
      username: '',
      password: '',
    });
  },

  loadDashboard() {
    if (!this.data.token) {
      return Promise.resolve();
    }

    this.setData({ loading: true, error: '' });
    return Promise.all([api.getProfile(), api.getRegistrations(), api.getUnreadCount()])
      .then(([profile, registrations, unreadCount]) => {
        this.setData({
          profile,
          registrations: registrations.map(formatRegistration),
          unreadCount,
        });
      })
      .catch((error) => {
        this.setData({ error: error.message || '个人中心加载失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  openNotifications() {
    wx.navigateTo({ url: '/pages/notifications/index' });
  },
});
