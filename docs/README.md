# 项目文档中心

影院购票管理系统 (Cinema Ticket Management System) 的完整文档体系。

## 文档体系总览

| 目录 | 内容 | 适用场景 |
|------|------|----------|
| [product/](product/README.md) | 产品需求、竞品分析、PRD、路线图 | 产品分析、面试答辩 |
| [design/](design/README.md) | 系统设计、架构图、数据库设计、API 安全、部署设计 | 开发协作、架构评审 |
| [deployment/](deployment/) | 生产部署指南 | 运维上线 |
| [interview/](interview/) | 面试准备资料 | 求职答辩 |
| [superpowers/specs/](superpowers/specs/) | 架构重构与功能升级设计文档 | 技术决策追溯 |

## 快速链接

- [产品需求文档](product/README.md)
- [系统设计文档](design/README.md)
- [部署指南](deployment/deploy-guide.md)
- [Bug 修复记录](../Bug.md)
- [变更日志](../CHANGELOG.md)

## 三方一致原则

项目中出现的模块、接口、数据结构必须能在以下三处找到对应：

1. **文档**：需求文档、设计文档、README
2. **代码**：前端页面、后端 Controller/Service/Mapper
3. **数据库**：`schema.sql`、`data.sql`、实体字段和 MyBatis XML

如不一致，以当前可编译、可初始化、可通过 E2E 的实现为准，再反向更新文档。
