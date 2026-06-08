<template>
  <el-tag :type="meta.type || 'info'" effect="light" round>
    {{ meta.label }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue';
import { registrationStatusMap } from '../constants/registrations';

const props = defineProps({
  status: {
    type: String,
    default: '',
  },
});

const meta = computed(
  () => registrationStatusMap[props.status] || { label: props.status || '未知状态', type: 'info' },
);
</script>
