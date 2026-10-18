# 影院购票管理系统 · 工程契约

## 先读哪里

- **`项目索引.md`** —— 唯一入口：文档地图、一页纸事实、易踩清单。
- 本文件 —— 开工前必守的 5 条。

## 开发守则

### 修改流程

1. 先通读后修改：跨端改动先读关键文件，不凭记忆
2. 三方校验：对比 文档 / 代码 / 数据库，定位不一致源头（库表快照见 `xm_film/springboot/src/test/resources/db/`）
3. 逐块验证：每个逻辑块改完单独验证
4. 主动启动验证：改完主动提议启动验证
5. 文档同步：代码变更后核对文档是否需更新（文档归属表见 `docs/engineering-rules.md` §3）

### 硬性约束

- 新增 `@SpringBootTest` 必须带 `@ActiveProfiles("ci")`；会写库的再加 `@Transactional`。**测试库是 `xm_film_test`，碰开发库即事故。**
- 新增 `/api/v1/**` 端点必须核对 `AuthInterceptor` 三个前缀集合（`docs/backend.md` §2）。
- 覆写 `BaseService.update` 必须委派 `super.update`（存在性判定的唯一出处）。
- 状态是中文字面量，改动须同步 Java / mapper XML / 前端常量三处。
- 动表格先读 `audit/table-spec.md`。
