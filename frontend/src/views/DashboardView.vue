<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ authStore.isAdmin ? '管理工作台' : '研究工作台' }}</h1>
        <p class="page-subtitle">
          {{
            authStore.isAdmin
              ? '在这里查看平台概况、治理入口与待处理事项，帮助你从整体视角维护平台运行。'
              : '在这里查看账号概况、常用工作入口与实验流程建议，帮助你更顺畅地推进研究任务。'
          }}
        </p>
      </div>
      <div class="inline-actions">
        <el-button v-if="authStore.isAdmin" @click="router.push('/config')">进入系统配置</el-button>
        <el-button type="primary" @click="router.push('/experiments/create')">创建新实验</el-button>
      </div>
    </div>

    <div class="grid-cards">
      <section class="metric-card">
        <div class="metric-card__label">当前角色</div>
        <div class="metric-card__value">{{ authStore.profile?.role || '--' }}</div>
        <div class="metric-card__hint">用户名：{{ authStore.profile?.username || '--' }}</div>
      </section>

      <section
        v-if="authStore.isResearcher"
        class="metric-card"
        style="background: linear-gradient(135deg, #0f766e, #0ea5e9)"
      >
        <div class="metric-card__label">信誉分</div>
        <div class="metric-card__value">{{ authStore.profile?.reputationScore ?? '--' }}</div>
        <div class="metric-card__hint">累计评价次数：{{ authStore.profile?.totalReviews ?? 0 }}</div>
      </section>

      <section
        v-if="authStore.isResearcher"
        class="metric-card"
        style="background: linear-gradient(135deg, #7c3aed, #2563eb)"
      >
        <div class="metric-card__label">研究者评分</div>
        <div class="metric-card__value">{{ authStore.profile?.researcherRating ?? '--' }}</div>
        <div class="metric-card__hint">持续观察参与者对研究流程和履约表现的反馈</div>
      </section>

      <section
        v-if="authStore.isAdmin"
        class="metric-card"
        style="background: linear-gradient(135deg, #0f766e, #0ea5e9)"
      >
        <div class="metric-card__label">平台用户数</div>
        <div class="metric-card__value">{{ summary?.userCount ?? '--' }}</div>
        <div class="metric-card__hint">覆盖研究者、管理员与用户侧账号</div>
      </section>

      <section
        v-if="authStore.isAdmin"
        class="metric-card"
        style="background: linear-gradient(135deg, #7c3aed, #2563eb)"
      >
        <div class="metric-card__label">待处理申诉</div>
        <div class="metric-card__value">{{ pendingAppeals }}</div>
        <div class="metric-card__hint">可前往申诉管理页继续处理争议与异常流程</div>
      </section>
    </div>

    <el-row :gutter="18">
      <el-col :xs="24" :xl="16">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="inline-actions" style="justify-content: space-between; width: 100%">
              <div>
                <strong>{{ authStore.isAdmin ? '平台概况' : '研究者视图' }}</strong>
                <div class="page-subtitle">
                  {{
                    authStore.isAdmin
                      ? '集中查看平台当前的核心运行指标与治理关注点。'
                      : '聚焦研究者在 Web 后台中最常使用的工作路径与实验处理节奏。'
                  }}
                </div>
              </div>
              <el-button v-if="authStore.isAdmin" @click="refreshAdminOverview">刷新</el-button>
            </div>
          </template>

          <template v-if="authStore.isAdmin">
            <el-skeleton :loading="summaryLoading || appealsLoading" animated :rows="5">
              <template v-if="summary">
                <div class="admin-grid">
                  <div class="focus-card">
                    <div class="focus-card__label">平台概况</div>
                    <div class="focus-card__title">全平台已有 {{ summary.experimentCount }} 个实验、{{ summary.registrationCount }} 条报名记录</div>
                    <div class="focus-card__text">
                      当前评价记录共 {{ summary.reviewCount }} 条。建议优先关注申诉管理、系统配置与核心实验流程的整体运行状态。
                    </div>
                  </div>
                  <div class="focus-card focus-card--secondary">
                    <div class="focus-card__label">治理重点</div>
                    <div class="focus-card__title">待处理申诉 {{ pendingAppeals }} 条</div>
                    <div class="focus-card__text">
                      申诉、系统配置与平台级治理功能已集中在管理员视图中，便于你统一处理异常与争议流程。
                    </div>
                  </div>
                </div>
              </template>
              <el-empty v-else description="当前暂时无法获取平台概况，请稍后再试。" />
            </el-skeleton>
          </template>

          <template v-else>
            <el-row :gutter="18">
              <el-col :xs="24" :md="12">
                <div class="focus-card">
                  <div class="focus-card__label">常用工作流程</div>
                  <div class="focus-card__title">实验创建 → 报名审核 → 支付确认 → 评价管理</div>
                  <div class="focus-card__text">当前界面已优先保留研究者最常用、最需要的后台功能，帮助你快速推进自己的实验任务。</div>
                </div>
              </el-col>
              <el-col :xs="24" :md="12">
                <div class="focus-card focus-card--secondary">
                  <div class="focus-card__label">管理建议</div>
                  <div class="focus-card__title">优先关注自己的实验与报名记录</div>
                  <div class="focus-card__text">如果需要处理平台级治理内容或系统设置，请切换管理员账号操作。</div>
                </div>
              </el-col>
            </el-row>
          </template>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="8">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div>
              <strong>{{ authStore.isAdmin ? '治理入口' : '常用入口' }}</strong>
              <div class="page-subtitle">
                {{
                  authStore.isAdmin
                    ? '从这里快速进入平台治理与全局管理入口。'
                    : '从这里快速进入研究者最常使用的功能模块。'
                }}
              </div>
            </div>
          </template>
          <el-timeline v-if="authStore.isAdmin">
            <el-timeline-item timestamp="平台概况" type="primary">
              查看平台用户、实验、报名与评价等核心统计概况
            </el-timeline-item>
            <el-timeline-item timestamp="申诉管理" type="danger">
              统一处理争议、异常申诉与后续治理流程
            </el-timeline-item>
            <el-timeline-item timestamp="系统配置" type="warning">
              维护实验类型、地点与标签等系统级配置项
            </el-timeline-item>
          </el-timeline>
          <el-timeline v-else>
            <el-timeline-item timestamp="实验管理" type="primary">
              统一管理实验创建、编辑、发布与详情查看
            </el-timeline-item>
            <el-timeline-item timestamp="报名审核" type="success">
              集中处理被试报名、签到与完成确认
            </el-timeline-item>
            <el-timeline-item timestamp="支付与评价" type="warning">
              持续跟进支付状态、评价提交与后续反馈处理
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div>
          <strong>{{ authStore.isAdmin ? '管理说明' : '功能说明' }}</strong>
          <div class="page-subtitle">
            {{
              authStore.isAdmin
                ? '管理员可在此集中处理平台治理、配置维护与争议流程。'
                : '研究者可在此完成实验管理、报名处理、支付跟进与评价相关操作。'
            }}
          </div>
        </div>
      </template>
      <el-descriptions :column="1" border v-if="authStore.isAdmin">
        <el-descriptions-item label="平台治理">查看平台概况、处理申诉与关注关键运行状态</el-descriptions-item>
        <el-descriptions-item label="系统维护">维护实验类型、地点、标签等系统配置内容</el-descriptions-item>
        <el-descriptions-item label="全局协同">在出现争议、例外流程或平台级问题时提供统一处理入口</el-descriptions-item>
        <el-descriptions-item label="业务追踪">必要时进入实验管理、支付确认等页面查看或辅助处理平台事务</el-descriptions-item>
      </el-descriptions>
      <el-descriptions :column="1" border v-else>
        <el-descriptions-item label="账号资料">查看与维护个人基础资料、角色与信誉信息</el-descriptions-item>
        <el-descriptions-item label="实验管理">创建实验、查看详情、发布实验并维护实验信息</el-descriptions-item>
        <el-descriptions-item label="报名处理">审核报名、完成签到与实验结束后的流程确认</el-descriptions-item>
        <el-descriptions-item label="支付与评价">跟踪支付状态，并在合适阶段完成评价与后续反馈处理</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getAppeals } from '../api/appeals';
