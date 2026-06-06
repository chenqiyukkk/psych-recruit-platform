import { defineStore } from 'pinia';
import { getProfile, login, logout } from '../api/auth';
import { WEB_ALLOWED_ROLES } from '../constants/roles';
import { clearToken, getToken, setToken } from '../utils/storage';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    profile: null,
    initialized: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isWebAllowed: (state) =>
      Boolean(state.profile?.role) && WEB_ALLOWED_ROLES.includes(state.profile.role),
  },
  actions: {
    async login(credentials) {
      const data = await login(credentials);
      this.token = data.token;
      setToken(data.token);
      await this.fetchProfile();
      return this.profile;
    },
    async fetchProfile() {
      if (!this.token) {
        this.profile = null;
        this.initialized = true;
        return null;
      }
      try {
        const profile = await getProfile();
        this.profile = profile;
        this.initialized = true;
        return profile;
      } catch (error) {
        this.logoutLocally();
        throw error;
      }
    },
    async logout() {
      try {
        await logout();
      } finally {
        this.logoutLocally();
      }
    },
    logoutLocally() {
      clearToken();
      this.token = '';
      this.profile = null;
      this.initialized = true;
    },
  },
});
