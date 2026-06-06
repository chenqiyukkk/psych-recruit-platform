<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ isEdit ? '编辑实验' : '创建实验' }}</h1>
        <p class="page-subtitle">表单字段与后端 `ExperimentCreateRequest / ExperimentUpdateRequest` 保持一致，可直接联调。</p>
      </div>
      <div class="inline-actions">
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">{{ isEdit ? '保存修改' : '创建实验' }}</el-button>
      </div>
    </div>

    <el-row :gutter="18">
      <el-col :xs="24" :xl="17">
        <el-card class="panel-card" shadow="never">
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-row :gutter="16">
              <el-col :xs="24" :md="12">
                <el-form-item label="实验标题" prop="title">
                  <el-input v-model="form.title" placeholder="请输入实验标题" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="伦理审批编号" prop="ethicsApprovalNo">
                  <el-input v-model="form.ethicsApprovalNo" placeholder="如：IRB-2026-PSY-001" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="实验描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入实验背景、流程与受试说明" />
            </el-form-item>

            <el-row :gutter="16">
              <el-col :xs="24" :md="8">
                <el-form-item label="地点" prop="location">
                  <el-select v-model="form.location" filterable allow-create default-first-option placeholder="请选择或输入实验地点" style="width: 100%">
                    <el-option v-for="item in options.locations" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="风险等级" prop="riskLevel">
                  <el-select v-model="form.riskLevel" placeholder="请选择风险等级" style="width: 100%">
                    <el-option v-for="item in RISK_LEVEL_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="支付方式" prop="paymentMethod">
                  <el-select v-model="form.paymentMethod" placeholder="请选择支付方式" style="width: 100%">
                    <el-option v-for="item in PAYMENT_METHOD_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :xs="24" :md="12">
                <el-form-item label="开始时间" prop="startTime">
                  <el-date-picker v-model="form.startTime" type="datetime" style="width: 100%" placeholder="选择开始时间" value-format="YYYY-MM-DDTHH:mm:ss" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="结束时间" prop="endTime">
                  <el-date-picker v-model="form.endTime" type="datetime" style="width: 100%" placeholder="选择结束时间" value-format="YYYY-MM-DDTHH:mm:ss" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :xs="24" :md="8">
                <el-form-item label="报酬金额（元）" prop="paymentAmount">
                  <el-input-number v-model="form.paymentAmount" :min="0" :precision="2" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="16">
                <el-form-item label="报酬说明" prop="paymentDescription">
                  <el-input v-model="form.paymentDescription" placeholder="例如：实验结束后线下扫码支付" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="筛选条件 JSON" prop="screeningCriteria">
              <el-input v-model="form.screeningCriteria" type="textarea" :rows="5" placeholder='例如：{"gender":"female","ageRange":[18,25]}' />
            </el-form-item>

            <el-form-item label="互斥标签 JSON" prop="excludeTags">
              <el-input v-model="form.excludeTags" type="textarea" :rows="4" placeholder='例如：["sleep-study","fmri"]' />
            </el-form-item>

            <el-divider content-position="left">实验标签与冷却期</el-divider>

            <div class="tag-editor">
              <div v-for="(tag, index) in form.tags" :key="index" class="tag-editor__row">
                <el-input v-model="tag.tagName" placeholder="标签名称，如睡眠、认知、fMRI" />
                <el-input-number v-model="tag.coolingDays" :min="0" style="width: 160px" />
                <el-button type="danger" plain @click="removeTag(index)">删除</el-button>
              </div>
              <el-button plain @click="addTag">新增标签</el-button>
            </div>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="7">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div>
              <strong>后端联调提示</strong>
              <div class="page-subtitle">下面这些字段会被后端严格校验。</div>
            </div>
          </template>
          <el-alert title="riskLevel 只能是 LOW / MEDIUM / HIGH" type="warning" :closable="false" show-icon />
          <el-alert title="paymentMethod 只能是 OFFLINE / ONLINE" type="warning" :closable="false" show-icon style="margin-top: 12px" />
          <el-alert title="screeningCriteria 与 excludeTags 必须是合法 JSON" type="warning" :closable="false" show-icon style="margin-top: 12px" />
          <el-divider />
          <div class="tag-list">
            <el-tag v-for="item in options.tags" :key="item" round effect="plain">{{ item }}</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  createExperiment,
  getExperimentById,
  getExperimentOptions,
  updateExperiment,
} from '../../api/experiments';
import { PAYMENT_METHOD_OPTIONS, RISK_LEVEL_OPTIONS } from '../../constants/experiments';

const route = useRoute();
const router = useRouter();
const formRef = ref();
const submitting = ref(false);
const options = reactive({
  experimentTypes: [],
  locations: [],
  tags: [],
});

const form = reactive({
  title: '',
  description: '',
  location: '',
  startTime: '',
  endTime: '',
  ethicsApprovalNo: '',
  riskLevel: 'LOW',
  paymentAmount: 0,
  paymentMethod: 'OFFLINE',
  paymentDescription: '',
  screeningCriteria: '',
  excludeTags: '',
  tags: [],
});

const rules = {
  title: [{ required: true, message: '请输入实验标题', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
  paymentAmount: [{ required: true, message: '请输入报酬金额', trigger: 'change' }],
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
};

const isEdit = computed(() => Boolean(route.params.id));

function addTag() {
  form.tags.push({ tagName: '', coolingDays: 0 });
}

function removeTag(index) {
  form.tags.splice(index, 1);
}

function normalizePayload() {
  return {
    ...form,
    tags: form.tags.filter((item) => item.tagName),
  };
}

async function loadOptions() {
  Object.assign(options, await getExperimentOptions());
}

async function loadDetail() {
  if (!isEdit.value) {
    return;
  }
  const detail = await getExperimentById(route.params.id);
  Object.assign(form, {
    title: detail.title || '',
    description: detail.description || '',
    location: detail.location || '',
    startTime: detail.startTime || '',
    endTime: detail.endTime || '',
    ethicsApprovalNo: detail.ethicsApprovalNo || '',
    riskLevel: detail.riskLevel || 'LOW',
    paymentAmount: Number(detail.paymentAmount ?? 0),
    paymentMethod: detail.paymentMethod || 'OFFLINE',
    paymentDescription: detail.paymentDescription || '',
    screeningCriteria: detail.screeningCriteria || '',
    excludeTags: detail.excludeTags || '',
    tags: (detail.tags || []).map((item) => ({ tagName: item.tagName, coolingDays: item.coolingDays })),
  });
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  submitting.value = true;
  try {
    if (isEdit.value) {
      await updateExperiment(route.params.id, normalizePayload());
      ElMessage.success('实验已更新');
    } else {
      await createExperiment(normalizePayload());
      ElMessage.success('实验已创建');
    }
    router.push('/experiments');
  } finally {
    submitting.value = false;
  }
}

Promise.all([loadOptions(), loadDetail()]);
</script>

<style scoped>
.tag-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tag-editor__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 160px auto;
  gap: 12px;
}
</style>
