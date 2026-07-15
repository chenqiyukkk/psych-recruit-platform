<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ isEdit ? '编辑实验' : '创建实验' }}</h1>
        <p class="page-subtitle">请填写实验基础信息、参与人数、招募筛选条件与参与限制，系统会自动整理为可保存的实验设置。</p>
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
                  <el-date-picker
                    v-model="form.startTime"
                    type="datetime"
                    style="width: 100%"
                    placeholder="选择开始时间"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    :disabled-date="disabledStartDate"
                    :default-time="defaultStartTime"
                    editable="false"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="结束时间" prop="endTime">
                  <el-date-picker
                    v-model="form.endTime"
                    type="datetime"
                    style="width: 100%"
                    placeholder="选择结束时间"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    :disabled-date="disabledEndDate"
                    :default-time="defaultEndTime"
                    editable="false"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :xs="24" :md="8">
                <el-form-item label="参与人数上限" prop="participantLimit">
                  <el-input-number v-model="form.participantLimit" :min="1" :step="1" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="报酬金额（元）" prop="paymentAmount">
                  <el-input-number v-model="form.paymentAmount" :min="0" :precision="2" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="报酬说明" prop="paymentDescription">
                  <el-input v-model="form.paymentDescription" placeholder="例如：实验结束后线下扫码支付" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-divider content-position="left">招募筛选条件</el-divider>
            <p class="section-hint">用可视化表单设置适合参与本实验的人群范围，系统会自动保存为实验规则。</p>

            <el-row :gutter="16">
              <el-col :xs="24" :md="8">
                <el-form-item label="性别要求">
                  <el-select v-model="screening.gender" placeholder="请选择性别要求" style="width: 100%">
                    <el-option v-for="item in SCREENING_GENDER_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="最小年龄">
                  <el-input-number v-model="screening.minAge" :min="0" :max="120" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="最大年龄">
                  <el-input-number v-model="screening.maxAge" :min="0" :max="120" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :xs="24" :md="12">
                <el-form-item label="专业类别">
                  <el-select v-model="screening.majorCategories" multiple collapse-tags collapse-tags-tooltip placeholder="可多选适合的专业类别" style="width: 100%">
                    <el-option v-for="item in MAJOR_CATEGORY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="利手要求">
                  <el-select v-model="screening.handedness" placeholder="请选择利手要求" style="width: 100%">
                    <el-option v-for="item in HANDEDNESS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="补充筛选说明">
              <el-input v-model="screening.notes" type="textarea" :rows="3" placeholder="可补充需要特别说明的筛选要求，例如是否需要相关经历或配合程度。" />
            </el-form-item>

            <el-divider content-position="left">互斥规则</el-divider>
            <p class="section-hint">设置与本实验存在冲突或需要间隔参与的实验标签，避免短时间内重复参与相近实验。</p>

            <el-form-item label="互斥标签">
              <el-select
                v-model="exclusion.selectedTags"
                multiple
                filterable
                allow-create
                default-first-option
                collapse-tags
                collapse-tags-tooltip
                placeholder="请选择或输入需要互斥的标签"
                style="width: 100%"
              >
                <el-option v-for="item in options.tags" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>

            <el-form-item label="规则说明">
              <el-input
                v-model="exclusion.notes"
                type="textarea"
                :rows="3"
                placeholder="例如：参与过同类 fMRI 实验的被试，建议间隔一段时间后再报名。"
              />
            </el-form-item>

            <el-divider content-position="left">实验标签与冷却期</el-divider>
            <p class="section-hint">为实验添加标签并设置建议冷却期，便于后续识别同类实验与参与间隔。</p>

            <div class="tag-editor">
              <div v-for="(tag, index) in form.tags" :key="index" class="tag-editor__row">
                <el-select
                  v-model="tag.tagName"
                  filterable
                  allow-create
                  default-first-option
                  placeholder="选择或输入标签名称"
                  style="width: 100%"
                >
                  <el-option v-for="item in options.tags" :key="item" :label="item" :value="item" />
                </el-select>
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
              <strong>填写建议</strong>
              <div class="page-subtitle">以下信息会帮助你更清晰地说明招募范围与参与限制。</div>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item timestamp="基础信息" type="primary">
              先完善实验标题、时间、地点、报酬与风险等级，让参与者快速了解实验安排。
            </el-timeline-item>
            <el-timeline-item timestamp="筛选条件" type="success">
              通过性别、年龄、专业类别和利手要求设置适合参与的人群，无需手动编写规则文本。
            </el-timeline-item>
            <el-timeline-item timestamp="互斥规则" type="warning">
              使用互斥标签说明不建议重复参与的实验类型，帮助后续报名审核时快速判断。
            </el-timeline-item>
          </el-timeline>
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
import { useAuthStore } from '../../stores/auth';
import {
  createExperiment,
  getExperimentById,
  getExperimentOptions,
  updateExperiment,
} from '../../api/experiments';
import {
  HANDEDNESS_OPTIONS,
  MAJOR_CATEGORY_OPTIONS,
  PAYMENT_METHOD_OPTIONS,
  RISK_LEVEL_OPTIONS,
  SCREENING_GENDER_OPTIONS,
} from '../../constants/experiments';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
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
  participantLimit: 20,
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

