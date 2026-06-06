<template>
  <div class="page-shell" v-loading="loading">
    <div class="page-header" v-if="detail">
      <div>
        <h1 class="page-title">{{ detail.title }}</h1>
        <p class="page-subtitle">{{ detail.description || '暂无实验描述' }}</p>
      </div>
      <div class="inline-actions">
        <ExperimentStatusTag :status="detail.status" />
        <el-button @click="router.push(`/experiments/${detail.id}/edit`)" :disabled="detail.status !== 'DRAFT'">编辑</el-button>
        <el-button type="primary" @click="router.push('/registrations/review?experimentId=' + detail.id)">查看报名审核</el-button>
      </div>
    </div>

    <el-row v-if="detail" :gutter="18">
      <el-col :xs="24" :xl="15">
        <el-card class="panel-card" shadow="never">
          <template #header><strong>基础信息</strong></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="地点">{{ detail.location || '--' }}</el-descriptions-item>
            <el-descriptions-item label="伦理审批编号">{{ detail.ethicsApprovalNo || '--' }}</el-descriptions-item>
            <el-descriptions-item label="风险等级">{{ riskText(detail.riskLevel) }}</el-descriptions-item>
            <el-descriptions-item label="支付方式">{{ paymentText(detail.paymentMethod) }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ formatDateTime(detail.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ formatDateTime(detail.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="报酬金额">{{ formatCurrency(detail.paymentAmount) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="报酬说明" :span="2">{{ detail.paymentDescription || '--' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header><strong>筛选与互斥规则</strong></template>
          <el-row :gutter="16">
            <el-col :xs="24" :lg="12">
              <div class="section-label">筛选条件 JSON</div>
              <pre class="code-block">{{ prettyJson(detail.screeningCriteria) }}</pre>
            </el-col>
            <el-col :xs="24" :lg="12">
              <div class="section-label">互斥标签 JSON</div>
              <pre class="code-block">{{ prettyJson(detail.excludeTags) }}</pre>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="9">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div>
              <strong>实验统计</strong>
              <div class="page-subtitle">后端接口：GET /api/statistics/experiments/{id}</div>
            </div>
          </template>
          <el-skeleton :loading="statsLoading" animated :rows="5">
            <div v-if="stats" class="grid-cards">
              <el-statistic title="报名总数" :value="stats.registrationCount" />
              <el-statistic title="通过" :value="stats.approvedCount" />
              <el-statistic title="拒绝" :value="stats.rejectedCount" />
              <el-statistic title="签到" :value="stats.signedInCount" />
              <el-statistic title="完成" :value="stats.completedCount" />
            </div>
            <el-empty v-else description="暂无统计数据" />
          </el-skeleton>
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header><strong>实验标签</strong></template>
          <div class="tag-list">
            <el-tag v-for="tag in detail.tags || []" :key="tag.id || tag.tagName" effect="plain" round>
              {{ tag.tagName }} · {{ tag.coolingDays }} 天
            </el-tag>
            <span v-if="!(detail.tags || []).length">暂无标签</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getExperimentById, getExperimentStatistics } from '../../api/experiments';
import ExperimentStatusTag from '../../components/ExperimentStatusTag.vue';
import { PAYMENT_METHOD_OPTIONS, RISK_LEVEL_OPTIONS } from '../../constants/experiments';
import { formatCurrency, formatDateTime, prettyJson } from '../../utils/format';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const statsLoading = ref(false);
const detail = ref(null);
const stats = ref(null);

function riskText(value) {
  return RISK_LEVEL_OPTIONS.find((item) => item.value === value)?.label || value || '--';
}

function paymentText(value) {
  return PAYMENT_METHOD_OPTIONS.find((item) => item.value === value)?.label || value || '--';
}

async function loadDetail() {
  loading.value = true;
  try {
    detail.value = await getExperimentById(route.params.id);
  } finally {
    loading.value = false;
  }
}

async function loadStats() {
  statsLoading.value = true;
  try {
    stats.value = await getExperimentStatistics(route.params.id);
  } catch (_error) {
    stats.value = null;
  } finally {
    statsLoading.value = false;
  }
}

Promise.all([loadDetail(), loadStats()]);
</script>

<style scoped>
.section-label {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}
</style>
