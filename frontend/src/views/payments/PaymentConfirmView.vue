<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">支付确认</h1>
        <p class="page-subtitle">
          研究者或管理员可围绕已报名记录查看支付状态、登记付款进度，并为后续评价与申诉流程做准备。
        </p>
      </div>
      <div class="inline-actions">
        <el-button @click="refreshCurrent">刷新</el-button>
      </div>
    </div>

    <el-card class="panel-card" shadow="never">
      <el-form inline>
        <el-form-item label="选择实验">
          <el-select
            v-model="selectedExperimentId"
            placeholder="请选择需要处理支付的实验"
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
      <div class="page-subtitle">选择实验后即可查看相关报名记录，并逐条跟进支付状态。</div>
    </el-card>

    <el-row :gutter="18">
      <el-col :xs="24" :xl="17">
        <el-card class="panel-card" shadow="never">
          <el-table :data="paymentRows" v-loading="loading">
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
            <el-table-column label="支付金额" width="140">
              <template #default="{ row }">
                {{ formatCurrency(row.paymentRecord?.amount ?? selectedExperiment?.paymentAmount) }}
              </template>
            </el-table-column>
            <el-table-column label="支付状态" width="130">
              <template #default="{ row }">
                <PaymentStatusTag :status="row.paymentRecord?.status || 'PENDING'" />
              </template>
            </el-table-column>
            <el-table-column label="付款确认时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.paymentRecord?.payerConfirmedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="260" fixed="right">
              <template #default="{ row }">
                <div class="inline-actions">
                  <el-button link type="primary" @click="inspectRecord(row)">查看支付状态</el-button>
                  <el-button
                    link
                    type="success"
                    :disabled="!canConfirm(row)"
                    @click="openConfirmDialog(row)"
                  >
                    确认已付款
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <el-empty
            v-if="!loading && selectedExperimentId && !paymentRows.length"
            description="当前实验暂无可处理的报名记录"
          />
          <el-empty v-if="!loading && !selectedExperimentId" description="请先选择实验" />
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="7">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div>
              <strong>支付流程说明</strong>
              <div class="page-subtitle">以下状态帮助你快速判断当前记录还处于哪个支付阶段。</div>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item timestamp="未确认" type="info">
              还没有登记付款信息，或尚未完成付款确认
            </el-timeline-item>
            <el-timeline-item timestamp="已付款" type="warning">
              付款方已登记支付，等待对方确认收款
            </el-timeline-item>
            <el-timeline-item timestamp="已确认" type="success">
              对方已确认到账，本次支付流程已完成
            </el-timeline-item>
            <el-timeline-item timestamp="争议中" type="danger">
              当前支付存在争议，可转入后续申诉处理
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header>
            <div>
              <strong>当前实验默认金额</strong>
              <div class="page-subtitle">打开确认弹窗时，会优先带出实验的默认报酬金额。</div>
            </div>
          </template>
          <el-statistic
            title="实验报酬"
            :value="Number(selectedExperiment?.paymentAmount || 0)"
            prefix="¥"
            :precision="2"
          />
          <div class="page-subtitle" style="margin-top: 12px; line-height: 1.8">
            {{ selectedExperiment?.paymentDescription || '当前实验暂未补充报酬说明。' }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="确认已付款" width="520px">
      <el-form ref="formRef" :model="confirmForm" :rules="formRules" label-position="top">
        <el-form-item label="报名 ID">
          <el-input :model-value="confirmForm.registrationId" disabled />
        </el-form-item>
        <el-form-item label="收款用户 ID">
          <el-input :model-value="confirmForm.payeeUserId" disabled />
        </el-form-item>
        <el-form-item label="支付金额（元）" prop="amount">
          <el-input-number v-model="confirmForm.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="支付凭证链接（可选）" prop="paymentScreenshotUrl">
          <el-input
            v-model="confirmForm.paymentScreenshotUrl"
            placeholder="可填写截图链接或文件地址，例如 https://..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="inline-actions" style="justify-content: flex-end; width: 100%">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitConfirm">确认已付款</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getExperimentById, getExperiments } from '../../api/experiments';
import { confirmPayer, getPaymentRecord } from '../../api/payments';
import { getExperimentRegistrations } from '../../api/registrations';
import PaymentStatusTag from '../../components/PaymentStatusTag.vue';
import RegistrationStatusTag from '../../components/RegistrationStatusTag.vue';
import { formatCurrency, formatDateTime } from '../../utils/format';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const formRef = ref();
const experiments = ref([]);
const paymentRows = ref([]);
const selectedExperiment = ref(null);
const selectedExperimentId = ref(route.query.experimentId ? Number(route.query.experimentId) : null);
const selectedRegistrationRow = ref(null);

const confirmForm = reactive({
  registrationId: null,
  payeeUserId: null,
  amount: 0,
  paymentScreenshotUrl: '',
});

const formRules = {
  amount: [{ required: true, message: '请输入支付金额', trigger: 'change' }],
};

function canConfirm(row) {
  const status = row.paymentRecord?.status || 'PENDING';
  return row.status === 'APPROVED' && status !== 'CONFIRMED' && status !== 'DISPUTED';
}

async function loadExperiments() {
  const data = await getExperiments({ page: 0, size: 100 });
  experiments.value = data.content || [];
}

async function loadExperimentDetail(experimentId) {
  selectedExperiment.value = await getExperimentById(experimentId);
}

async function enrichPaymentRecords(rows) {
  const enriched = await Promise.all(
    rows.map(async (row) => ({
      ...row,
      paymentRecord: await getPaymentRecord(row.id),
    })),
  );
  paymentRows.value = enriched;
}

async function loadRows() {
  if (!selectedExperimentId.value) {
    selectedExperiment.value = null;
    paymentRows.value = [];
    return;
  }

  loading.value = true;
  try {
    await loadExperimentDetail(selectedExperimentId.value);
    const rows = await getExperimentRegistrations(selectedExperimentId.value);
    await enrichPaymentRecords(rows || []);
  } finally {
    loading.value = false;
  }
}

async function handleExperimentChange() {
  if (selectedExperimentId.value) {
    router.replace({
      path: '/payments/confirm',
      query: { experimentId: String(selectedExperimentId.value) },
    });
  }
  await loadRows();
}

async function inspectRecord(row) {
  row.paymentRecord = await getPaymentRecord(row.id);
  if (row.paymentRecord) {
    ElMessage.success('已刷新支付状态');
  } else {
    ElMessage.info('当前报名记录还没有支付记录');
  }
}

function openConfirmDialog(row) {
  selectedRegistrationRow.value = row;
  confirmForm.registrationId = row.id;
  confirmForm.payeeUserId = row.userId;
  confirmForm.amount = Number(row.paymentRecord?.amount ?? selectedExperiment.value?.paymentAmount ?? 0);
  confirmForm.paymentScreenshotUrl = row.paymentRecord?.paymentScreenshotUrl || '';
  dialogVisible.value = true;
}

async function submitConfirm() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    const record = await confirmPayer({
      registrationId: confirmForm.registrationId,
      payeeUserId: confirmForm.payeeUserId,
      amount: confirmForm.amount,
      paymentScreenshotUrl: confirmForm.paymentScreenshotUrl || null,
    });

    if (selectedRegistrationRow.value) {
      selectedRegistrationRow.value.paymentRecord = record;
    }
    ElMessage.success('支付确认成功，状态已更新为已付款');
    dialogVisible.value = false;
    await loadRows();
  } finally {
    submitting.value = false;
  }
}

async function refreshCurrent() {
  await loadRows();
}

Promise.all([loadExperiments(), loadRows()]);
</script>
