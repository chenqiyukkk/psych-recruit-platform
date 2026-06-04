const { API_BASE_URL } = require('../config/env');

function buildUrl(baseUrl, path, query = {}) {
  const normalizedBase = baseUrl.replace(/\/$/, '');
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const params = Object.keys(query)
    .filter((key) => query[key] !== undefined && query[key] !== null && query[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(query[key])}`);

  return params.length
    ? `${normalizedBase}${normalizedPath}?${params.join('&')}`
    : `${normalizedBase}${normalizedPath}`;
}

function unwrapResult(result) {
  if (!result || result.code !== 0) {
    throw new Error((result && result.message) || '请求失败');
  }
  return result.data;
}

function normalizePageData(payload) {
  if (Array.isArray(payload)) {
    return {
      list: payload,
      total: payload.length,
      page: 0,
      size: payload.length,
    };
  }

  return {
    list: payload && Array.isArray(payload.content) ? payload.content : [],
    total: payload && typeof payload.totalElements === 'number' ? payload.totalElements : 0,
    page: payload && typeof payload.number === 'number' ? payload.number : 0,
    size: payload && typeof payload.size === 'number' ? payload.size : 0,
  };
}

function request(options) {
  if (typeof wx === 'undefined') {
    return Promise.reject(new Error('wx runtime is required'));
  }

  const app = getApp();
  const token = app && app.getToken ? app.getToken() : wx.getStorageSync('psych_recruit_token');
  const header = Object.assign({}, options.header || {});

  if (options.auth !== false && token) {
    header.Authorization = `Bearer ${token}`;
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: buildUrl(API_BASE_URL, options.path, options.query),
      method: options.method || 'GET',
      data: options.data,
      header,
      success(response) {
        try {
          resolve(unwrapResult(response.data));
        } catch (error) {
          reject(error);
        }
      },
      fail(error) {
        reject(error);
      },
    });
  });
}

const api = {
  login(data) {
    return request({ path: '/api/auth/login', method: 'POST', data, auth: false });
  },

  wxLogin(data) {
    return request({ path: '/api/auth/wx-login', method: 'POST', data, auth: false });
  },

  getExperiments(query) {
    return request({ path: '/api/experiments', query }).then(normalizePageData);
  },

  getExperiment(id) {
    return request({ path: `/api/experiments/${id}` });
  },

  applyExperiment(id) {
    return request({ path: `/api/registrations/experiments/${id}`, method: 'POST' });
  },

  getProfile() {
    return request({ path: '/api/users/profile' });
  },

  getRegistrations() {
    return request({ path: '/api/registrations/my' });
  },

  getNotifications(query) {
    return request({ path: '/api/notifications/my', query }).then(normalizePageData);
  },

  getUnreadCount() {
    return request({ path: '/api/notifications/unread-count' });
  },

  markAllNotificationsRead() {
    return request({ path: '/api/notifications/read-all', method: 'PUT' });
  },
};

module.exports = {
  api,
  buildUrl,
  normalizePageData,
  request,
  unwrapResult,
};
