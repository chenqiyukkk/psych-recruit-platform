<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">申诉管理</h1>
        <p class="page-subtitle">
          面向管理员集中查看用户申诉、按状态筛选，并执行通过 / 拒绝审核。该页面延续现有管理台的卡片与操作反馈风格。
        </p>
      </div>
      <div class="inline-actions">
        <el-button @click="loadAppeals">刷新</el-button>
      </div>
    </div>

    <template v-if="isAdmin">
      <el-card class="panel-card" shadow="never">
        <el-form inline>
          <el-form-item label="申诉状态">
            <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 220px">
              <el-option
                v-for="item in APPEAL_STATUS_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <div class="inline-actions">
              <el-button type="primary" @click="search">查询</el-button>
              <el-button @click="reset">重置</el-button>
            </div>
          </el-form-item>
        </el-form>
        <div class="page-subtitle">
          列表接口为 `/api/appeals`，当前按后端实际分页结构读取 `data.content / totalElements`。
        </div>
      </el-card>

      <el-row :gutter="18">
        <el-col :xs="24" :xl="17">
          <el-card class="panel-card" shadow="never">
            <el-table :data="records" v-loading="loading">
              <el-table-column prop="id" label="申诉 ID" width="100" />
              <el-table-column prop="appellantId" label="申诉人 ID" width="120" />
              <el-table-column label="申诉类型" width="160">
                <template #default="{ row }">
                  {{ formatAppealType(row.appealType) }}
                </template>
              </el-table-column>
              <el-table-column prop="targetId" label="关联记录 ID" width="140" />
              <el-table-column label="申诉理由" min-width="240">
                <template #default="{ row }">
                  <el-tooltip :content="row.reason || '--'" placement="top-start">
                    <div class="reason-preview">{{ row.reason || '--' }}</div>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column label="证据信息" min-width="220">
                <template #default="{ row }">
                  <div class="evidence-preview">{{ formatEvidence(row.evidenceUrls) }}</div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="130">
                <template #default="{ row }">
                  <AppealStatusTag :status="row.status" />
                </template>
              </el-table-column>
              <el-table-column prop="reviewerId" label="审核人 ID" width="120">
                <template #default="{ row }">
                  {{ row.reviewerId || '--' }}
                </template>
              </el-table-column>
              <el-table-column label="审核时间" width="180">
                <template #default="{ row }">{{ formatDateTime(row.reviewedAt) }}</template>
              </el-table-column>
              <el-table-column label="创建时间" width="180">
                <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="160" fixed="right">
                <template #default="{ row }">
                  <div class="inline-actions">
                    <el-button link type="primary" @click="openDialog(row)">查看/审核</el-button>
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
        </el-col>

        <el-col :xs="24" :xl="7">
          <el-card class="panel-card" shadow="never">
            <template #header>
              <div>
                <strong>处理说明</strong>
                <div class="page-subtitle">当前后端审核通过后会更新状态并发送通知，但不会自动撤销底层业务记录。</div>
              </div>
            </template>
            <el-timeline>
              <el-timeline-item timestamp="PENDING" type="warning">
                待处理申诉，可进入审核弹窗执行通过或拒绝
              </el-timeline-item>
              <el-timeline-item timestamp="APPROVED" type="success">
                已通过，用户会收到结果通知
              </el-timeline-item>
              <el-timeline-item timestamp="REJECTED" type="danger">
                已拒绝，可查看审核意见
              </el-timeline-item>
              <el-timeline-item timestamp="UNDER_REVIEW" type="info">
                当前后端服务未实际写入，但页面已兼容展示
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </el-col>
      </el-row>

      <el-dialog v-model="dialogVisible" title="申诉审核" width="640px">
        <el-descriptions :column="2" border v-if="currentAppeal">
          <el-descriptions-item label="申诉 ID">{{ currentAppeal.id }}</el-descriptions-item>
          <el-descriptions-item label="申诉人 ID">{{ currentAppeal.appellantId }}</el-descriptions-item>
          <el-descriptions-item label="申诉类型">{{ formatAppealType(currentAppeal.appealType) }}</el-descriptions-item>
          <el-descriptions-item label="关联记录 ID">{{ currentAppeal.targetId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <AppealStatusTag :status="currentAppeal.status" />
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentAppeal.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="申诉理由" :span="2">
            {{ currentAppeal.reason || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="证据信息" :span="2">
            {{ formatEvidence(currentAppeal.evidenceUrls) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          style="margin-top: 18px"
          type="warning"
          :closable="false"
          show-icon
          title="审核通过后，当前后端只会更新申诉状态并发送通知，不会自动回滚底层业务。"
        />

        <el-form
          ref="formRef"
          :model="reviewForm"
          :rules="formRules"
          label-position="top"
          style="margin-top: 18px"
        >
          <el-form-item label="审核结论" prop="decision">
            <el-radio-group v-model="reviewForm.decision" :disabled="isReviewed(currentAppeal)">
              <el-radio label="APPROVED">通过</el-radio>
              <el-radio label="REJECTED">拒绝</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审核意见" prop="reviewComment">
            <el-input
              v-model="reviewForm.reviewComment"
              type="textarea"
              :rows="4"
              placeholder="可填写补充说明或拒绝理由"
              :disabled="isReviewed(currentAppeal)"
            />
          </el-form-item>
        </el-form>

        <template #footer>
          <div class="inline-actions" style="justify-content: flex-end; width: 100%">
            <el-button @click="dialogVisible = false">关闭</el-button>
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="isReviewed(currentAppeal)"
              @click="submitReview"
            >
              提交审核
            </el-button>
          </div>
        </template>
      </el-dialog>
    </template>

    <el-card v-else class="panel-card" shadow="never">
      <el-empty description="当前账号仅可查看研究者工作流。申诉管理页仅对管理员开放。" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getAppeals, reviewAppeal } from '../../api/appeals';
import AppealStatusTag from '../../components/AppealStatusTag.vue';
import { APPEAL_STATUS_OPTIONS, appealTypeMap } from '../../constants/appeals';
import { ROLE_ADMIN } from '../../constants/roles';
import { useAuthStore } from '../../stores/auth';
import { formatDateTime } from '../../utils/format';

const authStore = useAuthStore();
const isAdmin = computed(() => authStore.profile?.role === ROLE_ADMIN);

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const formRef = ref();
const records = ref([]);
const total = ref(0);
const currentAppeal = ref(null);

const filters = reactive({
  status: '',
});

const pagination = reactive({
  page: 0,
  size: 10,
});

const reviewForm = reactive({
  decision: 'APPROVED',
  reviewComment: '',
});

const formRules = {
  decision: [{ required: true, message: '请选择审核结论', trigger: 'change' }],
};

function isReviewed(appeal) {
  return appeal && ['APPROVED', 'REJECTED'].includes(appeal.status);
}

function formatAppealType(value) {
  return appealTypeMap[value]?.label || value || '--';
}

function formatEvidence(value) {
  if (!value) {
    return '--';
  }

  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.join('；') : value;
  } catch (_error) {
    return value;
  }
}

function buildParams() {
  return {
    status: filters.status || undefined,
    page: pagination.page,
    size: pagination.size,
  };
}

async function loadAppeals() {
  if (!isAdmin.value) {
    records.value = [];
    total.value = 0;
    return;
  }

  loading.value = true;
  try {
    const pageData = await getAppeals(buildParams());
    records.value = pageData.content || [];
    total.value = pageData.totalElements || 0;
  } finally {
    loading.value = false;
  }
}

function search() {
  pagination.page = 0;
  loadAppeals();
}

function reset() {
  filters.status = '';
  search();
}

function handlePageChange(page) {
  pagination.page = page - 1;
  loadAppeals();
}

function openDialog(row) {
  currentAppeal.value = row;
  reviewForm.decision = row.status === 'REJECTED' ? 'REJECTED' : 'APPROVED';
  reviewForm.reviewComment = row.reviewComment || '';
  dialogVisible.value = true;
}

async function submitReview() {
  if (!currentAppeal.value) {
    return;
  }
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    const updated = await reviewAppeal(currentAppeal.value.id, {
      decision: reviewForm.decision,
      reviewComment: reviewForm.reviewComment,
    });
    ElMessage.success('申诉审核已提交');
    currentAppeal.value = updated;
    dialogVisible.value = false;
    await loadAppeals();
  } finally {
    submitting.value = false;
  }
}

loadAppeals();
</script>

<style scoped>
.reason-preview,
.evidence-preview {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-height: 1.7;
  color: #334155;
}
</style>
