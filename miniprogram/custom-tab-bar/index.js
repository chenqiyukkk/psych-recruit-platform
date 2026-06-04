Component({
  data: {
    selected: 0,
    list: [
      {
        pagePath: '/pages/home/index',
        text: '招募',
        iconClass: 'experiment',
      },
      {
        pagePath: '/pages/profile/index',
        text: '个人',
        iconClass: 'profile',
      },
    ],
  },

  methods: {
    switchTab(event) {
      const { path, index } = event.currentTarget.dataset;
      if (index === this.data.selected) {
        return;
      }
      wx.switchTab({ url: path });
    },
  },
});
