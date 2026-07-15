const { api } = require('../../utils/request');
const { formatRegistration } = require('../../utils/format');

Page({
  data: {
    username: '',
    password: '',
    token: '',
    profile: null,
    registrations: [],
    reviews: [],
    myReviews: [],
    unreadCount: 0,
    editGenderIndex: 0,
    editAgeGroup: '',
    editSubmitting: false,
    genderOptions: ['男', '女'],
    genderValues: ['MALE', 'FEMALE'],
    editMajorIndex: 0,
    editHandednessIndex: 0,
    majorOptions: ['心理学类', '计算机类', '文学类', '理学类', '工学类', '医学类', '经管类'],
    handednessOptions: ['右利手', '左利手', '混合利手'],
    handednessValues: ['RIGHT', 'LEFT', 'MIXED'],
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
      reviews: [],
      myReviews: [],
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
    return Promise.all([
      api.getProfile(),
      api.getRegistrations(),
      api.getUnreadCount(),
      api.getReceivedReviews().catch(() => []),
      api.getMyReviews().catch(() => []),
    ])
      .then(([profile, registrations, unreadCount, reviews, myReviews]) => {
        return this.enrichRegistrations(registrations).then((items) => {
          const reviewedIds = new Set((myReviews || []).map(r => r.registrationId));
          const enriched = items.map(item => ({
            ...item,
            reviewed: reviewedIds.has(item.id),
          }));
          const gender = profile.gender || '';
          const genderIdx = this.data.genderValues.indexOf(gender);
          this.setData({
            profile,
            registrations: enriched,
            reviews: Array.isArray(reviews) ? reviews : [],
            myReviews: myReviews || [],
            unreadCount,
            editGenderIndex: genderIdx >= 0 ? genderIdx : 0,
            editAgeGroup: profile.ageGroup || '',
            editMajorIndex: this.data.majorOptions.indexOf(profile.majorCategory || '') || 0,
            editHandednessIndex: this.data.handednessValues.indexOf(profile.handedness || '') || 0,
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

  openReviewPage(e) {
    const item = e.currentTarget.dataset.item;
    wx.navigateTo({
      url: `/pages/review/index?registrationId=${item.id}&title=${encodeURIComponent(item.experimentTitle || '')}`,
    });
  },

  onEditGenderChange(e) {
    this.setData({ editGenderIndex: Number(e.detail.value) });
  },

  onEditAgeGroupInput(e) {
    this.setData({ editAgeGroup: e.detail.value });
  },

  onEditMajorChange(e) {
    this.setData({ editMajorIndex: Number(e.detail.value) });
  },

  onEditHandednessChange(e) {
    this.setData({ editHandednessIndex: Number(e.detail.value) });
  },

  submitEditProfile() {
    if (!this.data.editAgeGroup.trim()) {
      wx.showToast({ title: '请填写年龄段', icon: 'none' });
      return;
    }
    this.setData({ editSubmitting: true });
    const genderVal = this.data.genderValues[this.data.editGenderIndex];
    const majorVal = this.data.majorOptions[this.data.editMajorIndex];
    const handedVal = this.data.handednessValues[this.data.editHandednessIndex];
    api.updateProfile({
      gender: genderVal,
      ageGroup: this.data.editAgeGroup.trim() || null,
      majorCategory: majorVal,
      handedness: handedVal,
    })
      .then(() => {
        wx.showToast({ title: '个人信息已保存', icon: 'success' });
        return this.loadDashboard();
      })
      .catch((error) => {
        wx.showToast({ title: error.message || '更新失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ editSubmitting: false });
      });
  },

  async enrichRegistrations(registrations) {
    const list = Array.isArray(registrations) ? registrations : [];
    const formatted = list.map(formatRegistration);
    const ids = Array.from(new Set(formatted.map((item) => item.experimentId).filter(Boolean)));

    if (!ids.length) {
      return Promise.resolve(formatted);
    }

    // Load experiments
    const expPairs = await Promise.all(
      ids.map((id) =>
        api.getExperiment(id).then((e) => [id, e]).catch(() => [id, null])
      )
    );
    const experimentMap = {};
    for (const [id, experiment] of expPairs) {
      if (experiment) experimentMap[id] = experiment;
    }

    // Load payment records
    const paymentMap = {};
    await Promise.all(
      formatted.map((item) =>
        api.getPaymentRecord(item.id)
          .then((record) => { paymentMap[item.id] = record; })
          .catch(() => {})
      )
    );

    return formatted.map((item) => {
      const experiment = experimentMap[item.experimentId];
      const payment = paymentMap[item.id];
      return Object.assign({}, item, {
        experimentTitle: experiment ? experiment.title : `实验 #${item.experimentId}`,
        experimentLocation: experiment ? experiment.location : '',
        paymentStatus: payment ? payment.status : null,
        paymentAmount: payment ? payment.amount : null,
      });
    });
  },
});