import { getPlatformSummary } from '../api/dashboard';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const summaryLoading = ref(false);
const appealsLoading = ref(false);
const summary = ref(null);
const pendingAppeals = ref(0);

async function loadSummary() {
  if (!authStore.isAdmin) {
    summary.value = null;
    return;
  }

  summaryLoading.value = true;
  try {
    summary.value = await getPlatformSummary();
  } catch (_error) {
    summary.value = null;
  } finally {
    summaryLoading.value = false;
  }
}

async function loadPendingAppeals() {
  if (!authStore.isAdmin) {
    pendingAppeals.value = 0;
    return;
  }

  appealsLoading.value = true;
  try {
    const pageData = await getAppeals({ status: 'PENDING', page: 0, size: 1 });
    pendingAppeals.value = pageData.totalElements || 0;
  } catch (_error) {
    pendingAppeals.value = 0;
  } finally {
    appealsLoading.value = false;
  }
}

async function refreshAdminOverview() {
  await Promise.all([loadSummary(), loadPendingAppeals()]);
}

onMounted(() => {
  if (authStore.isAdmin) {
    refreshAdminOverview();
  }
});
</script>

<style scoped>
.focus-card {
  border-radius: 22px;
  padding: 22px;
  min-height: 170px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(15, 23, 42, 0.04));
  border: 1px solid rgba(59, 130, 246, 0.12);
}

.focus-card--secondary {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.1), rgba(15, 118, 110, 0.08));
}

.focus-card__label {
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
}

.focus-card__title {
  margin-top: 14px;
  color: #0f172a;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.5;
}

.focus-card__text {
  margin-top: 12px;
  color: #475569;
  line-height: 1.8;
}

.admin-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

@media (max-width: 960px) {
  .admin-grid {
    grid-template-columns: 1fr;
  }
}
</style>
