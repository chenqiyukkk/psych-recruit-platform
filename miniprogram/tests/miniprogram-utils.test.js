const test = require('node:test');
const assert = require('node:assert/strict');

const { buildUrl, unwrapResult, normalizePageData } = require('../utils/request');
const {
  formatDateTime,
  formatExperiment,
  formatPayment,
  getExperimentStatusMeta,
  getRegistrationStatusMeta,
} = require('../utils/format');
const fs = require('node:fs');
const path = require('node:path');

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

test('formatExperiment turns criteria JSON into readable participant requirements', () => {
  const experiment = formatExperiment({
    status: 'PUBLISHED',
    paymentAmount: 25,
    startTime: '2026-06-03T09:30:00',
    endTime: '2026-06-03T10:30:00',
    screeningCriteria: '{"include":{"gender":"FEMALE","age_range":[20,32]}}',
    excludeTags: '["fMRI","发展类"]',
  });

  assert.deepEqual(experiment.screeningCriteriaItems, [
    { label: '性别', value: '女' },
    { label: '年龄', value: '20-32 岁' },
  ]);
  assert.deepEqual(experiment.excludeTagTexts, ['fMRI', '发展类']);
  assert.doesNotMatch(experiment.screeningCriteriaText, /[{}[\]"]/);
});

test('profile page defaults to WeChat login with a test-account fallback', () => {
  const profileWxml = fs.readFileSync(
    path.join(__dirname, '../pages/profile/index.wxml'),
    'utf8'
  );
  const requestSource = fs.readFileSync(path.join(__dirname, '../utils/request.js'), 'utf8');

  assert.match(profileWxml, /微信授权登录/);
  assert.match(profileWxml, /测试账号登录/);
  assert.match(requestSource, /wxLogin/);
});

test('appeal page supports creating an appeal without navigating to a missing page', () => {
  const appealWxml = fs.readFileSync(
    path.join(__dirname, '../pages/appeals/index.wxml'),
    'utf8'
  );
  const appealSource = fs.readFileSync(path.join(__dirname, '../pages/appeals/index.js'), 'utf8');
  const requestSource = fs.readFileSync(path.join(__dirname, '../utils/request.js'), 'utf8');

  assert.match(appealWxml, /提交申诉/);
  assert.match(appealWxml, /选择已完成实验/);
  assert.doesNotMatch(appealSource, /\/pages\/appeal\/index/);
  assert.match(appealSource, /getRegistrations/);
  assert.match(appealSource, /selected\.experimentId/);
  assert.match(requestSource, /createAppeal/);
});

test('member4 testing deliverables include report, cases, defects and postman collection', () => {
  const report = fs.readFileSync(
    path.join(__dirname, '../../docs/testing/成员4-软件测试与质量保证报告.md'),
    'utf8'
  );
  const cases = fs.readFileSync(
    path.join(__dirname, '../../docs/testing/成员4-测试用例与缺陷跟踪.md'),
    'utf8'
  );
  const postman = JSON.parse(
    fs.readFileSync(
      path.join(__dirname, '../../tests/postman/API接口测试集合.postman_collection.json'),
      'utf8'
    )
  );

  assert.match(report, /测试准出结论/);
  assert.ok((cases.match(/TC-\d{3}/g) || []).length >= 50);
  assert.ok((cases.match(/BUG-\d{3}/g) || []).length >= 10);
  assert.ok(Array.isArray(postman.item));
  assert.ok(postman.item.length >= 8);
});
