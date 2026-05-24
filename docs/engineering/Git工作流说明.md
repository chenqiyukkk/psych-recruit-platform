# Git 工作流说明

本文档用于说明 `psych-recruit-platform` 项目的 Git 协作流程，面向第一次参与团队开发的成员。目标是让每位成员都清楚以下问题：

- 仓库有哪些分支，它们分别做什么
- 第一次拿到项目后应该怎么操作
- 日常开发时应该在哪个分支写代码
- 写完代码后应该如何提交、推送、合并
- 遇到冲突、报错或操作失误时应该怎么办

本项目当前采用的是适合新手团队的三层分支工作流：

- `main`：正式稳定分支
- `develop`：团队日常集成分支
- `feature/*`：个人功能开发分支

---

## 1. 分支角色说明

### 1.1 `main` 分支

`main` 是项目的正式稳定版本分支，用来保存当前最适合演示、汇报、阶段提交和最终交付的代码。

对 `main` 的要求如下：

- 应尽量保持可以运行、可以展示、可以交付
- 不作为个人日常开发分支使用
- 不建议成员直接向 `main` 提交代码
- 只有在阶段性功能稳定后，才从 `develop` 合并到 `main`

可以把 `main` 理解为“成品区”。

### 1.2 `develop` 分支

`develop` 是团队日常协作和代码集成的主分支。每个人的功能开发完成后，应先合并到 `develop`，再统一测试。

对 `develop` 的要求如下：

- 作为团队开发主线使用
- 接收各成员的功能分支合并
- 用于联调、测试和阶段验收前检查
- 相比 `main` 更新更快，但稳定性可以略低一些

可以把 `develop` 理解为“总装区”或“集成区”。

### 1.3 `feature/*` 分支

`feature/*` 分支用于每位成员开发具体功能。每个成员应从 `develop` 拉取自己的功能分支，在该分支上独立开发。

示例：

- `feature/member1-backend-core`
- `feature/member2-registration`
- `feature/member2-signin`
- `feature/member3-web-admin`
- `feature/member4-miniapp-home`
- `feature/member5-support-modules`

对 `feature/*` 的要求如下：

- 一个功能分支尽量只负责一个模块或一类功能
- 不直接在 `main` 或 `develop` 上写业务代码
- 开发完成后，先推送到 GitHub，再发起合并到 `develop`

可以把 `feature/*` 理解为“个人工位”。

---

## 2. 整体工作流概览

本项目的完整分支流转关系如下：

```text
feature/*  ->  develop  ->  main
```

意思是：

1. 每位成员先在自己的 `feature/*` 分支开发
2. 开发完成后，将功能分支合并到 `develop`
3. 团队在 `develop` 上完成联调和测试
4. 阶段稳定后，再将 `develop` 合并到 `main`

请特别注意：

- 不要直接在 `main` 上开发
- 不要把未完成的半成品直接推到 `main`
- 尽量不要长期直接在 `develop` 上写功能代码

---

## 3. 第一次拿到项目时的操作

### 3.1 克隆仓库

每位成员第一次拿到项目时，只需要克隆一次整个仓库，不需要分别克隆 `main` 和 `develop`。

命令如下：

```bash
git clone https://github.com/chenqiyukkk/psych-recruit-platform.git
cd psych-recruit-platform
```

说明：

- `git clone` 克隆的是整个仓库，而不是某一个单独分支
- 仓库克隆到本地后，可以再切换到不同分支

### 3.2 获取远程分支信息

首次进入项目目录后，先执行：

```bash
git fetch origin
```

这条命令用于同步远程仓库的分支和最新状态。

### 3.3 切换到 `develop`

如果远程已经存在 `develop`，本地第一次切换时使用：

```bash
git switch -c develop origin/develop
```

如果本地已经有 `develop`，直接切换即可：

```bash
git switch develop
```

### 3.4 创建自己的功能分支

切换到 `develop` 后，再创建自己的功能分支：

```bash
git switch -c feature/memberX-模块名
```

例如：

```bash
git switch -c feature/member2-registration
```

说明：

- `memberX` 请替换成自己的成员编号
- `模块名` 要尽量写清楚功能内容
- 新功能分支必须从最新的 `develop` 拉出来，不要从 `main` 拉

---

## 4. 日常开发工作流

### 4.1 每次开始开发前

每次开始写代码前，都建议先同步 `develop`，保证自己不是基于过期代码开发。

操作顺序如下：

```bash
git switch develop
git pull origin develop
git switch feature/memberX-模块名
git merge develop
```

含义如下：

- 切换到 `develop`
- 拉取远程最新的 `develop`
- 回到自己的功能分支
- 将最新的 `develop` 合并进自己的分支

这样做的好处是：

- 能更早发现冲突
- 能减少最后合并时的大冲突
- 能确保你开发基于的是最新团队代码

### 4.2 开发过程中提交代码

建议每完成一个小功能，就进行一次提交，不要把很多天的修改堆成一次提交。

常用命令如下：

```bash
git add .
git commit -m "feat: 完成报名资格校验接口"
```

推荐的提交信息格式：

- `feat: 新增功能`
- `fix: 修复问题`
- `docs: 文档修改`
- `test: 测试代码`
- `refactor: 重构代码`
- `chore: 杂项调整`

示例：

- `feat: 完成报名申请接口`
- `fix: 修复签到重复提交问题`
- `docs: 补充 Git 工作流说明`
- `test: 增加信誉分模块单元测试`

### 4.3 推送到 GitHub

本地提交完成后，再把自己的分支推送到远程：

```bash
git push -u origin feature/memberX-模块名
```

