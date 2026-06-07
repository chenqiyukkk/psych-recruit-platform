<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">评价管理</h1>
        <p class="page-subtitle">
          研究者可在实验完成后对被试发起评价，并查看自己发出的评价与收到的评价。
        </p>
      </div>
      <div class="inline-actions">
        <el-button @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="待评价报名" name="pending-review">
        <el-card class="panel-card" shadow="never">
          <el-form inline>
            <el-form-item label="选择实验">
              <el-select
                v-model="selectedExperimentId"
                placeholder="请选择要查看评价状态的实验"
                filterable
                style="width: 360px"
                @change="handleExperimentChange"
              >
                <el-option
                  v-for="item in experiments"
                  :key="item.id"
                  :label="item.title"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="page-subtitle">仅展示已完成研究流程的报名记录，研究者可在这里继续对被试发起评价。</div>
        </el-card>

        <template v-if="isResearcher">
          <el-card class="panel-card" shadow="never">
            <el-table :data="eligibleRows" v-loading="loading">
              <el-table-column prop="id" label="报名 ID" width="110" />
              <el-table-column prop="userId" label="用户 ID" width="110" />
              <el-table-column label="报名状态" width="130">
                <template #default="{ row }">
                  <RegistrationStatusTag :status="row.status" />
                </template>
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
              <el-table-column label="评价状态" width="140">
                <template #default="{ row }">
                  <el-tag :type="hasReviewed(row.id) ? 'success' : 'warning'" round>
                    {{ hasReviewed(row.id) ? '已评价' : '待评价' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="220" fixed="right">
                <template #default="{ row }">
                  <div class="inline-actions">
                    <el-button
                      link
                      type="primary"
                      :disabled="hasReviewed(row.id)"
                      @click="openReviewDialog(row)"
                    >
                      去评价
                    </el-button>
                    <el-button link @click="switchToMyReviews(row.id)">查看已评价</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <el-empty
              v-if="!loading && selectedExperimentId && !eligibleRows.length"
              description="当前实验暂无可评价的报名记录"
            />
            <el-empty v-if="!loading && !selectedExperimentId" description="请先选择实验" />
          </el-card>
        </template>

        <el-card v-else class="panel-card" shadow="never">
          <el-empty description="当前账号可查看评价结果，研究者账号可在满足条件的实验流程后继续发起评价。" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="我发出的评价" name="my-reviews">
        <el-card class="panel-card" shadow="never">
          <el-table :data="myReviews" v-loading="myReviewsLoading">
            <el-table-column prop="id" label="评价 ID" width="110" />
            <el-table-column prop="registrationId" label="报名 ID" width="110" />
            <el-table-column prop="reviewedId" label="被评价人 ID" width="130" />
            <el-table-column label="评价方向" width="160">
              <template #default="{ row }">{{ formatReviewType(row.reviewType) }}</template>
            </el-table-column>
            <el-table-column prop="rating" label="总评分" width="90" />
            <el-table-column label="分项评分" min-width="220">
              <template #default="{ row }">
                <div class="score-preview">
                  沟通 {{ row.communicationScore ?? '--' }} / 专业 {{ row.professionalismScore ?? '--' }} / 守时 {{ row.punctualityScore ?? '--' }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="匿名" width="100">
              <template #default="{ row }">
                <el-tag :type="row.isAnonymous ? 'info' : 'success'" round>
                  {{ row.isAnonymous ? '匿名' : '实名' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="评价内容" min-width="220">
              <template #default="{ row }">
                <el-tooltip :content="row.comment || '--'" placement="top-start">
                  <div class="comment-preview">{{ row.comment || '--' }}</div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!myReviewsLoading && !myReviews.length" description="当前账号还没有发出任何评价" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="我收到的评价" name="received-reviews">
        <el-card class="panel-card" shadow="never">
          <el-table :data="receivedReviews" v-loading="receivedReviewsLoading">
            <el-table-column prop="id" label="评价 ID" width="110" />
            <el-table-column prop="registrationId" label="报名 ID" width="110" />
            <el-table-column prop="reviewerId" label="评价人 ID" width="120" />
            <el-table-column label="评价方向" width="160">
              <template #default="{ row }">{{ formatReviewType(row.reviewType) }}</template>
            </el-table-column>
            <el-table-column prop="rating" label="总评分" width="90" />
            <el-table-column label="评价内容" min-width="240">
              <template #default="{ row }">
                <el-tooltip :content="row.comment || '--'" placement="top-start">
                  <div class="comment-preview">{{ row.comment || '--' }}</div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="匿名" width="100">
              <template #default="{ row }">
                <el-tag :type="row.isAnonymous ? 'info' : 'success'" round>
                  {{ row.isAnonymous ? '匿名' : '实名' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!receivedReviewsLoading && !receivedReviews.length" description="当前账号还没有收到任何评价" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" title="评价被试" width="640px">
      <el-descriptions :column="2" border v-if="selectedRow">
        <el-descriptions-item label="报名 ID">{{ selectedRow.id }}</el-descriptions-item>
        <el-descriptions-item label="被试用户 ID">{{ selectedRow.userId }}</el-descriptions-item>
        <el-descriptions-item label="报名状态">
          <RegistrationStatusTag :status="selectedRow.status" />
        </el-descriptions-item>
        <el-descriptions-item label="签到时间">{{ formatDateTime(selectedRow.signInTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-form ref="formRef" :model="reviewForm" :rules="formRules" label-position="top" style="margin-top: 18px">
        <el-form-item label="总体评分" prop="rating">
          <el-rate v-model="reviewForm.rating" :max="5" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :xs="24" :md="8">
            <el-form-item label="沟通评分" prop="communicationScore">
              <el-rate v-model="reviewForm.communicationScore" :max="5" allow-half="false" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="专业评分" prop="professionalismScore">
              <el-rate v-model="reviewForm.professionalismScore" :max="5" allow-half="false" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="守时评分" prop="punctualityScore">
              <el-rate v-model="reviewForm.punctualityScore" :max="5" allow-half="false" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="评价内容" prop="comment">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="4" placeholder="请输入对被试的评价描述" />
        </el-form-item>
        <el-form-item label="匿名评价">
          <el-switch v-model="reviewForm.isAnonymous" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="inline-actions" style="justify-content: flex-end; width: 100%">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitReview">提交评价</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getExperiments } from '../../api/experiments';
import { getExperimentRegistrations } from '../../api/registrations';
import { createReview, getMyReviews, getReceivedReviews } from '../../api/reviews';
import RegistrationStatusTag from '../../components/RegistrationStatusTag.vue';
import { ROLE_RESEARCHER } from '../../constants/roles';
import { reviewTypeMap } from '../../constants/reviews';
import { useAuthStore } from '../../stores/auth';
import { formatDateTime } from '../../utils/format';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const isResearcher = computed(() => authStore.profile?.role === ROLE_RESEARCHER);

const activeTab = ref('pending-review');
const loading = ref(false);
const myReviewsLoading = ref(false);
const receivedReviewsLoading = ref(false);
const dialogVisible = ref(false);
const submitting = ref(false);
const formRef = ref();
const experiments = ref([]);
const registrationRows = ref([]);
const myReviews = ref([]);
const receivedReviews = ref([]);
const selectedRow = ref(null);
const selectedExperimentId = ref(route.query.experimentId ? Number(route.query.experimentId) : null);

const reviewForm = reactive({
  rating: 0,
  communicationScore: 0,
  professionalismScore: 0,
  punctualityScore: 0,
  comment: '',
  isAnonymous: false,
});

const formRules = {
  rating: [{ required: true, message: '请给出总体评分', trigger: 'change' }],
};

const reviewedRegistrationIds = computed(() => {
  const ids = new Set();
  myReviews.value.forEach((item) => {
    if (item.reviewType === 'RESEARCHER_TO_SUBJECT') {
      ids.add(item.registrationId);
    }
  });
  return ids;
});

const eligibleRows = computed(() =>
  registrationRows.value.filter(
    (row) => row.status === 'APPROVED' && row.signInTime && row.isCompleted === true,
  ),
);

function formatReviewType(value) {
  return reviewTypeMap[value]?.label || value || '--';
}

function hasReviewed(registrationId) {
  return reviewedRegistrationIds.value.has(registrationId);
}

async function loadExperiments() {
  const data = await getExperiments({ page: 0, size: 100 });
  experiments.value = data.content || [];
}

async function loadRegistrations() {
  if (!selectedExperimentId.value) {
    registrationRows.value = [];
    return;
  }

  loading.value = true;
  try {
    registrationRows.value = await getExperimentRegistrations(selectedExperimentId.value);
  } finally {
    loading.value = false;
  }
}

async function loadMyReviews() {
  myReviewsLoading.value = true;
  try {
    myReviews.value = await getMyReviews();
  } finally {
    myReviewsLoading.value = false;
  }
}

async function loadReceivedReviews() {
  receivedReviewsLoading.value = true;
  try {
    receivedReviews.value = await getReceivedReviews();
  } finally {
    receivedReviewsLoading.value = false;
  }
}

async function handleExperimentChange() {
  if (selectedExperimentId.value) {
    router.replace({
      path: '/reviews',
      query: { experimentId: String(selectedExperimentId.value) },
    });
  }
  await loadRegistrations();
}

function resetReviewForm() {
  reviewForm.rating = 0;
  reviewForm.communicationScore = 0;
  reviewForm.professionalismScore = 0;
  reviewForm.punctualityScore = 0;
  reviewForm.comment = '';
  reviewForm.isAnonymous = false;
}

function openReviewDialog(row) {
  selectedRow.value = row;
  resetReviewForm();
  dialogVisible.value = true;
}

function switchToMyReviews(registrationId) {
  activeTab.value = 'my-reviews';
  ElMessage.info(`报名 ${registrationId} 的评价记录已收录，可在“我发出的评价”中查看。`);
}

async function submitReview() {
  if (!selectedRow.value) {
    return;
  }
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    await createReview(selectedRow.value.id, {
      reviewType: 'RESEARCHER_TO_SUBJECT',
      rating: reviewForm.rating,
      communicationScore: reviewForm.communicationScore || null,
      professionalismScore: reviewForm.professionalismScore || null,
      punctualityScore: reviewForm.punctualityScore || null,
      comment: reviewForm.comment || null,
      isAnonymous: reviewForm.isAnonymous,
    });
    ElMessage.success('评价提交成功');
    dialogVisible.value = false;
    await Promise.all([loadMyReviews(), loadRegistrations()]);
    activeTab.value = 'my-reviews';
  } finally {
    submitting.value = false;
  }
}

async function refreshAll() {
  await Promise.all([loadMyReviews(), loadReceivedReviews(), loadRegistrations()]);
}

Promise.all([loadExperiments(), loadMyReviews(), loadReceivedReviews(), loadRegistrations()]);
</script>

<style scoped>
.score-preview,
.comment-preview {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-height: 1.7;
  color: #334155;
}
</style>
