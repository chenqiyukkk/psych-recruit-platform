<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">管理员可查看平台所有用户、重置密码、禁用或启用账号（管理员不可禁用）。</p>
      </div>
      <div class="inline-actions">
        <el-button @click="loadUsers">刷新</el-button>
      </div>
    </div>

    <el-card class="panel-card" shadow="never">
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="160" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || '--' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">{{ row.email || '--' }}</template>
        </el-table-column>
        <el-table-column prop="reputationScore" label="信誉分" width="80" />
        <el-table-column label="研究者评分" width="140">
          <template #default="{ row }">
            <template v-if="row.researcherRating != null">
              {{ row.researcherRating }} <span style="color:#94a3b8;font-size:12px">({{ row.totalReviews ?? 0 }}次)</span>
            </template>
            <span v-else style="color:#94a3b8">--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.role === 'DISABLED'" link type="success" @click="enable(row)">启用</el-button>
            <el-button v-if="row.role !== 'DISABLED'" link type="primary" @click="resetPwd(row)">重置密码</el-button>
            <el-button v-if="row.role !== 'DISABLED'" link type="danger" :disabled="row.role === '管理员'" @click="remove(row)">
              禁用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="重置密码" width="400px">
      <el-form @submit.prevent="doReset">
        <el-form-item label="用户">
          <el-input :model-value="resetTarget?.username" disabled />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" placeholder="至少6位" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="doReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '../../api/http';

const loading = ref(false);
const users = ref([]);
const dialogVisible = ref(false);
const resetTarget = ref(null);
const newPassword = ref('');
const resetting = ref(false);

async function loadUsers() {
  loading.value = true;
  try {
    const data = await http.get('/users');
    users.value = Array.isArray(data) ? data : [];
  } finally {
    loading.value = false;
  }
}

function resetPwd(row) {
  resetTarget.value = row;
  newPassword.value = '';
  dialogVisible.value = true;
}

async function doReset() {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('密码至少6位');
    return;
  }
  resetting.value = true;
  try {
    await http.put(`/users/${resetTarget.value.id}/reset-password`, { password: newPassword.value });
    ElMessage.success(`用户 ${resetTarget.value.username} 的密码已重置`);
    dialogVisible.value = false;
  } finally {
    resetting.value = false;
  }
}

async function enable(row) {
  try {
    const { value: role } = await ElMessageBox.prompt(
      `为「${row.username}」选择恢复的角色：\n输入"研究者"或"被试"`,
      '启用用户',
      {
        confirmButtonText: '确认启用',
        cancelButtonText: '取消',
        inputValue: '被试',
        inputPattern: /^(研究者|被试)$/,
        inputErrorMessage: '只能输入"研究者"或"被试"',
      },
    );
    if (!role) return;
    await http.put(`/users/${row.id}/enable`, { role });
    ElMessage.success('用户已启用');
    await loadUsers();
  } catch (_error) {
    // 用户取消
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认禁用用户「${row.username}」吗？禁用后该账号将无法登录，但数据保留。`, '禁用确认', {
    type: 'warning',
  });
  await http.delete(`/users/${row.id}`);
  ElMessage.success('用户已禁用');
  await loadUsers();
}

loadUsers();
</script>
