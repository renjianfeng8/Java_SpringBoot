# Product Documentation Index

本目录用于补齐影院购票管理系统的需求阶段文档，承接当前已实现的 Spring Boot + Vue + MySQL 项目能力，并为后续迭代、答辩、验收和产品优化提供统一依据。

> **建议阅读顺序：** [术语表](./glossary.md) → [用户需求与业务需求](./01-user-and-business-requirements.md) → [竞品分析](./02-competitive-analysis.md) → [产品需求文档 PRD](./03-product-requirements-document.md) → [建模图](./05-modeling-diagrams.md) → [PRD 优化路线图](./04-prd-optimization-roadmap.md)

## 当前产品定位

影院购票管理系统面向三类角色：

- **用户端**：浏览电影、筛选影院、查看排片、在线选座、提交订单、查看订单、评分评价。（详见 [PRD §7.2](./03-product-requirements-document.md#72-用户前台)）
- **影院端**：维护本影院资料、管理影厅、排片、订单。（详见 [PRD §7.3](./03-product-requirements-document.md#73-影院后台)）
- **管理端**：维护平台基础数据，审核影院，管理电影、演员、分类、地区、公告、订单、评价和视频。（详见 [PRD §7.4](./03-product-requirements-document.md#74-管理后台)）

当前版本更适合作为”中小型院线/教学型票务平台”的全链路管理系统，而不是面向大规模商业化流量的公共票务平台。产品文档也按这个定位展开（详见 [竞品分析差异化定位](./02-competitive-analysis.md#7-差异化定位建议)）。

## 文档清单

| 文档 | 用途 | 适用场景 |
|------|------|----------|
| [术语表](./glossary.md) | 统一定义角色、实体、业务流程和技术术语 | 新读者入门、跨文档查阅、消除歧义 |
| [用户需求与业务需求](./01-user-and-business-requirements.md) | 明确用户、影院、管理员三端的需求边界 | 需求分析、立项说明、答辩前置材料 |
| [竞品分析](./02-competitive-analysis.md) | 对比猫眼、淘票票、Fandango、AMC 等票务产品 | 市场分析、功能取舍、差异化说明 |
| [产品需求文档 PRD](./03-product-requirements-document.md) | 形成完整产品定义、功能清单、流程和验收标准 | 开发排期、测试用例设计、项目验收 |
| [用例图、业务流程图与数据流图](./05-modeling-diagrams.md) | 补充 UML 用例、业务流程和 DFD 数据流建模 | 需求评审、系统分析、答辩图示 |
| [PRD 优化路线图](./04-prd-optimization-roadmap.md) | 基于现状给出 P0/P1/P2 优化计划 | 版本规划、二期迭代、毕业设计扩展 |

## 文档使用指南

### 谁应该读什么

| 角色 | 必读 | 参考 |
|------|------|------|
| 新加入项目 | [术语表](./glossary.md)、[PRD](./03-product-requirements-document.md) | — |
| 产品/需求分析 | [用户需求](./01-user-and-business-requirements.md)、[竞品分析](./02-competitive-analysis.md) | [建模图](./05-modeling-diagrams.md)、[路线图](./04-prd-optimization-roadmap.md) |
| 开发/测试 | [PRD](./03-product-requirements-document.md)、[术语表](./glossary.md) | [建模图](./05-modeling-diagrams.md)、[路线图](./04-prd-optimization-roadmap.md) |
| 答辩/汇报 | [用户需求](./01-user-and-business-requirements.md)、[PRD](./03-product-requirements-document.md)、[建模图](./05-modeling-diagrams.md) | [竞品分析](./02-competitive-analysis.md) |

### 三方一致原则

后续新增或修改需求时，需要同时检查：

- **文档**：本目录 PRD、README、AGENTS、设计文档是否同步。
- **代码**：前端页面、后端接口、权限策略是否覆盖。
- **数据库**：表结构、初始化数据、外键关系是否支撑需求。

如果三者不一致，以”当前代码可验证行为 + 数据库真实结构”为准，再反向更新文档。

### 交叉引用说明

本目录各文档通过“文档名 + 章节号 + 相对路径”的方式相互引用。所有业务术语首次出现时链接到 [术语表](./glossary.md)。文档间依赖关系如下：

```
术语表 ──→ 用户需求 ──→ PRD ──→ 建模图 ──→ 路线图
                     ↕          ↑         ↑
                竞品分析 ────────┘─────────┘
```

箭头 A → B 表示”B 引用了 A 的定义或内容”。
