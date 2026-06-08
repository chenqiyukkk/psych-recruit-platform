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
    wxLoginLoading: false,
    showDevLogin: false,
    error: '',
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 });
    }
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

  toggleDevLogin() {
    this.setData({ showDevLogin: !this.data.showDevLogin });
  },

  loginByWechat() {
    if (this.data.wxLoginLoading) {
      return;
    }

    if (typeof wx === 'undefined' || !wx.login) {
      wx.showToast({ title: '当前环境不支持微信登录', icon: 'none' });
      return;
    }

    this.setData({ wxLoginLoading: true });
    wx.login({
      success: (res) => {
        if (!res.code) {
          wx.showToast({ title: '微信登录失败', icon: 'none' });
          this.setData({ wxLoginLoading: false });
          return;
        }

        api
          .wxLogin({ code: res.code })
          .then((result) => {
            getApp().setToken(result.token);
            this.setData({ token: result.token });
            return this.loadDashboard();
          })
          .then(() => {
            wx.showToast({ title: '登录成功', icon: 'success' });
          })
          .catch((error) => {
            wx.showToast({ title: error.message || '微信登录接口待完成', icon: 'none' });
          })
          .finally(() => {
            this.setData({ wxLoginLoading: false });
          });
      },
      fail: () => {
        wx.showToast({ title: '微信登录失败', icon: 'none' });
        this.setData({ wxLoginLoading: false });
      },
    });
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
      showDevLogin: false,
    });
  },

  loadDashboard() {
    if (!this.data.token) {
      return Promise.resolve();
    }

    this.setData({ loading: true, error: '' });
    return Promise.all([api.getProfile(), api.getRegistrations(), api.getUnreadCount()])
      .then(([profile, registrations, unreadCount]) => {
        return this.enrichRegistrations(registrations).then((items) => {
          this.setData({
            profile,
            registrations: items,
            unreadCount,
          });
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

  openAppeals() {
    wx.navigateTo({ url: '/pages/appeals/index' });
  },

  enrichRegistrations(registrations) {
    const list = Array.isArray(registrations) ? registrations : [];
    const formatted = list.map(formatRegistration);
    const ids = Array.from(new Set(formatted.map((item) => item.experimentId).filter(Boolean)));

    if (!ids.length) {
      return Promise.resolve(formatted);
    }

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
        if (experiment) {
          map[id] = experiment;
        }
        return map;
      }, {});

      return formatted.map((item) => {
        const experiment = experimentMap[item.experimentId];
        return Object.assign({}, item, {
          experimentTitle: experiment ? experiment.title : `实验 #${item.experimentId}`,
          experimentLocation: experiment ? experiment.location : '',
        });
      });
    });
  },
});
