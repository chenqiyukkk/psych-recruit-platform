<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">实验管理</h1>
        <p class="page-subtitle">围绕实验全生命周期进行筛选、查看、发布与状态回收。</p>
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
            <div class="table-title">{{ row.title }}</div>
            <div class="table-meta">{{ row.location || '未设置地点' }}</div>
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
        <el-table-column label="报酬" width="140">
          <template #default="{ row }">{{ formatCurrency(row.paymentAmount) }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
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
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button link type="primary" @click="router.push(`/experiments/${row.id}`)">查看</el-button>
              <el-button link @click="router.push(`/experiments/${row.id}/edit`)" :disabled="row.status !== 'DRAFT'">编辑</el-button>
              <el-button link type="success" @click="publish(row)" :disabled="row.status !== 'DRAFT'">发布</el-button>
              <el-button link type="warning" @click="cancel(row)" :disabled="row.status === 'COMPLETED'">回收</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  cancelExperiment,
  deleteExperiment,
  getExperiments,
  publishExperiment,
} from '../../api/experiments';
import {
  EXPERIMENT_STATUSES,
  PAYMENT_METHOD_OPTIONS,
  RISK_LEVEL_OPTIONS,
} from '../../constants/experiments';
import ExperimentStatusTag from '../../components/ExperimentStatusTag.vue';
import { formatCurrency, formatDateTime } from '../../utils/format';

const router = useRouter();
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
    page: pagination.page,
    size: pagination.size,
  };
}

async function loadData() {
  loading.value = true;
  try {
    const pageData = await getExperiments(buildParams());
    records.value = pageData.content || [];
    total.value = pageData.totalElements || 0;
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
