# 性能测试 (Locust)

心理学被试招募平台的性能测试，使用 Locust 框架模拟多用户并发访问，验证系统在高负载下的表现。

---

## 📋 测试目标

| 指标 | 目标值 |
|------|--------|
| 接口平均响应时间 | < 500ms |
| P95 响应时间 | < 2s |
| 错误率 | < 1% |
| 并发用户数 | 100+ |
| RPS（每秒请求数） | > 50 |

---

## 🛠 环境准备

### 1. 安装 Python 依赖

```bash
pip install -r tests/performance/requirements.txt
```

### 2. 确保后端服务运行

```bash
cd backend
mvn spring-boot:run
```

### 3. 准备测试数据

```bash
# 运行测试数据生成脚本（如果已实现）
python scripts/generate_test_data.py

# 或手动在数据库中插入测试用户:
# - test_subject_1 ~ test_subject_50   (角色: 被试)
# - test_researcher_1 ~ test_researcher_20 (角色: 研究者)
# - admin (角色: 管理员)
# 密码均为: 123456
```

---

## 🚀 运行方式

### 方式一：Web UI 模式（推荐，可视化）

```bash
locust -f tests/performance/locustfile.py --host=http://localhost:8080
```

浏览器打开 `http://localhost:8089`，在 Web UI 中：
- **Number of users**: 设置并发用户数（建议从 10 开始，逐步增加到 100）
- **Spawn rate**: 每秒启动用户数（建议 5~10）
- **Host**: `http://localhost:8080`

点击 **Start swarming** 开始测试。

### 方式二：无 UI 模式（命令行，适合 CI/CD）

```bash
locust -f tests/performance/locustfile.py \
    --host=http://localhost:8080 \
    --headless \
    --users 100 \
    --spawn-rate 10 \
    --run-time 300s \
    --html=report.html \
    --csv=results
```

参数说明：
| 参数 | 说明 |
|------|------|
| `--headless` | 无 UI 模式 |
| `--users 100` | 模拟 100 个并发用户 |
| `--spawn-rate 10` | 每秒启动 10 个用户 |
| `--run-time 300s` | 持续运行 5 分钟 |
| `--html=report.html` | 生成 HTML 格式测试报告 |
| `--csv=results` | 生成 CSV 统计数据 |

### 方式三：分布式运行（大规模压测）

```bash
# Master 节点
locust -f tests/performance/locustfile.py --master --host=http://localhost:8080

# Worker 节点（在其他终端中运行多个）
locust -f tests/performance/locustfile.py --worker --master-host=localhost
```

---

## 📊 测试报告解读

### Locust Web UI 实时仪表盘

打开 `http://localhost:8089` 可查看：

- **Statistics**: 每个接口的请求数、失败数、平均/最小/最大响应时间、P50/P95/P99、RPS
- **Charts**: 实时响应时间曲线、RPS 曲线、用户数曲线
- **Failures**: 失败请求详情
- **Download Data**: 导出 CSV 统计数据

### HTML 报告

使用 `--html=report.html` 生成的报告包含完整的统计表格和图表。

### 终端输出

测试结束时会在终端打印摘要和达标情况：

```
📊 性能测试结果摘要
============================================================
  总请求数:     15230
  失败请求数:   12
  平均响应时间: 234 ms
  P50 响应时间: 180 ms
  P95 响应时间: 450 ms
  P99 响应时间: 890 ms
  最大响应时间: 2340 ms
  RPS (请求/秒): 50.8
  失败率:       0.08%
============================================================

✅ 非功能需求检验标准:
  ✅ 接口平均响应时间 < 500ms (实际: 234ms)
  ✅ P95 响应时间 < 2s (实际: 450ms)
  ✅ 错误率 < 1% (实际: 0.08%)
```

---

## 👥 模拟用户类型

| 用户类 | 权重 | 模拟行为 |
|--------|------|---------|
| **SubjectUser** | ~55% | 浏览实验、查看详情、报名、查看报名状态、查看通知 |
| **ResearcherUser** | ~25% | 创建实验、浏览列表、审核报名、确认支付、查看评价 |
| **AnonymousUser** | ~12% | 访问公开配置、浏览 Swagger |
| **AdminUser** | ~8% | 查看统计、审核申诉、查看信誉分 |

> 权重由 task 装饰器的数值控制，数字越大执行频率越高。

---

## 🔧 自定义测试场景

编辑 `locustfile.py` 中的参数和任务：

### 调整并发用户分布

修改每个 `@task(N)` 的权重值可以调整操作频率。

### 调整用户等待时间

修改 `wait_time = between(min, max)` 可以控制操作间隔。

### 添加新的测试场景

在对应的 User 类中添加新的 `@task` 方法即可。

---

## 📁 文件说明

```
tests/performance/
├── locustfile.py      # 性能测试脚本（主文件）
├── requirements.txt   # Python 依赖
└── README.md          # 本文档
```
