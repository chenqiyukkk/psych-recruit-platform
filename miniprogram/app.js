App({
  globalData: {
    tokenKey: 'psych_recruit_token',
  },

  getToken() {
    return wx.getStorageSync(this.globalData.tokenKey) || '';
  },

  setToken(token) {
    wx.setStorageSync(this.globalData.tokenKey, token);
  },

  clearToken() {
    wx.removeStorageSync(this.globalData.tokenKey);
  },
});
