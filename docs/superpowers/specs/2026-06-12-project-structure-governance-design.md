# 项目目录整治与文档治理设计

## 概述

对影院购票管理系统进行全面的目录结构标准化和文档治理，使其符合求职作品级的项目标准。

## 整治目标

1. **目录标准化** — 按行业标准 Spring Boot + Vue 项目惯例重组目录
2. **文档规范化** — 统一文档索引体系 + kebab-case 命名规则
3. **垃圾清理** — 移除所有日志文件、构建产物、测试残留、IDE 工件
4. **路径一致性** — 确保所有脚本、CI/CD、Docker 配置中的路径引用准确

## 目录结构（最终目标）

```
project_02/
│
├── README.md                    # GitHub 项目主页
├── CLAUDE.md                    # AI 助手指令（合并 AGENTS.md 后）
├── CHANGELOG.md                 # 变更日志
├── LICENSE                      # 许可证
├── Bug.md                       # Bug 修复记录
├── .gitignore / .dockerignore   # 忽略规则
├── .env.example                 # 环境变量模板
├── docker-compose.yml           # 容器编排（保留根目录）
│
├── docs/                        # 文档
│   ├── README.md                # 【新增】文档总索引
│   ├── product/                 # 产品需求文档（保持不动）
│   ├── design/                  # 系统设计文档（保持不动）
│   ├── deployment/
│   │   └── deploy-guide.md      # 【重命名】production-deploy.md → deploy-guide.md
│   ├── interview/
│   │   └── rbac-matrix.md       # 【重命名】05-rbac-matrix.md → rbac-matrix.md
│   └── superpowers/specs/       # 设计文档（保持不动）
│
├── scripts/                     # 【新建】通用脚本目录
│   ├── start-dev.bat            # ← 从根目录移入
│   ├── run-e2e-tests.bat        # ← 从根目录移入
│   ├── start-claude.bat         # ← 从根目录移入 + 重命名(kebab-case)
│   └── scan-project.sh          # ← 从 xm_film/scripts/ 移入
│
├── xm_film/                     # 项目源码主目录
│   ├── springboot/              # 后端（Spring Boot）
│   │   ├── Dockerfile           # 保留原位
│   │   ├── pom.xml
│   │   └── src/
│   ├── vue/                     # 前端（Vue 3）
│   │   ├── Dockerfile           # 保留原位
│   │   ├── nginx.conf
│   │   └── src/
│   └── sql/                     # 数据库脚本
│
└── .github/workflows/           # CI/CD
```

## 文档命名规范

| 规则 | 说明 | 示例 |
|------|------|------|
| 全部小写 | 不允许大写字母 | `deploy-guide.md` |
| kebab-case | 连字符分隔单词 | `competitive-analysis.md` |
| 无中文文件名 | 纯英文命名 | ❌ `产品需求.md` |
| 索引用 README.md | 无编号 | `docs/design/README.md` |
| 系列文档用 01- 前缀 | 按阅读顺序编号 | `01-system-design.md` |
| 独立文档无编号 | 术语表、部署指南等 | `glossary.md` |
| 设计文档用日期前缀 | 唯一例外 | `2026-06-12-xxx.md` |

## 清理清单

### 删除文件（14 项）

| # | 文件路径 | 原因 |
|---|---------|------|
| 1 | `AGENTS.md` | 内容合并至 CLAUDE.md |
| 2 | `backend-e2e.err.log` | 日志，应被 gitignore |
| 3 | `backend-e2e.log` | 日志 |
| 4 | `backend.log` | 日志 |
| 5 | `frontend-e2e.err.log` | 日志 |
| 6 | `frontend-e2e.log` | 日志 |
| 7 | `xm_film/springboot/backend.log` | 日志 |
| 8 | `xm_film/springboot/backend-e2e.log` | 日志 |
| 9 | `xm_film/springboot/backend-e2e.err.log` | 日志 |
| 10 | `xm_film/springboot/D:projectproject_02backend.log` | 乱码文件名，日志 |
| 11 | `xm_film/springboot/login.json` | 测试残留 |
| 12 | `xm_film/springboot/xm-film-backup-20260529.sql` | 备份错位 |
| 13 | `xm_film/vue/frontend-e2e.log` | 日志 |
| 14 | `xm_film/vue/frontend-e2e.err.log` | 日志 |
| 15 | `xm_film/vue/frontend.log` | 日志 |
| 16 | `xm_film/backend.log` | 日志 |
| 17 | `xm_film/frontend.log` | 日志 |

