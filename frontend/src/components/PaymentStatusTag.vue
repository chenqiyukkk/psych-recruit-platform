<template>
  <el-tag :type="meta.type" effect="light" round>
    {{ meta.label }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue';
import { paymentStatusMap } from '../constants/payments';

const props = defineProps({
  status: {
    type: String,
    default: '',
  },
});

const meta = computed(() => {
  if (!props.status) {
    return { label: '待查询', type: 'info' };
  }

  return paymentStatusMap[props.status] || {
    label: props.status,
    type: 'info',
  };
});
</script>