第一次推送用 `-u`，后续再推送同一分支时可以直接用：

```bash
git push
```

---

## 5. 功能开发完成后的合并流程

当你负责的功能开发完成，并且已经完成基本自测后，不要直接合并到 `main`，也不建议直接把别人的 `develop` 当作自己的开发分支。

正确流程如下：

1. 将本地功能分支推送到 GitHub
2. 在 GitHub 上发起 Pull Request（PR，合并请求）
3. 将目标分支选择为 `develop`
4. 由负责人或其他组员检查代码
5. 确认无明显问题后合并到 `develop`

### 5.1 为什么要先合并到 `develop`

因为 `develop` 是团队集成和测试分支。各成员的模块先汇总到这里，方便：

- 联调接口
- 检查页面与后端是否对接成功
- 统一测试
- 发现跨模块问题

### 5.2 为什么不要直接合并到 `main`

因为 `main` 应该尽量保持稳定。如果大家都直接往 `main` 合并，容易出现：

- 半成品进入正式版本
- 有 bug 的代码直接进入演示版本
- 回滚和问题定位更困难

只有在 `develop` 中的功能已经较稳定、适合演示或阶段提交时，才将 `develop` 合并到 `main`。

---

## 6. 推荐的分支命名规范

### 6.1 主分支与集成分支

- `main`
- `develop`

### 6.2 功能分支

格式建议：

```text
feature/member编号-功能名
```

示例：

- `feature/member1-user-module`
- `feature/member2-registration`
- `feature/member2-signin`
- `feature/member3-web-admin`
- `feature/member4-miniapp-profile`
- `feature/member5-appeal-module`

### 6.3 问题修复分支

格式建议：

```text
bugfix/问题名
```

示例：

- `bugfix/signin-duplicate-submit`
- `bugfix/login-token-expired`

### 6.4 紧急修复分支

格式建议：

```text
hotfix/问题名
```

这种分支一般只用于需要快速修复正式版本时使用。

---

## 7. Pull Request 规则

建议团队统一遵循以下规则：

- 所有功能代码优先通过 PR 合并到 `develop`
- 合并前至少由 1 名组员简单查看
- 合并前自己先完成基本自测
- 如果有明显冲突，先在自己分支解决后再发起合并
- 大改动不要一次提交过多无关内容

PR 标题建议简洁明确，例如：

- `feat: 新增报名资格校验模块`
- `fix: 修复实验签到过期判断错误`
- `docs: 补充工程化文档`

PR 描述中建议写清楚：

- 这次改了什么
- 为什么要改
- 影响了哪些模块
- 有没有需要别人特别注意的地方

---

## 8. `develop` 合并到 `main` 的时机

只有在以下情况之一时，才建议将 `develop` 合并到 `main`：

- 阶段性功能已经跑通
- 可以用于答辩演示
- 可以形成里程碑版本
- 准备最终提交作业

合并前建议先确认：

- 核心功能可以运行
- 前后端至少完成基本联调
- 没有明显的阻塞性 bug
- 必要文档已补齐

---

## 9. 常用命令速查

### 9.1 查看当前分支

```bash
git branch
```

### 9.2 查看远程分支

```bash
git branch -a
```

### 9.3 拉取远程更新

```bash
git pull origin develop
```

### 9.4 新建并切换分支

```bash
git switch -c feature/member2-registration
```

### 9.5 切换分支

```bash
git switch develop
```

### 9.6 添加并提交修改

```bash
git add .
git commit -m "feat: 完成报名模块基础接口"
```

### 9.7 推送到远程

```bash
git push -u origin feature/member2-registration
```

### 9.8 合并 `develop` 到当前分支

```bash
git merge develop
```

### 9.9 查看工作区状态

```bash
git status
```

---

## 10. 常见问题与处理建议

### 10.1 为什么不能直接在 `main` 上写代码

因为 `main` 是稳定分支。直接在 `main` 上开发会导致正式版本混入未测试功能，增加冲突和回退风险。

### 10.2 为什么我不需要分别克隆 `main` 和 `develop`

因为 `git clone` 克隆的是整个仓库。分支是仓库内部的不同开发线，不需要为每个分支克隆一份独立目录。

### 10.3 如果我忘了切分支，直接在 `develop` 上写了代码怎么办

如果修改还没提交，可以先把改动转移到新分支；如果已经提交，应尽快联系负责人确认是否保留，并尽量避免再次发生。

建议做法是：每次开始开发前先执行 `git branch`，确认自己当前所在分支。

### 10.4 如果推送时报冲突怎么办

先不要慌，通常是因为你的分支落后于远程最新版本。建议：

1. 先同步最新 `develop`
2. 将 `develop` 合并到自己的功能分支
3. 解决冲突
4. 本地确认没问题后再推送

### 10.5 什么是冲突

冲突是指两个人修改了同一段代码，Git 无法自动判断应该保留谁的版本。这时需要人工选择或合并内容。

### 10.6 如何减少冲突

- 每个人负责的模块边界尽量明确
- 不要多人同时改同一个文件的同一部分
- 经常同步 `develop`
- 每次提交尽量聚焦一个小功能

---

## 11. 团队协作规则建议

为了让项目协作更顺畅，建议团队统一遵守以下规则：

- 不直接向 `main` 提交代码
- 日常开发统一基于 `develop`
- 每个人只在自己的 `feature/*` 分支开发
- 开发前先同步 `develop`
- 完成一个小功能就及时提交
- 合并前先自测
- 重要改动先在群里说明
- 发现冲突或问题尽早沟通，不要拖到最后

---

