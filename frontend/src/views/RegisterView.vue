<template>
  <div class="login-page">
    <div class="login-hero">
      <div class="hero-badge">心试通 · 研究者注册</div>
      <h1>从注册开始，把研究项目管理得更有秩序。</h1>
      <p>
        该入口仅面向研究者创建 Web 管理后台账号，帮助你快速接入实验创建、报名审核、支付确认与后续流程治理。
      </p>
      <div class="hero-grid">
        <div class="hero-card">
          <span>研究者专用</span>
          <strong>该入口默认创建研究者账号，不提供管理员或被试角色切换。</strong>
        </div>
        <div class="hero-card">
          <span>快速开始</span>
          <strong>注册完成后即可前往登录页，继续进入实验管理与后台工作流。</strong>
        </div>
        <div class="hero-card">
          <span>体验统一</span>
          <strong>延续后台现有卡片与层次风格，让注册入口也保持正式、清晰的产品体验。</strong>
        </div>
      </div>
    </div>

    <el-card class="login-card" shadow="never">
      <template #header>
        <div>
          <div class="login-card__title">注册研究者账号</div>
          <div class="login-card__subtitle">
            当前 Web 管理后台注册入口仅面向研究者开放。其他角色请使用对应的业务入口。
          </div>
        </div>
      </template>

      <el-alert
        type="warning"
        show-icon
        :closable="false"
        title="该注册入口会自动按研究者身份创建账号，不需要手动选择角色。"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="login-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="可选，便于后续联系" size="large" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="可选，便于接收通知" size="large" />
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
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入密码"
            size="large"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :loading="submitting" @click="submit">
          创建研究者账号
        </el-button>
      </el-form>

      <div class="register-footer">
        已有研究者或管理员账号？
        <el-button link type="primary" @click="router.push('/login')">返回登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { register } from '../api/auth';
import { ROLE_RESEARCHER } from '../constants/roles';

const router = useRouter();
const formRef = ref();
const submitting = ref(false);

const form = reactive({
  username: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
});

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请再次输入密码'));
          return;
        }
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'));
          return;
        }
        callback();
      },
      trigger: 'blur',
    },
  ],
  email: [
    {
      type: 'email',
      message: '请输入合法的邮箱地址',
      trigger: 'blur',
    },
  ],
};

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    await register({
      username: form.username,
      password: form.password,
      phone: form.phone || null,
      email: form.email || null,
      role: ROLE_RESEARCHER,
    });
    ElMessage.success('研究者账号注册成功，请继续登录后台');
    router.push({
      path: '/login',
      query: { username: form.username },
    });
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 460px);
  align-items: stretch;
}

.login-hero {
  padding: 72px clamp(32px, 5vw, 84px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.24), transparent 26%),
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

.register-footer {
  margin-top: 16px;
  text-align: center;
  color: #64748b;
  font-size: 14px;
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
