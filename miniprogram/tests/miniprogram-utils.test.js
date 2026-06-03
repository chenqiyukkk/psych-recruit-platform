const test = require('node:test');
const assert = require('node:assert/strict');

const { buildUrl, unwrapResult, normalizePageData } = require('../utils/request');
const {
  formatDateTime,
  formatPayment,
  getExperimentStatusMeta,
  getRegistrationStatusMeta,
} = require('../utils/format');

test('buildUrl appends encoded query params and skips empty values', () => {
  const url = buildUrl('http://localhost:8080', '/api/experiments', {
    keyword: '记忆 实验',
    page: 0,
    size: 10,
    status: '',
    optional: undefined,
  });

  assert.equal(
    url,
    'http://localhost:8080/api/experiments?keyword=%E8%AE%B0%E5%BF%86%20%E5%AE%9E%E9%AA%8C&page=0&size=10'
  );
});

test('unwrapResult returns backend data and throws for non-zero result code', () => {
  assert.deepEqual(unwrapResult({ code: 0, data: { id: 1 }, message: 'OK' }), { id: 1 });
  assert.throws(() => unwrapResult({ code: 5001, message: '未登录' }), /未登录/);
});

test('normalizePageData supports Spring Page and plain arrays', () => {
  assert.deepEqual(
    normalizePageData({ content: [{ id: 1 }], totalElements: 3, number: 0, size: 10 }),
    { list: [{ id: 1 }], total: 3, page: 0, size: 10 }
  );
  assert.deepEqual(normalizePageData([{ id: 2 }]), {
    list: [{ id: 2 }],
    total: 1,
    page: 0,
    size: 1,
  });
});

test('format helpers produce miniapp friendly labels', () => {
  assert.equal(formatDateTime('2026-06-03T09:30:00'), '06-03 09:30');
  assert.equal(formatPayment(25), '¥25.00');
  assert.deepEqual(getExperimentStatusMeta('PUBLISHED'), { text: '招募中', className: 'success' });
  assert.deepEqual(getRegistrationStatusMeta('APPROVED'), { text: '已通过', className: 'success' });
});