const screening = reactive(createDefaultScreening());
const exclusion = reactive(createDefaultExclusion());

const rules = {
  title: [{ required: true, message: '请输入实验标题', trigger: 'blur' }],
  participantLimit: [{ required: true, message: '请设置参与人数上限', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
  paymentAmount: [{ required: true, message: '请输入报酬金额', trigger: 'change' }],
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
};

const isEdit = computed(() => Boolean(route.params.id));

const defaultStartTime = new Date(0, 0, 0, 8, 0, 0);  // 默认 08:00
const defaultEndTime = new Date(0, 0, 0, 18, 0, 0);    // 默认 18:00

function disabledStartDate(date) {
  // 开始时间不能选今天之前的日期
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date.getTime() < today.getTime();
}

function disabledEndDate(date) {
  // 结束时间不能选今天之前，且不能早于开始时间
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  if (date.getTime() < today.getTime()) return true;
  if (form.startTime) {
    const start = new Date(form.startTime);
    start.setHours(0, 0, 0, 0);
    return date.getTime() < start.getTime();
  }
  return false;
}

function createDefaultScreening() {
  return {
    gender: 'ANY',
    minAge: null,
    maxAge: null,
    majorCategories: [],
    handedness: 'ANY',
    notes: '',
    rawValue: '',
    rawType: null,
  };
}

function createDefaultExclusion() {
  return {
    selectedTags: [],
    notes: '',
    rawValue: '',
    rawType: null,
  };
}

function addTag() {
  form.tags.push({ tagName: '', coolingDays: 0 });
}

function removeTag(index) {
  form.tags.splice(index, 1);
}

function assignScreeningState(nextState) {
  Object.assign(screening, createDefaultScreening(), nextState);
}

function assignExclusionState(nextState) {
  Object.assign(exclusion, createDefaultExclusion(), nextState);
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

function fillScreeningFromJson(value) {
  const parsed = parseJsonValue(value);
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    assignScreeningState({
      rawValue: value || '',
      rawType: parsed === null ? null : typeof parsed,
    });
    return;
  }

  const include = parsed.include && typeof parsed.include === 'object' ? parsed.include : parsed;
  const ageRange = Array.isArray(include.age_range)
    ? include.age_range
    : Array.isArray(include.ageRange)
      ? include.ageRange
      : [];

  assignScreeningState({
    gender: include.gender || 'ANY',
    minAge: Number.isFinite(Number(ageRange[0])) ? Number(ageRange[0]) : null,
    maxAge: Number.isFinite(Number(ageRange[1])) ? Number(ageRange[1]) : null,
    majorCategories: Array.isArray(include.major_categories)
      ? include.major_categories
      : Array.isArray(include.majorCategories)
        ? include.majorCategories
        : [],
    handedness: include.handedness || 'ANY',
    notes: include.notes || parsed.notes || '',
    rawValue: '',
    rawType: null,
  });
}

function fillExclusionFromJson(value) {
  const parsed = parseJsonValue(value);
  if (Array.isArray(parsed)) {
    assignExclusionState({
      selectedTags: parsed.filter((item) => typeof item === 'string' && item.trim()).map((item) => item.trim()),
      notes: '',
      rawValue: '',
      rawType: null,
    });
    return;
  }

  if (parsed && typeof parsed === 'object') {
    const tags = Array.isArray(parsed.tags)
      ? parsed.tags
      : Array.isArray(parsed.excludeTags)
        ? parsed.excludeTags
        : [];
    assignExclusionState({
      selectedTags: tags.filter((item) => typeof item === 'string' && item.trim()).map((item) => item.trim()),
      notes: parsed.notes || parsed.description || '',
      rawValue: '',
      rawType: null,
    });
    return;
  }

  assignExclusionState({
    rawValue: value || '',
    rawType: parsed === null ? null : typeof parsed,
  });
}

function buildScreeningCriteria() {
  const include = {};

  if (screening.gender && screening.gender !== 'ANY') {
    include.gender = screening.gender;
  }
  if (screening.minAge !== null || screening.maxAge !== null) {
    include.age_range = [screening.minAge ?? null, screening.maxAge ?? null];
  }
  if (screening.majorCategories.length && !screening.majorCategories.includes('不限')) {
    include.major_categories = screening.majorCategories;
  }
  if (screening.handedness && screening.handedness !== 'ANY') {
    include.handedness = screening.handedness;
  }
  if (screening.notes?.trim()) {
    include.notes = screening.notes.trim();
  }

  return Object.keys(include).length ? JSON.stringify({ include }) : null;
}

function buildExcludeTags() {
  const tags = exclusion.selectedTags.map((item) => item.trim()).filter(Boolean);
  return tags.length ? JSON.stringify(tags) : null;
}

function normalizePayload() {
  return {
    ...form,
    screeningCriteria: buildScreeningCriteria(),
    excludeTags: buildExcludeTags(),
    tags: form.tags
      .filter((item) => item.tagName)
      .map((item) => ({
        tagName: item.tagName.trim(),
        coolingDays: Number(item.coolingDays ?? 0),
      })),
  };
}

function validateStructuredSections() {
  if (!form.participantLimit || Number(form.participantLimit) < 1) {
    ElMessage.warning('参与人数上限必须大于 0');
    return false;
  }

  if (screening.minAge !== null && screening.maxAge !== null && screening.minAge > screening.maxAge) {
    ElMessage.warning('最小年龄不能大于最大年龄');
    return false;
  }

  if (screening.rawValue) {
    ElMessage.warning('当前实验的筛选条件存在旧格式内容，请先在表单中重新确认后再保存。');
    return false;
  }

  if (exclusion.rawValue) {
    ElMessage.warning('当前实验的互斥规则存在旧格式内容，请先在表单中重新确认后再保存。');
    return false;
  }

  return true;
}

async function loadOptions() {
  Object.assign(options, await getExperimentOptions());
}

async function loadDetail() {
  if (!isEdit.value) {
    return;
  }
  try {
    const detail = await getExperimentById(route.params.id);
    if (authStore.isResearcher && detail.organizerId !== authStore.profile?.id) {
      ElMessage.warning('研究者仅可查看和编辑自己创建的实验');
      await router.replace('/experiments');
      return;
    }
    Object.assign(form, {
      title: detail.title || '',
      description: detail.description || '',
      location: detail.location || '',
      participantLimit: Number(detail.participantLimit ?? 20),
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
    fillScreeningFromJson(detail.screeningCriteria || '');
    fillExclusionFromJson(detail.excludeTags || '');
  } catch (error) {
    if (error?.response?.status === 403) {
      router.replace('/experiments');
    }
    throw error;
  }
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid || !validateStructuredSections()) {
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
.section-hint {
  margin: -4px 0 18px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

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

@media (max-width: 768px) {
  .tag-editor__row {
    grid-template-columns: 1fr;
  }
}
</style>
