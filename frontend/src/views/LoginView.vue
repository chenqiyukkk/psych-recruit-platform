<template>
  <div class="login-page">
    <div class="login-hero">
      <div class="hero-badge">Frontend Design · Research Console</div>
      <h1>让研究流程更清晰，也让管理体验更专业。</h1>
      <p>
        心试通 Web 管理端面向研究者与管理者，聚焦实验创建、报名审核、统计洞察与流程治理。
      </p>
      <div class="hero-grid">
        <div class="hero-card">
          <span>实验发布</span>
          <strong>从草稿到招募，全流程可控</strong>
        </div>
        <div class="hero-card">
          <span>审核中台</span>
          <strong>快速处理报名、签到与完成确认</strong>
        </div>
        <div class="hero-card">
          <span>数据概览</span>
          <strong>用统一视角追踪平台与实验状态</strong>
        </div>
      </div>
    </div>

    <el-card class="login-card" shadow="never">
      <template #header>
        <div>
          <div class="login-card__title">登录 Web 管理后台</div>
          <div class="login-card__subtitle">请使用研究者或管理员账号登录，账号体系与后端 JWT 鉴权直接打通。</div>
        </div>
      </template>

      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="被试账号只用于小程序端。若登录后角色为“被试”，系统会阻止进入 Web 后台。"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="login-form" @keyup.enter="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            size="large"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :loading="submitting" @click="submit">
          登录并进入工作台
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const formRef = ref();
const submitting = ref(false);

const form = reactive({
  username: '',
  password: '',
});

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    await authStore.login(form);
    ElMessage.success('登录成功');
    router.push((route.query.redirect && String(route.query.redirect)) || '/dashboard');
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 440px);
  align-items: stretch;
}

.login-hero {
  padding: 72px clamp(32px, 5vw, 84px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.24), transparent 26%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.94), rgba(30, 41, 59, 0.96));
  color: #eff6ff;
}

.hero-badge {
  align-self: flex-start;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  background: rgba(56, 189, 248, 0.16);
  border: 1px solid rgba(125, 211, 252, 0.18);
}

.login-hero h1 {
  margin: 22px 0 0;
  font-size: clamp(34px, 4vw, 54px);
  line-height: 1.1;
}

.login-hero p {
  max-width: 680px;
  margin: 18px 0 0;
  color: rgba(226, 232, 240, 0.84);
  font-size: 16px;
  line-height: 1.8;
}

.hero-grid {
  margin-top: 36px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.hero-card {
  padding: 18px;
  border-radius: 20px;
  background: rgba(15, 23, 42, 0.26);
  border: 1px solid rgba(148, 163, 184, 0.16);
  backdrop-filter: blur(12px);
}

.hero-card span {
  font-size: 12px;
  color: rgba(125, 211, 252, 0.92);
}

.hero-card strong {
  margin-top: 12px;
  display: block;
  line-height: 1.7;
  color: #f8fafc;
}

.login-card {
  margin: auto 32px auto 0;
  border-radius: 28px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
}

.login-card__title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.login-card__subtitle {
  margin-top: 8px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.login-form {
  margin-top: 20px;
}

.login-button {
  width: 100%;
  margin-top: 10px;
  height: 48px;
  border-radius: 14px;
}

@media (max-width: 1100px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-card {
    margin: -40px 20px 28px;
  }

  .hero-grid {
    grid-template-columns: 1fr;
  }
}
</style>
