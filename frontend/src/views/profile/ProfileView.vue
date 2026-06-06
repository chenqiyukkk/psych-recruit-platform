<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">个人资料</h1>
        <p class="page-subtitle">
          查看当前账号的角色、信誉分与评价概况，并维护 Web 端已开放的基础资料字段。
        </p>
      </div>
      <div class="inline-actions">
        <el-button @click="loadProfile">刷新资料</el-button>
      </div>
    </div>

    <div class="grid-cards">
      <section class="metric-card">
        <div class="metric-card__label">当前角色</div>
        <div class="metric-card__value">{{ profile?.role || '--' }}</div>
        <div class="metric-card__hint">用户名：{{ profile?.username || '--' }}</div>
      </section>
      <section class="metric-card" style="background: linear-gradient(135deg, #0f766e, #0ea5e9)">
        <div class="metric-card__label">信誉分</div>
        <div class="metric-card__value">{{ profile?.reputationScore ?? '--' }}</div>
        <div class="metric-card__hint">累计评价次数：{{ profile?.totalReviews ?? 0 }}</div>
      </section>
      <section class="metric-card" style="background: linear-gradient(135deg, #7c3aed, #2563eb)">
        <div class="metric-card__label">研究者评分</div>
        <div class="metric-card__value">{{ profile?.researcherRating ?? '--' }}</div>
        <div class="metric-card__hint">该字段只读展示，保存资料时不会修改</div>
      </section>
    </div>

    <el-row :gutter="18">
      <el-col :xs="24" :xl="10">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div>
              <strong>资料概览</strong>
              <div class="page-subtitle">这些字段来自 `GET /api/users/profile`，用于展示但不可直接编辑。</div>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ profile?.username || '--' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ profile?.role || '--' }}</el-descriptions-item>
            <el-descriptions-item label="信誉分">{{ profile?.reputationScore ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="研究者评分">{{ profile?.researcherRating ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="累计评价次数">{{ profile?.totalReviews ?? 0 }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="14">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div>
              <strong>基础资料编辑</strong>
              <div class="page-subtitle">当前后端仅支持更新手机号与邮箱。保存成功后会同步刷新顶部用户资料状态。</div>
            </div>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-row :gutter="16">
              <el-col :xs="24" :md="12">
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="form.phone" placeholder="请输入手机号，可留空" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="form.email" placeholder="请输入邮箱，可留空" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <div class="inline-actions" style="margin-top: 8px">
            <el-button @click="resetForm">重置</el-button>
            <el-button type="primary" :loading="submitting" @click="submit">保存资料</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getProfile, updateProfile } from '../../api/auth';
import { useAuthStore } from '../../stores/auth';

const authStore = useAuthStore();
const profile = ref(authStore.profile);
const formRef = ref();
const submitting = ref(false);

const form = reactive({
  phone: '',
  email: '',
});

const rules = {
  email: [
    {
      type: 'email',
      message: '请输入合法的邮箱地址',
      trigger: 'blur',
    },
  ],
};

function syncForm(data) {
  form.phone = data?.phone || '';
  form.email = data?.email || '';
}

async function loadProfile() {
  const data = await getProfile();
  profile.value = data;
  syncForm(data);
  return data;
}

function resetForm() {
  syncForm(profile.value);
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    await updateProfile({
      phone: form.phone || null,
      email: form.email || null,
    });
    await authStore.fetchProfile();
    profile.value = authStore.profile;
    syncForm(profile.value);
    ElMessage.success('个人资料已更新');
  } finally {
    submitting.value = false;
  }
}

loadProfile();
</script>
