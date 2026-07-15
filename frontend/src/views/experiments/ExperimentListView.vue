<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">实验管理</h1>
        <p class="page-subtitle">围绕实验全生命周期进行筛选、查看、发布与状态回收。研究者账号仅展示自己创建的实验。</p>
      </div>
      <div class="inline-actions">
        <el-button @click="loadData">刷新</el-button>
        <el-button type="primary" @click="router.push('/experiments/create')">创建实验</el-button>
      </div>
    </div>

    <el-card class="panel-card" shadow="never">
      <el-form :model="filters" inline>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="标题 / 描述" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 160px">
            <el-option v-for="item in EXPERIMENT_STATUSES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="filters.riskLevel" placeholder="全部风险" clearable style="width: 160px">
            <el-option v-for="item in RISK_LEVEL_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="filters.paymentMethod" placeholder="全部方式" clearable style="width: 160px">
            <el-option v-for="item in PAYMENT_METHOD_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="inline-actions">
            <el-button type="primary" @click="search">查询</el-button>
            <el-button @click="reset">重置</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <el-table :data="records" v-loading="loading">
        <el-table-column prop="title" label="实验标题" min-width="220">
          <template #default="{ row }">
            <div class="table-title">
              {{ row.title }}
              <el-tag v-if="row.status === 'DRAFT' && row.reviewComment" size="small" type="danger" style="margin-left: 6px">
                需修改
              </el-tag>
            </div>
            <div class="table-meta">{{ row.location || '未设置地点' }}</div>
            <div
              v-if="row.status === 'DRAFT' && row.reviewComment"
              class="table-meta"
              style="color: #dc2626; margin-top: 2px"
            >
              <el-icon><WarningFilled /></el-icon>
              {{ row.reviewComment }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <ExperimentStatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="风险" width="120">
          <template #default="{ row }">
            {{ riskText(row.riskLevel) }}
          </template>
        </el-table-column>
        <el-table-column label="已报名/上限" width="140">
          <template #default="{ row }">
            <span :style="{ color: row.approvedCount >= row.participantLimit ? '#dc2626' : '#0f172a' }">
              {{ row.approvedCount ?? 0 }} / {{ row.participantLimit || '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="报酬" width="140">
          <template #default="{ row }">{{ formatCurrency(row.paymentAmount) }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="审核状态" width="180" v-if="authStore.isResearcher">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'DRAFT' && row.reviewComment" type="danger" effect="dark">
              已驳回
            </el-tag>
            <span v-else class="table-meta">--</span>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="220">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag v-for="tag in row.tags || []" :key="tag.id || tag.tagName" round effect="plain">
                {{ tag.tagName }} · 冷却 {{ tag.coolingDays }} 天
              </el-tag>
              <span v-if="!(row.tags || []).length" class="table-meta">暂无标签</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button link type="primary" @click="router.push(`/experiments/${row.id}`)">查看</el-button>
              <el-button link @click="router.push(`/experiments/${row.id}/edit`)" :disabled="row.status !== 'DRAFT'">编辑</el-button>
              <!-- 研究者：草稿 → 提交审核 -->
              <el-button v-if="!authStore.isAdmin && row.status === 'DRAFT'" link type="warning" @click="submitReview(row)">提交审核</el-button>
              <!-- 管理员：待审核 → 通过 / 驳回 -->
              <el-button v-if="authStore.isAdmin && row.status === 'PENDING_REVIEW'" link type="success" @click="approve(row)">通过</el-button>
              <el-button v-if="authStore.isAdmin && row.status === 'PENDING_REVIEW'" link type="danger" @click="rejectExp(row)">驳回</el-button>
              <!-- 发布 / 回收（管理员对已发布） -->
              <el-button v-if="authStore.isAdmin && row.status === 'PUBLISHED'" link type="warning" @click="cancel(row)">回收</el-button>
              <!-- 研究者发布已审核的实验 -->
              <el-button v-if="!authStore.isAdmin && row.status === 'PENDING_REVIEW'" link type="success" @click="publish(row)">发布</el-button>
              <!-- 研究者回收已发布 -->
              <el-button v-if="!authStore.isAdmin && row.status === 'PUBLISHED'" link type="warning" @click="cancel(row)">回收</el-button>
              <el-button link type="danger" @click="remove(row)" :disabled="row.status !== 'DRAFT'">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 20px; display: flex; justify-content: flex-end">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          :current-page="pagination.page + 1"
          :page-size="pagination.size"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  approveExperiment,
  cancelExperiment,
  deleteExperiment,
  getExperiments,
  publishExperiment,
  rejectExperiment,
  submitForReview,
} from '../../api/experiments';
import {
  EXPERIMENT_STATUSES,
  PAYMENT_METHOD_OPTIONS,
  RISK_LEVEL_OPTIONS,
} from '../../constants/experiments';
import ExperimentStatusTag from '../../components/ExperimentStatusTag.vue';
import { WarningFilled } from '@element-plus/icons-vue';
import { formatCurrency, formatDateTime } from '../../utils/format';

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const total = ref(0);
const records = ref([]);

const filters = reactive({
  keyword: '',
  status: '',
  riskLevel: '',
  paymentMethod: '',
});

const pagination = reactive({
  page: 0,
  size: 10,
});

function riskText(value) {
  return RISK_LEVEL_OPTIONS.find((item) => item.value === value)?.label || value || '--';
}

function buildParams() {
  return {
    ...filters,
    organizerId: authStore.isResearcher ? authStore.profile?.id : undefined,
    page: pagination.page,
    size: pagination.size,
  };
}

async function loadData() {
  if (authStore.isResearcher && !authStore.profile?.id) {
    records.value = [];
    total.value = 0;
    return;
  }

  loading.value = true;
  try {
    const pageData = await getExperiments(buildParams());
    const content = pageData.content || [];
    const visibleRecords = authStore.isResearcher
      ? content.filter((item) => item.organizerId === authStore.profile?.id)
      : content;
    records.value = visibleRecords;
    total.value = authStore.isResearcher ? visibleRecords.length : pageData.totalElements || 0;
  } finally {
    loading.value = false;
  }
}

function search() {
  pagination.page = 0;
  loadData();
}

function reset() {
  Object.assign(filters, {
    keyword: '',
    status: '',
    riskLevel: '',
    paymentMethod: '',
  });
  search();
}

function handlePageChange(page) {
  pagination.page = page - 1;
  loadData();
}

async function publish(row) {
  await ElMessageBox.confirm(`确认发布实验「${row.title}」吗？`, '发布确认', {
    type: 'warning',
  });
  await publishExperiment(row.id);
  ElMessage.success('实验已发布');
  await loadData();
}

async function submitReview(row) {
  await ElMessageBox.confirm(`确认将实验「${row.title}」提交管理员审核吗？`, '提交审核', {
    type: 'warning',
  });
  await submitForReview(row.id);
  ElMessage.success('已提交审核');
  await loadData();
}

async function approve(row) {
  await ElMessageBox.confirm(`确认通过实验「${row.title}」的审核吗？通过后将自动发布。`, '审批通过', {
    type: 'warning',
  });
  await approveExperiment(row.id);
  ElMessage.success('审核通过，实验已发布');
  await loadData();
}

async function rejectExp(row) {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      `请输入驳回「${row.title}」的原因，研究者将看到此说明：`,
      '驳回实验',
      {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        type: 'warning',
        inputType: 'textarea',
        inputPlaceholder: '例如：伦理审批编号无效，请补充后重新提交',
      },
    );
    if (reason === undefined) return; // 用户取消
    await rejectExperiment(row.id, reason || undefined);
    ElMessage.success('已驳回，实验退回草稿');
    await loadData();
  } catch (_error) {
    // 用户取消弹窗，忽略
  }
}

async function cancel(row) {
  await ElMessageBox.confirm(`确认回收实验「${row.title}」吗？回收后状态会回到草稿。`, '回收确认', {
    type: 'warning',
  });
  await cancelExperiment(row.id);
  ElMessage.success('实验状态已回收为草稿');
  await loadData();
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除草稿实验「${row.title}」吗？此操作不可撤回。`, '删除确认', {
    type: 'warning',
  });
  await deleteExperiment(row.id);
  ElMessage.success('实验已删除');
  await loadData();
}

loadData();
</script>

<style scoped>
.table-title {
  font-weight: 600;
  color: #0f172a;
}

.table-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}
</style>
