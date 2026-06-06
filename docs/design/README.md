# System Design Documentation Index

本目录用于补齐影院购票管理系统的系统设计阶段文档，承接 [产品需求文档](../product/README.md)，并与当前代码、数据库和部署配置保持一致。

## 文档清单

| 文档 | 用途 | 适用场景 |
|------|------|----------|
| [系统设计说明书](./01-system-design-specification.md) | 总体说明系统目标、架构、模块、数据、接口、安全和部署 | 系统设计评审、毕业设计正文、项目交付 |
| [架构与模块设计图](./02-architecture-and-module-diagrams.md) | 补充总体架构图、分层图、模块结构图、调用链路图 | 答辩展示、技术方案说明、开发协作 |
| [数据库设计说明](./03-database-design.md) | 说明表结构、实体关系、关键字段和一致性约束 | 数据库设计、SQL 维护、测试数据校验 |
| [接口与安全设计](./04-api-and-security-design.md) | 说明 RESTful API、认证授权、错误响应和文件上传安全 | 前后端联调、安全评审、接口测试 |
| [部署与运行设计](./05-deployment-design.md) | 说明本地、CI、Docker、环境变量和运行拓扑 | 部署交付、运维说明、CI/CD 说明 |

## 设计依据

- 当前产品需求：[docs/product](../product/README.md)
- 当前架构重构设计：[docs/superpowers/specs/2026-05-29-architecture-refactoring-design.md](../superpowers/specs/2026-05-29-architecture-refactoring-design.md)
- 后端主目录：`xm_film/springboot`
- 前端主目录：`xm_film/vue`
- 数据库脚本：`xm_film/sql`
- CI/E2E：`.github/workflows/ci.yml`、`xm_film/vue/e2e-tests`

## 三方一致原则

系统设计文档中出现的模块、接口、数据结构必须能在以下三处找到对应：

- 文档：需求文档、设计文档、README/AGENTS。
- 代码：前端页面/工具、后端 Controller/Service/Mapper。
- 数据库：`schema.sql`、`data.sql`、实体字段和 MyBatis XML。

如果后续设计和实现发生偏差，以当前可编译、可初始化、可通过 E2E 的实现为准，再反向更新设计文档。
