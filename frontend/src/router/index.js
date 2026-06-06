import { createRouter, createWebHistory } from 'vue-router';
import { ElMessage } from 'element-plus';
import MainLayout from '../layouts/MainLayout.vue';
import { useAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { guestOnly: true, title: '登录' },
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
        meta: { title: '申诉管理' },
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
        meta: { title: '系统配置' },
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

  if (!authStore.initialized && authStore.token) {
    try {
      await authStore.fetchProfile();
    } catch (_error) {
      if (to.path !== '/login') {
        return {
          path: '/login',
          query: { redirect: to.fullPath },
        };
      }
    }
  } else if (!authStore.initialized) {
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

  document.title = `${to.meta.title || '心试通'} · 心试通 Web 管理后台`;
  return true;
});

export default router;
