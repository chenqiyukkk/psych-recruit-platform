<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">报名审核</h1>
        <p class="page-subtitle">研究者或管理员可按实验查看报名记录，并集中完成审核、签到与完成确认。</p>
      </div>
      <div class="inline-actions">
        <el-button @click="loadRegistrations" :disabled="!selectedExperimentId">刷新</el-button>
      </div>
    </div>

    <el-card class="panel-card" shadow="never">
      <el-form inline>
        <el-form-item label="选择实验">
          <el-select v-model="selectedExperimentId" placeholder="请选择需要审核的实验" filterable style="width: 360px" @change="loadRegistrations">
            <el-option v-for="item in experiments" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="page-subtitle">选择实验后即可查看相关报名记录，并执行审核、签到或完成确认。</div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <el-table :data="records" v-loading="loading">
        <el-table-column prop="id" label="报名 ID" width="110" />
        <el-table-column prop="userId" label="用户 ID" width="110" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <RegistrationStatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="报名时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.appliedAt) }}</template>
        </el-table-column>
        <el-table-column label="审核时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.reviewedAt) }}</template>
        </el-table-column>
        <el-table-column label="签到时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.signInTime) }}</template>
        </el-table-column>
        <el-table-column label="完成状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.isCompleted ? 'success' : 'info'" round>
              {{ row.isCompleted ? '已完成' : '未完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="320" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button link type="success" :disabled="row.status !== 'PENDING'" @click="approve(row)">通过</el-button>
              <el-button link type="danger" :disabled="row.status !== 'PENDING'" @click="reject(row)">拒绝</el-button>
              <el-button link type="primary" :disabled="row.status !== 'APPROVED' || !!row.signInTime" @click="signIn(row)">签到</el-button>
              <el-button link :disabled="!row.signInTime || row.isCompleted" @click="complete(row)">完成</el-button>
              <el-button link type="danger" :disabled="row.status !== 'APPROVED' || !!row.signInTime || row.isCompleted" @click="noShow(row)">爽约</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && selectedExperimentId && !records.length" description="当前实验暂无报名记录" />
      <el-empty v-if="!loading && !selectedExperimentId" description="请先选择实验" />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getExperiments } from '../../api/experiments';
import {
  approveRegistration,
  completeRegistration,
  getExperimentRegistrations,
  markNoShow,
  rejectRegistration,
  signInRegistration,
} from '../../api/registrations';
import RegistrationStatusTag from '../../components/RegistrationStatusTag.vue';
import { formatDateTime } from '../../utils/format';

const route = useRoute();
const loading = ref(false);
const experiments = ref([]);
const records = ref([]);
const selectedExperimentId = ref(route.query.experimentId ? Number(route.query.experimentId) : null);

async function loadExperiments() {
  const data = await getExperiments({ page: 0, size: 100 });
  experiments.value = data.content || [];
}

async function loadRegistrations() {
  if (!selectedExperimentId.value) {
    records.value = [];
    return;
  }
  loading.value = true;
  try {
    records.value = await getExperimentRegistrations(selectedExperimentId.value);
  } finally {
    loading.value = false;
  }
}

async function approve(row) {
  await approveRegistration(row.id);
  ElMessage.success(`报名 ${row.id} 已通过`);
  await loadRegistrations();
}

async function reject(row) {
  await rejectRegistration(row.id);
  ElMessage.success(`报名 ${row.id} 已拒绝`);
  await loadRegistrations();
}

async function signIn(row) {
  await signInRegistration(row.id);
  ElMessage.success(`报名 ${row.id} 已签到`);
  await loadRegistrations();
}

async function complete(row) {
  await completeRegistration(row.id);
  ElMessage.success(`报名 ${row.id} 已标记完成，被试信誉分 +2`);
  await loadRegistrations();
}

async function noShow(row) {
  await ElMessageBox.confirm(
    `确认将报名 ${row.id} 标记为爽约吗？被试将被扣除 20 信誉分。`,
    '标记爽约',
    { type: 'warning', confirmButtonText: '确认爽约', cancelButtonText: '取消' },
  );
  await markNoShow(row.id);
  ElMessage.success(`报名 ${row.id} 已标记爽约，被试信誉分 -20`);
  await loadRegistrations();
}

Promise.all([loadExperiments(), loadRegistrations()]);
</script>
