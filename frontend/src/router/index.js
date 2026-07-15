import { createRouter, createWebHistory } from 'vue-router';
import { ElMessage } from 'element-plus';
import MainLayout from '../layouts/MainLayout.vue';
import { ROLE_ADMIN } from '../constants/roles';
import { useAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { guestOnly: true, title: '登录' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { guestOnly: true, title: '研究者注册' },
  },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard',
      },
      {
        path: '/dashboard',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: { title: '工作台' },
      },
      {
        path: '/experiments',
        name: 'experiments',
        component: () => import('../views/experiments/ExperimentListView.vue'),
        meta: { title: '实验管理' },
      },
      {
        path: '/experiments/create',
        name: 'experiment-create',
        component: () => import('../views/experiments/ExperimentFormView.vue'),
        meta: { title: '创建实验' },
      },
      {
        path: '/experiments/:id/edit',
        name: 'experiment-edit',
        component: () => import('../views/experiments/ExperimentFormView.vue'),
        meta: { title: '编辑实验' },
      },
      {
        path: '/experiments/:id',
        name: 'experiment-detail',
        component: () => import('../views/experiments/ExperimentDetailView.vue'),
        meta: { title: '实验详情' },
      },
      {
        path: '/registrations/review',
        name: 'registration-review',
        component: () => import('../views/registrations/RegistrationReviewView.vue'),
        meta: { title: '报名审核' },
      },
      {
        path: '/payments/confirm',
        name: 'payment-confirm',
        component: () => import('../views/payments/PaymentConfirmView.vue'),
        meta: { title: '支付确认' },
      },
      {
        path: '/appeals/manage',
        name: 'appeal-manage',
        component: () => import('../views/appeals/AppealManagementView.vue'),
        meta: { title: '申诉管理', roles: [ROLE_ADMIN] },
      },
      {
        path: '/reviews',
        name: 'reviews',
        component: () => import('../views/reviews/ReviewManagementView.vue'),
        meta: { title: '评价管理' },
      },
      {
        path: '/profile',
        name: 'profile',
        component: () => import('../views/profile/ProfileView.vue'),
        meta: { title: '个人资料' },
      },
      {
        path: '/config',
        name: 'config',
        component: () => import('../views/config/ConfigManagementView.vue'),
        meta: { title: '系统配置', roles: [ROLE_ADMIN] },
      },
      {
        path: '/admin/users',
        name: 'admin-users',
        component: () => import('../views/admin/AdminUsersView.vue'),
        meta: { title: '用户管理', roles: [ROLE_ADMIN] },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();

  // 访问公开页面（登录/注册）时不阻塞等待 profile，直接放行
  if (!authStore.initialized && authStore.token && !to.meta.guestOnly) {
    try {
      await authStore.fetchProfile();
    } catch (_error) {
      return {
        path: '/login',
        query: { redirect: to.fullPath },
      };
    }
  } else if (!authStore.initialized) {
    // 有 token 但去公开页面，后台静默验证不阻塞
    if (authStore.token) {
      authStore.fetchProfile().catch(() => {});
    }
    authStore.initialized = true;
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    };
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return '/dashboard';
  }

  if (to.meta.requiresAuth && authStore.profile && !authStore.isWebAllowed) {
    ElMessage.error('当前账号角色仅支持小程序端使用，请使用研究者或管理员账号登录。');
    authStore.logoutLocally();
    return '/login';
  }

  if (to.meta.requiresAuth && to.meta.roles?.length && !to.meta.roles.includes(authStore.profile?.role)) {
    ElMessage.warning('当前账号无权限访问该页面，已返回工作台。');
    return '/dashboard';
  }

  document.title = `${to.meta.title || '心试通'} · 心试通 Web 管理后台`;
  return true;
});

export default router;
