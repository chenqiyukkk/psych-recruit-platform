# psych-recruit-platform

“心试通”心理学被试招募平台项目仓库。

本项目面向高校心理学实验场景，目标是提供一个覆盖实验发布、资格校验、报名审核、知情同意、签到执行、双向评价、信誉分管理与统计导出的统一平台。

## 项目定位

- `backend`：Spring Boot 后端服务
- `frontend`：Vue 3 Web 管理后台
- `miniprogram`：微信小程序端
- `database`：数据库脚本与初始化数据
- `tests`：接口测试、性能测试与补充测试资料
- `docs`：需求、建模、工程化、测试与交付文档

## 目录结构

```text
psych-recruit-platform/
├─ docs/
│  ├─ requirements/
│  ├─ uml/
│  ├─ engineering/
│  ├─ testing/
│  └─ reports/
├─ backend/
│  └─ src/
├─ frontend/
│  └─ src/
├─ miniprogram/
│  └─ pages/
├─ database/
│  ├─ schema/
│  ├─ migration/
│  └─ seed/
├─ tests/
│  ├─ postman/
│  └─ performance/
├─ scripts/
│  ├─ dev/
│  └─ deploy/
└─ .github/
   └─ workflows/
```

## 技术栈规划

- Backend（后端）：Spring Boot、MySQL、Redis、Maven
- Frontend（前端）：Vue 3、Element Plus
- Mini Program（小程序）：微信小程序
- Engineering（工程化）：GitHub、GitHub Actions、Swagger / Knife4j、Jacoco、SonarQube

## 协作建议

- 主分支：`main`
- 集成分支：`develop`
- 功能分支：`feature/<module-name>`
- 缺陷分支：`bugfix/<issue-name>`
- 紧急修复分支：`hotfix/<issue-name>`

建议所有成员通过 Pull Request（合并请求）向 `develop` 合并代码，并在合并前完成基础自测与代码审查。

## 当前状态

当前仓库已完成初始目录搭建，后续可按团队分工逐步补充：

- 需求与设计文档
- UML 建模文件
- 后端与前端源码
- 小程序源码
- 数据库脚本
- CI 配置与测试资料
