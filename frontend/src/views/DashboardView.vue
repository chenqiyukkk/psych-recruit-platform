<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">研究工作台</h1>
        <p class="page-subtitle">
          这里汇总你的账号状态、关键实验信息与平台动态，帮助你快速进入管理节奏。
        </p>
      </div>
      <el-button type="primary" @click="router.push('/experiments/create')">创建新实验</el-button>
    </div>

    <div class="grid-cards">
      <section class="metric-card">
        <div class="metric-card__label">当前角色</div>
        <div class="metric-card__value">{{ authStore.profile?.role || '--' }}</div>
        <div class="metric-card__hint">用户名：{{ authStore.profile?.username || '--' }}</div>
      </section>
      <section class="metric-card" style="background: linear-gradient(135deg, #0f766e, #0ea5e9)">
        <div class="metric-card__label">信誉分</div>
        <div class="metric-card__value">{{ authStore.profile?.reputationScore ?? '--' }}</div>
        <div class="metric-card__hint">累计评价次数：{{ authStore.profile?.totalReviews ?? 0 }}</div>
      </section>
      <section class="metric-card" style="background: linear-gradient(135deg, #7c3aed, #2563eb)">
        <div class="metric-card__label">研究者评分</div>
        <div class="metric-card__value">{{ authStore.profile?.researcherRating ?? '--' }}</div>
        <div class="metric-card__hint">对接后端 `GET /api/users/profile` 实时展示</div>
      </section>
    </div>

    <el-row :gutter="18">
      <el-col :xs="24" :xl="16">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="inline-actions" style="justify-content: space-between; width: 100%">
              <div>
                <strong>平台总览</strong>
                <div class="page-subtitle">管理员可直接读取 `/api/statistics/summary`；研究者账号会显示业务提示。</div>
              </div>
              <el-button @click="loadSummary">刷新</el-button>
            </div>
          </template>

          <el-skeleton :loading="summaryLoading" animated :rows="5">
            <template v-if="summary">
              <div class="grid-cards">
                <el-statistic title="平台用户数" :value="summary.userCount" />
                <el-statistic title="实验总数" :value="summary.experimentCount" />
                <el-statistic title="报名记录数" :value="summary.registrationCount" />
                <el-statistic title="评价记录数" :value="summary.reviewCount" />
              </div>
            </template>
            <el-empty v-else description="当前账号没有平台总览权限，后续仍可正常使用实验与报名审核功能。" />
          </el-skeleton>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="8">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div>
              <strong>本轮交付范围</strong>
              <div class="page-subtitle">已优先完成成员3最关键的 Web 端路径。</div>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item timestamp="P0" type="primary">
              登录鉴权、路由守卫、主布局与通知抽屉
            </el-timeline-item>
            <el-timeline-item timestamp="P1" type="success">
              实验列表、创建/编辑实验、实验详情、报名审核
            </el-timeline-item>
            <el-timeline-item timestamp="Next" type="warning">
              支付确认、评价页、申诉页、系统配置、个人资料编辑
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div>
          <strong>对接说明</strong>
          <div class="page-subtitle">本版本已按后端接口真实字段实现，便于后续直接联调。</div>
        </div>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="登录接口">POST /api/auth/login</el-descriptions-item>
        <el-descriptions-item label="个人信息接口">GET /api/users/profile</el-descriptions-item>
        <el-descriptions-item label="实验管理接口">/api/experiments + /api/config/*</el-descriptions-item>
        <el-descriptions-item label="报名审核接口">
          /api/registrations/experiment/{experimentId}、/approve、/reject、/api/sign-ins/*
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getPlatformSummary } from '../api/dashboard';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const summaryLoading = ref(false);
const summary = ref(null);

async function loadSummary() {
  summaryLoading.value = true;
  try {
    summary.value = await getPlatformSummary();
  } catch (_error) {
    summary.value = null;
  } finally {
    summaryLoading.value = false;
  }
}

onMounted(() => {
  loadSummary();
});
</script>
