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
            <el-descriptions-item label="参与人数上限">{{ detail.participantLimit || '--' }} 人</el-descriptions-item>
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
              <div class="section-label">招募筛选条件</div>
              <template v-if="screeningSummary.type === 'structured'">
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="性别要求">{{ screeningSummary.gender }}</el-descriptions-item>
                  <el-descriptions-item label="年龄范围">{{ screeningSummary.ageRange }}</el-descriptions-item>
                  <el-descriptions-item label="专业类别">{{ screeningSummary.majorCategories }}</el-descriptions-item>
                  <el-descriptions-item label="利手要求">{{ screeningSummary.handedness }}</el-descriptions-item>
                  <el-descriptions-item label="补充说明">{{ screeningSummary.notes }}</el-descriptions-item>
                </el-descriptions>
              </template>
              <el-empty v-else-if="screeningSummary.type === 'empty'" description="未设置招募筛选条件" />
              <template v-else>
                <div class="legacy-tip">该实验仍保存为旧格式规则，下面显示原始内容：</div>
                <pre class="code-block">{{ prettyJson(detail.screeningCriteria) }}</pre>
              </template>
            </el-col>
            <el-col :xs="24" :lg="12">
              <div class="section-label">互斥规则</div>
              <template v-if="exclusionSummary.type === 'structured'">
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="互斥标签">
                    <div class="tag-list">
                      <el-tag v-for="tag in exclusionSummary.tags" :key="tag" effect="plain" round>{{ tag }}</el-tag>
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="规则说明">{{ exclusionSummary.notes }}</el-descriptions-item>
                </el-descriptions>
              </template>
              <el-empty v-else-if="exclusionSummary.type === 'empty'" description="未设置互斥规则" />
              <template v-else>
                <div class="legacy-tip">该实验仍保存为旧格式规则，下面显示原始内容：</div>
                <pre class="code-block">{{ prettyJson(detail.excludeTags) }}</pre>
              </template>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="9">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div>
              <strong>实验统计</strong>
              <div class="page-subtitle">查看当前实验的报名、签到与完成情况。</div>
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
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';
import { getExperimentById, getExperimentStatistics } from '../../api/experiments';
import ExperimentStatusTag from '../../components/ExperimentStatusTag.vue';
import {
  HANDEDNESS_OPTIONS,
  MAJOR_CATEGORY_OPTIONS,
  PAYMENT_METHOD_OPTIONS,
  RISK_LEVEL_OPTIONS,
  SCREENING_GENDER_OPTIONS,
} from '../../constants/experiments';
import { formatCurrency, formatDateTime, prettyJson } from '../../utils/format';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const statsLoading = ref(false);
const detail = ref(null);
const stats = ref(null);

const genderMap = optionMap(SCREENING_GENDER_OPTIONS);
const handednessMap = optionMap(HANDEDNESS_OPTIONS);
const majorMap = optionMap(MAJOR_CATEGORY_OPTIONS);

const screeningSummary = computed(() => summarizeScreening(detail.value?.screeningCriteria));
const exclusionSummary = computed(() => summarizeExclusion(detail.value?.excludeTags));

function optionMap(options) {
  return Object.fromEntries(options.map((item) => [item.value, item.label]));
}

function parseJsonValue(value) {
  if (!value || !String(value).trim()) {
    return null;
  }
  try {
    return JSON.parse(value);
  } catch (_error) {
    return null;
  }
}

function formatAgeRange(value) {
  if (!Array.isArray(value) || !value.length) {
    return '--';
  }
  const [minAge, maxAge] = value;
  if (minAge === null && maxAge === null) {
    return '--';
  }
  if (minAge !== null && minAge !== undefined && maxAge !== null && maxAge !== undefined) {
    return `${minAge} - ${maxAge} 岁`;
  }
  if (minAge !== null && minAge !== undefined) {
    return `${minAge} 岁及以上`;
  }
  if (maxAge !== null && maxAge !== undefined) {
    return `${maxAge} 岁及以下`;
  }
  return '--';
}

function summarizeScreening(value) {
  const parsed = parseJsonValue(value);
  if (!parsed) {
    return { type: 'empty' };
  }
  if (Array.isArray(parsed) || typeof parsed !== 'object') {
    return { type: 'legacy' };
  }

  const include = parsed.include && typeof parsed.include === 'object' ? parsed.include : parsed;
  return {
    type: 'structured',
    gender: genderMap[include.gender] || '不限',
    ageRange: formatAgeRange(Array.isArray(include.age_range) ? include.age_range : include.ageRange),
    majorCategories: Array.isArray(include.major_categories)
      ? include.major_categories.map((item) => majorMap[item] || item).join('、') || '--'
      : Array.isArray(include.majorCategories)
        ? include.majorCategories.map((item) => majorMap[item] || item).join('、') || '--'
        : '--',
    handedness: handednessMap[include.handedness] || '不限',
    notes: include.notes || parsed.notes || '--',
  };
}

function summarizeExclusion(value) {
  const parsed = parseJsonValue(value);
  if (!parsed) {
    return { type: 'empty' };
  }

  if (Array.isArray(parsed)) {
    const tags = parsed.filter((item) => typeof item === 'string' && item.trim()).map((item) => item.trim());
    return {
      type: tags.length ? 'structured' : 'empty',
      tags,
      notes: '--',
    };
  }

  if (typeof parsed === 'object') {
    const tagsSource = Array.isArray(parsed.tags)
      ? parsed.tags
      : Array.isArray(parsed.excludeTags)
        ? parsed.excludeTags
        : [];
    const tags = tagsSource.filter((item) => typeof item === 'string' && item.trim()).map((item) => item.trim());
    return {
      type: tags.length || parsed.notes || parsed.description ? 'structured' : 'empty',
      tags,
      notes: parsed.notes || parsed.description || '--',
    };
  }

  return { type: 'legacy' };
}

function riskText(value) {
  return RISK_LEVEL_OPTIONS.find((item) => item.value === value)?.label || value || '--';
}

function paymentText(value) {
  return PAYMENT_METHOD_OPTIONS.find((item) => item.value === value)?.label || value || '--';
}

async function loadDetail() {
  loading.value = true;
  try {
    const result = await getExperimentById(route.params.id);
    if (authStore.isResearcher && result.organizerId !== authStore.profile?.id) {
      ElMessage.warning('研究者仅可查看自己创建的实验');
      await router.replace('/experiments');
      return;
    }
    detail.value = result;
  } catch (error) {
    if (error?.response?.status === 403) {
      ElMessage.warning('研究者仅可查看自己创建的实验');
      await router.replace('/experiments');
      return;
    }
    throw error;
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

.legacy-tip {
  margin-bottom: 10px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}
</style>
