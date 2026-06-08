<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">系统配置</h1>
        <p class="page-subtitle">
          集中维护实验表单依赖的配置项。当前系统主要使用逗号分隔字符串存储实验类型、地点与标签，请按原有格式编辑。
        </p>
      </div>
      <div class="inline-actions">
        <el-button @click="loadConfigs">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="18">
      <el-col :xs="24" :xl="17">
        <el-card class="panel-card" shadow="never">
          <el-table :data="records" v-loading="loading">
            <el-table-column label="配置项" width="180">
              <template #default="{ row }">
                <div class="config-key">{{ friendlyKey(row.configKey) }}</div>
                <div class="page-subtitle">{{ row.configKey }}</div>
              </template>
            </el-table-column>
            <el-table-column label="配置值" min-width="320">
              <template #default="{ row }">
                <el-tooltip :content="row.configValue || '--'" placement="top-start">
                  <div class="config-preview">{{ row.configValue || '--' }}</div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="说明" min-width="220">
              <template #default="{ row }">
                <div class="config-preview">{{ row.description || '--' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && !records.length" description="当前还没有可管理的系统配置项" />
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="7">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div>
              <strong>编辑说明</strong>
              <div class="page-subtitle">保存后会影响实验创建/编辑页中的候选值展示。</div>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item timestamp="experiment_types" type="primary">
              管理实验类型候选值，建议保持逗号分隔
            </el-timeline-item>
            <el-timeline-item timestamp="locations" type="success">
              管理实验地点候选值，实验表单中的地点下拉会同步读取
            </el-timeline-item>
            <el-timeline-item timestamp="experiment_tags" type="warning">
              管理实验标签候选值，实验表单右侧提示与标签体系将一起使用
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="编辑系统配置" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="配置键">
          <el-input :model-value="currentConfig?.configKey || ''" disabled />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input
            v-model="form.configValue"
            type="textarea"
            :rows="5"
            placeholder="请输入配置值。对于实验类型、地点、标签等，请继续使用逗号分隔字符串。"
          />
        </el-form-item>
        <el-form-item label="配置说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可补充当前配置项的说明文案" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="inline-actions" style="justify-content: flex-end; width: 100%">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">保存配置</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getAllConfigs, updateConfig } from '../../api/config';

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const formRef = ref();
const records = ref([]);
const currentConfig = ref(null);

const form = reactive({
  configValue: '',
  description: '',
});

const rules = {
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
};

const keyLabelMap = {
  experiment_types: '实验类型',
  locations: '常用地点',
  experiment_tags: '实验标签',
};

function friendlyKey(key) {
  return keyLabelMap[key] || key;
}

async function loadConfigs() {
  loading.value = true;
  try {
    records.value = await getAllConfigs();
  } finally {
    loading.value = false;
  }
}

function openDialog(row) {
  currentConfig.value = row;
  form.configValue = row.configValue || '';
  form.description = row.description || '';
  dialogVisible.value = true;
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid || !currentConfig.value) {
    return;
  }

  submitting.value = true;
  try {
    await updateConfig(currentConfig.value.configKey, {
      configValue: form.configValue,
      description: form.description || null,
    });
    ElMessage.success('系统配置已更新');
    dialogVisible.value = false;
    await loadConfigs();
  } finally {
    submitting.value = false;
  }
}

loadConfigs();
</script>

<style scoped>
.config-key {
  font-weight: 600;
  color: #0f172a;
}

.config-preview {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  line-height: 1.7;
  color: #334155;
}
</style>