### 保留（已在 .gitignore 中无需操作）
- `springboot-jar/`（`*.jar` 规则覆盖）
- `xm_film/springboot/target/`（已有 `target/` 规则）
- `xm_film/vue/node_modules/`、`xm_film/vue/dist/`（已有 `node_modules/`、`dist/` 规则）

## 文件移动清单

| 源路径 | 目标路径 | 说明 |
|-------|---------|------|
| `start-dev.bat` | `scripts/start-dev.bat` | 脚本集中管理 |
| `run-e2e-tests.bat` | `scripts/run-e2e-tests.bat` | 脚本集中管理 |
| `start_claude.bat` | `scripts/start-claude.bat` | 脚本集中管理 + 重命名为 kebab-case |
| `xm_film/scripts/scan-project.sh` | `scripts/scan-project.sh` | 脚本集中管理 |

## 文件重命名清单

| 源路径 | 目标路径 | 规则原因 |
|-------|---------|---------|
| `docs/deployment/production-deploy.md` | `docs/deployment/deploy-guide.md` | 更简洁的独立文档命名 |
| `docs/interview/05-rbac-matrix.md` | `docs/interview/rbac-matrix.md` | 去掉无上下文的编号前缀 |

## 路径更新清单

### .gitignore
- `start_claude.bat` → `scripts/start-claude.bat`（路径随移动更新）

### CLAUDE.md
- 目录树中的 `scripts/` 路径更新
- 快速启动命令中的路径更新
- 合并 AGENTS.md 的重复内容后精简

### README.md
- 如有引用脚本路径则更新

### docs/design/README.md
- 如有交叉引用路径则更新

### docs/product/README.md
- 如有交叉引用路径则更新

### Docker 相关文件（无需改动）
- `docker-compose.yml` 引用 `Dockerfile` 路径不变
- `xm_film/vue/Dockerfile` 引用路径不变
- 根 `Dockerfile` 引用路径不变

### CI/CD（无需改动）
- `.github/workflows/ci.yml` 所有路径都是相对于 `xm_film/` 内部的，不受影响

## 新增文件

| 文件 | 内容 |
|------|------|
| `docs/README.md` | 文档总索引，链接所有子目录 README |

## 实施顺序

1. **清理垃圾文件** — 删除所有日志/残留
2. **合并 AGENTS.md** → 更新 CLAUDE.md 后删除 AGENTS.md
3. **创建 scripts/ 目录** — 移动 4 个脚本
4. **重命名文档** — deploy-guide.md + rbac-matrix.md
5. **创建 docs/README.md** — 文档总索引
6. **更新 .gitignore** — 脚本路径修正
7. **最终验证** — `git status` 确认

## 治理后文档索引

```
docs/
├── README.md              # [新增] 文档总索引
├── product/
│   ├── README.md
│   ├── glossary.md
│   ├── 01-user-and-business-requirements.md
│   ├── 02-competitive-analysis.md
│   ├── 03-product-requirements-document.md
│   ├── 04-prd-optimization-roadmap.md
│   └── 05-modeling-diagrams.md
├── design/
│   ├── README.md
│   ├── 01-system-design-specification.md
│   ├── 02-architecture-and-module-diagrams.md
│   ├── 03-database-design.md
│   ├── 04-api-and-security-design.md
│   └── 05-deployment-design.md
├── deployment/
│   └── deploy-guide.md    # [重命名]
├── interview/
│   └── rbac-matrix.md     # [重命名]
└── superpowers/specs/
    ├── 2026-05-29-architecture-refactoring-design.md
    ├── 2026-06-10-resume-grade-fullstack-upgrade.md
    └── 2026-06-12-project-structure-governance-design.md  # [新增]
```
