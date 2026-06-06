<template>
  <el-container class="layout-shell">
    <el-aside :width="collapsed ? '88px' : '248px'" class="layout-aside">
      <div class="brand-block">
        <div class="brand-mark">Ψ</div>
        <div v-if="!collapsed" class="brand-copy">
          <div class="brand-title">心试通</div>
          <div class="brand-subtitle">研究者 / 管理者工作台</div>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        class="side-menu"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><House /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/experiments">
          <el-icon><Files /></el-icon>
          <span>实验管理</span>
        </el-menu-item>
        <el-menu-item index="/registrations/review">
          <el-icon><Finished /></el-icon>
          <span>报名审核</span>
        </el-menu-item>
        <el-menu-item index="/payments/confirm">
          <el-icon><Coin /></el-icon>
          <span>支付确认</span>
        </el-menu-item>
        <el-menu-item index="/reviews">
          <el-icon><ChatDotRound /></el-icon>
          <span>评价管理</span>
        </el-menu-item>
        <el-menu-item index="/config">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/appeals/manage">
          <el-icon><Warning /></el-icon>
          <span>申诉管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-button text circle @click="collapsed = !collapsed">
            <el-icon :size="18"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          </el-button>
          <div>
            <div class="header-greeting">欢迎回来</div>
            <div class="header-meta">{{ authStore.profile?.username || '未登录' }} · {{ authStore.profile?.role || '--' }}</div>
          </div>
        </div>
        <div class="header-right">
          <el-badge :value="unreadCount" :hidden="!unreadCount">
            <el-button text type="primary" @click="drawerVisible = true">通知</el-button>
          </el-badge>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-pill">
              <el-avatar :size="36" class="user-avatar">{{ initials }}</el-avatar>
              <span>{{ authStore.profile?.username || '访客' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-drawer v-model="drawerVisible" title="通知中心" size="420px">
    <div class="notification-toolbar">
      <el-button type="primary" plain @click="readAll">全部设为已读</el-button>
      <el-button @click="loadNotifications">刷新</el-button>
    </div>
    <el-empty v-if="!notifications.length" description="暂无通知" />
    <div v-else class="notification-list">
      <el-card v-for="item in notifications" :key="item.id" class="notification-card" shadow="hover">
        <div class="notification-card__head">
          <div>
            <div class="notification-title">{{ item.title || '系统通知' }}</div>
            <div class="notification-time">{{ formatDateTime(item.createdAt) }}</div>
          </div>
          <el-tag :type="item.isRead ? 'info' : 'danger'">{{ item.isRead ? '已读' : '未读' }}</el-tag>
        </div>
        <div class="notification-content">{{ item.content || '暂无内容' }}</div>
        <div class="notification-actions">
          <el-button v-if="!item.isRead" type="primary" link @click="readOne(item.id)">标记已读</el-button>
        </div>
      </el-card>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import {
  ArrowDown,
  ChatDotRound,
  Coin,
  Expand,
  Files,
  Finished,
  Fold,
  House,
  Setting,
  Warning,
} from '@element-plus/icons-vue';
import {
  getNotifications,
  getUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '../api/dashboard';
import { useAuthStore } from '../stores/auth';
import { formatDateTime } from '../utils/format';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const collapsed = ref(false);
const drawerVisible = ref(false);
const notifications = ref([]);
const unreadCount = ref(0);

const activeMenu = computed(() => {
  if (route.path.startsWith('/registrations')) {
    return '/registrations/review';
  }
  if (route.path.startsWith('/payments')) {
    return '/payments/confirm';
  }
  if (route.path.startsWith('/reviews')) {
    return '/reviews';
  }
  if (route.path.startsWith('/config')) {
    return '/config';
  }
  if (route.path.startsWith('/appeals')) {
    return '/appeals/manage';
  }
  if (route.path.startsWith('/experiments')) {
    return '/experiments';
  }
  return route.path;
});

const initials = computed(() => authStore.profile?.username?.slice(0, 1)?.toUpperCase() || '心');
const isAdmin = computed(() => authStore.profile?.role === '管理员');

async function loadNotifications() {
  const data = await getNotifications({ page: 0, size: 8 });
  notifications.value = data.content || [];
}

async function loadUnreadCount() {
  unreadCount.value = await getUnreadCount();
}

async function readOne(id) {
  await markNotificationRead(id);
  ElMessage.success('已标记为已读');
  await Promise.all([loadNotifications(), loadUnreadCount()]);
}

async function readAll() {
  await markAllNotificationsRead();
  ElMessage.success('全部通知已设为已读');
  await Promise.all([loadNotifications(), loadUnreadCount()]);
}

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile');
    return;
  }

  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await authStore.logout();
    router.push('/login');
  }
}

watch(drawerVisible, async (visible) => {
  if (visible) {
    await Promise.all([loadNotifications(), loadUnreadCount()]);
  }
});

onMounted(async () => {
  await Promise.all([loadNotifications(), loadUnreadCount()]);
});
</script>

<style scoped>
.layout-shell {
  min-height: 100vh;
}

.layout-aside {
  background: linear-gradient(180deg, #0f172a 0%, #172554 100%);
  color: #e2e8f0;
  padding: 22px 18px;
  transition: width 0.25s ease;
  overflow: hidden;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 26px;
}

.brand-mark {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #eff6ff;
  box-shadow: 0 10px 30px rgba(37, 99, 235, 0.35);
}

.brand-title {
  font-size: 18px;
  font-weight: 700;
  color: #f8fafc;
}

.brand-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.78);
}

.side-menu {
  border-right: none;
  background: transparent;
}

.side-menu :deep(.el-menu) {
  border-right: none;
}

.side-menu :deep(.el-menu-item) {
  border-radius: 14px;
  color: #cbd5e1;
  margin-bottom: 8px;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: rgba(59, 130, 246, 0.18);
  color: #eff6ff;
}

.side-menu :deep(.el-menu-item:hover) {
  background: rgba(148, 163, 184, 0.14);
  color: #eff6ff;
}

.layout-header {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.header-left,
.header-right,
.user-pill {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-greeting {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.header-meta {
  margin-top: 4px;
  font-size: 13px;
  color: #64748b;
}

.user-pill {
  padding: 6px 10px 6px 6px;
  border-radius: 999px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 30px rgba(148, 163, 184, 0.16);
}

.user-avatar {
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  color: #eff6ff;
}

.layout-main {
  padding: 28px;
}

.notification-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 16px;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.notification-card {
  border-radius: 18px;
}

.notification-card__head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.notification-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.notification-time {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

.notification-content {
  margin-top: 12px;
  font-size: 14px;
  color: #334155;
  line-height: 1.7;
}

.notification-actions {
  margin-top: 12px;
}
</style>
