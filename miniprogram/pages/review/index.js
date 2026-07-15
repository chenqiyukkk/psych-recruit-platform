const { api } = require('../../utils/request');

Page({
  data: {
    registrationId: '',
    experimentTitle: '',
    rating: 5,
    comment: '',
    submitting: false,
  },

  onLoad(options) {
    this.setData({
      registrationId: options.registrationId || '',
      experimentTitle: decodeURIComponent(options.title || ''),
    });
  },

  onRatingChange(e) {
    this.setData({ rating: e.detail.value });
  },

  onCommentInput(e) {
    this.setData({ comment: e.detail.value });
  },

  submit() {
    this.setData({ submitting: true });
    api.createReview(this.data.registrationId, {
      rating: this.data.rating,
      reviewType: 'SUBJECT_TO_RESEARCHER',
      comment: this.data.comment || undefined,
    })
      .then(() => {
        wx.showToast({ title: '评价成功', icon: 'success' });
        setTimeout(() => wx.navigateBack(), 1500);
      })
      .catch((error) => {
        wx.showToast({ title: error.message || '评价失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  },
});
