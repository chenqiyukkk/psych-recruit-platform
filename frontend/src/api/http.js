import axios from 'axios';
import { ElMessage } from 'element-plus';
import { clearToken, getToken } from '../utils/storage';

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
});

http.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const payload = response.data;
    if (
      payload &&
      typeof payload === 'object' &&
      Object.prototype.hasOwnProperty.call(payload, 'code') &&
      Object.prototype.hasOwnProperty.call(payload, 'message')
    ) {
      if (payload.code !== 0) {
        const error = new Error(payload.message || '请求失败');
        error.code = payload.code;
        throw error;
      }
      return payload.data;
    }
    return payload;
  },
  (error) => {
    const status = error?.response?.status;
    const message = error?.response?.data?.message || error.message || '请求失败';
    const silent = Boolean(error?.config?.silent || error?.silent);

    // 401 代表 token 失效或未登录，需要清理本地凭据；
    // 403 很多时候只是当前角色无权限访问某个接口（例如研究者访问管理员统计），
    // 不应把正常登录态一起清掉，否则后续业务请求会丢失 Authorization 头。
    if (status === 401) {
      clearToken();
    }

    if (!silent) {
      ElMessage.error(message);
    }

    return Promise.reject(error);
  },
);

export default http;
