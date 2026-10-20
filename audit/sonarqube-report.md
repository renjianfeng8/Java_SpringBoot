# SonarQube 扫描报告（2026-10-20）

> 环境：SonarQube 10.7.0.96327（Community，Docker）· 规则集 `Sonar way`（默认）
> 分析器：Java 8.2 · JavaScript/TypeScript/CSS 10.16 · XML 2.10
> 基线：`master` @ `0ff8331c`（工作区干净）
> 范围：`xm_film/springboot/src/main/java`、`src/test/java`、`xm_film/vue/src`；排除 `node_modules/`、`dist/`
> **状态**：基线 **74** 条 → P0 修复后 **69** 条 → 本轮清理后 **32** 条（漏洞 0，安全评级 A）。

---

## 0 一页纸

| 指标 | 基线（修复前） | P0 修复后 | 本轮清理后 |
| --- | --- | --- | --- |
| 文件数 | 251 | 253 | 253 |
| ncloc | 20,509 | 20,595 | 20,612 |
| 复杂度 / 认知复杂度 | 2,442 / 1,495 | 2,459 / 1,500 | 2,461 / 1,495 |
| 重复率 | 1.7%（22 块 / 457 行） | 1.7% | 1.7% |
| 注释率 | 9.6% | 9.7% | 9.7% |
| 技术债 | 402 分钟 | 392 分钟 | **191 分钟**（约 3.2 小时） |
| 评级 | 可靠性 **E** · 安全 **E** · 可维护性 **A** | 可靠性 **E** · 安全 **A** · 可维护性 **A** | 可靠性 **E** · 安全 **A** · 可维护性 **A** |
| 问题总数 | 74（后端 54 / 前端 20） | 69（后端 49 / 前端 20） | **32**（后端 20 / 前端 12；测试代码 0） |
| 级别分布 | BLOCKER 4 · CRITICAL 8 · MAJOR 36 · MINOR 25 · INFO 1 | BLOCKER 1 · CRITICAL 8 · MAJOR 34 · MINOR 25 · INFO 1 | BLOCKER 1 · CRITICAL 2 · MAJOR 24 · MINOR 4 · INFO 1 |
| 类型分布 | BUG 8 · 漏洞 1 · 坏味 65 | BUG 6 · 漏洞 0 · 坏味 63 | BUG 3 · **漏洞 0** · 坏味 29 |
| 安全热点 | 4 | 2 | 2（均为已判定为安全的正则） |

**一句话结论**：三条 P0（支付密码事务边界、硬编码默认口令、取票码随机源）与本轮 11 类清理项都已修复，测试代码问题清零；安全评级 A、漏洞 0。剩余 32 条里，24 条是“自调用但传播行为正确”的 `S6809` 与 ARIA 语义建议，2 条是前端复杂度；可靠性 E 只因 3 条 BUG，其中 3 条全部是**已核验的误报/有意为之**（`Thread.sleep` 限流、volatile 不可变快照、Vue 键盘指令不被识别）。

> ⚠️ 质量门禁显示 `OK` **不代表没有问题**：该门禁只配置了「新代码违规数 = 0」，本次是基线扫描（无 Previous Version 可比），所以整体问题数不影响门禁结果。判定请看上面的级别分布。

---

## 0.1 P0 修复记录（2026-10-20）

| 条目 | 改动 | 守卫测试 | 反证结果 |
| --- | --- | --- | --- |
| **BL-50 / Sonar `java:S2229`**<br>支付密码 `REQUIRES_NEW` 被自调用绕过 | `PayPasswordService` 不再用 `@Transactional`，改为复制一个 `TransactionTemplate` 并显式设 `REQUIRES_NEW`。不依赖 AOP 代理，`changePayPassword` / `verifyOldPassword` 怎么调都新开事务。 | ① `PayPasswordTransactionBoundaryTest`：真实库里开外层事务 → 输错一次 → 外层回滚 → 换连接读到计数 **1**；<br>② `PayPasswordServiceTest#verifyOldPasswordRunsInRequiresNewTransaction`：断言实际传播级别是 `REQUIRES_NEW`。 | 把传播级别改回 `REQUIRED` → 两条守卫都红（计数为 0），已确认还原。 |
| **BL-51 / Sonar `java:S2245`**<br>取票码用 `ThreadLocalRandom` | 新增 `common/SecureCodes`（`SecureRandom`），`generatePickupCode` 改为从它取 8 位；同一工具也供导入口令使用。 | 既有 `OrderedServiceTest#payOrderGeneratesPickupCode` 继续钉 4-4 格式与字母表。 | 随机源质量无法用行为断言反证；以「唯一出处 + 代码评审」为准。 |
| **Sonar `java:S6437`**<br>批量导入影院共用 `cinema123` | 每个账号生成独立的 12 位随机初始口令（约 70 bit 熵），库中只存 BCrypt 哈希；明文只在 `POST /cinemas/from-directory` 的 `accounts[]` 里返回一次。`insertCinema` 对空口令返回 400，不再回退默认值。后台名录页新增「账号与初始口令」弹窗 + 复制全部。 | `CinemaServiceTest` 新增/加强 3 条：明文能匹配落库哈希、不同账号口令不同、空口令被拒。 | 改回共用 `cinema123` → 3 条用例红（`expected: not equal but was: <cinema123>`），已确认还原。 |
| **BL-52 / Sonar `java:S1874`**<br>弃用异常（4 条） | `CinemaService` 的 catch 改为父类型 `PessimisticLockingFailureException`，覆盖死锁与等锁超时；测试同步。另 1 条是 Spring 6.2 弃用的 `HttpMessageNotReadableException` 单参构造器，改用带 `MockHttpInputMessage` 的两参形式。 | `CinemaServiceTest#importFromDirectoryReportsLockWaitTimeoutAsRetryableConflict` 用 `CannotAcquireLockException` 证明超时那半也被捕获。 | 若只捕死锁子类，新增的等锁超时用例会红；已由该用例钉住。 |

**回归**：`mvn -o clean test` = **398/398 通过**；`npm run build` 成功。

---

## 0.2 本轮清理（2026-10-20，第二轮）

| 规则 | 原数量 | 处理 | 反证/验证 |
| --- | --- | --- | --- |
| `java:S2259` | 3 | `TmdbClient.get()` 对 200 + 空 body 抛 `RestClientException`；`GlobalExceptionHandler` 先取局部变量消除误报 | 新增 `TmdbClientTest#emptyBodyThrowsRestClientExceptionInsteadOfNullPointer`。**反证**：删掉空值判断 → 用例红，异常正是 Sonar 预测的 `NullPointerException: root is null` |
| `java:S1874` | 4 | 见 §0.1 BL-52 | 新增等锁超时守卫 |
| `java:S1192` | 4 | `AmapClient` 的 `districts`/`level`、`TmdbClient` 的 `language`、`RoomService` 的影院不存在提示语各抽常量 | 全量测试 |
| `java:S1128` | 1 | 删除 `OrderStatusCriteria` 多余的 `@Param` import | 编译 |
| `java:S6213` | 3 | `RecordController` 的局部变量/参数 `record` → `recordItem`；测试方法 `record()` → `recordOf()` | 全量测试 |
| `typescript:S3863` | 2 | `back/Room.vue` 两处 `@/constants` import 合并 | 前端构建 |
| `Web:AvoidCommentedOutCode` | 3 | 三处其实是**解释性注释**（不是死代码），改写措辞去掉代码形态（`loading="lazy"`、`v-if="form.id"`、`native-type="button"`） | 复扫为 0 |
| `javascript:S3358` | 1 | `CinemaDetail.vue` 的嵌套三元抽成 `weekLabelOf()` | 前端构建 |
| `javascript:S3735` | 1 | `Home.vue` 的 `void offsetWidth` 改为 `getBoundingClientRect()` 调用 | 前端构建 |
| `javascript:S2004` | 1 | `BuyTicket.vue` 抽 `markSeatInMatrix()`，内层 `forEach` 改 `for...of` | 复扫为 0 |
| `java:S6068` | 8 | 删除无意义的 Mockito `eq(...)`（含 2 个因此闲置的 import） | 全量测试 |
| `java:S5838` | 6 | `isEqualTo(0)`→`isZero()`、`isGreaterThanOrEqualTo(0)`→`isNotNegative()`、`toString` 比较→`hasToString()` | 全量测试 |

**结果**：69 → **32**；技术债 392 → **191 分钟**；测试代码问题 **18 → 0**。

---

## 1 扫描口径（影响结论的三个细节）

1. **Java 必须带 Maven 依赖类路径**。用根 POM 直接扫时 `sonar.java.libraries` 为空，Lombok/Spring 类型解析不到，会凭空多出 **548 条 `java:S1068`（未使用私有字段）**。本报告的结果是把 `mvn dependency:build-classpath` 的 87 个依赖注入 `sonar.java.libraries` 后得到的；同理，测试源码只有注入 `sonar.java.test.libraries` 才能正确识别 `assertThrows` 语义。
2. **测试源码单独识别**（`sonar.tests`，42 个文件）。测试里的 `S5778`（一次 lambda 多个可能抛异常的调用）在类路径完整后全部消失，说明它们此前是**类型未解析造成的误报**，不是真问题。
3. **没有覆盖率数据**。仓库未接 JaCoCo，本次覆盖率不参与评级；「测试覆盖缺口」仍以 `audit/backend-backlog.md` 的手工结论为准。

---

## 2 规则维度总览（最终 32 条）

| 规则 | 级别 | 数量 | 说明 |
| --- | --- | --- | --- |
| `java:S6809` | MAJOR | 14 | 同类内 `this` 调用带 `@Transactional` 的方法（均为 `REQUIRED`，行为正确） |
| `Web:S6819` | MAJOR | 8 | 用 ARIA `role` 代替语义标签 |
| `javascript:S3776` | CRITICAL | 2 | 认知复杂度超阈值（23 / 18） |
| `java:S2276` | BLOCKER | 1 | 持锁时 `Thread.sleep`（限流本身，规则给错改法） |
| `java:S3011` | MAJOR | 1 | 反射 `setAccessible(true)` |
| `java:S1172` | MAJOR | 1 | 未使用的参数（有意保留的归属校验签名） |
| `java:S135` | MINOR | 1 | 循环内多个 `continue` |
| `java:S3077` | MINOR | 1 | `volatile Map`（不可变快照，正确写法） |
| `javascript:S1481` | MINOR | 1 | 解构排除字段 `_token`（有意保留） |
| `Web:MouseEventWithoutKeyboardEquivalent` | MINOR | 1 | Vue `@keyup.enter` 不被识别（误报） |
| `java:S6548` | INFO | 1 | 无状态共享单例（误报） |

---

## 3 已修复的 P0（保留诊断，便于复查时理解“为什么这么改”）

### 3.1 `java:S2229`（BLOCKER ×2）—— 支付密码限次曾被外层事务回滚

**原位置**：`service/PayPasswordService.java:84`、`:103`

`changePayPassword` 与 `verifyOldPassword` 通过 `this.checkOldPassword(...)` 调用一个标注了
`@Transactional(propagation = REQUIRES_NEW)` 的方法。Spring 的事务代理不拦截自调用，`REQUIRES_NEW` **不会生效**。

代码注释已经写明「代理会被绕过，靠入口没有外层事务、计数靠自动提交落库」，所以当时的演示路径是对的。但这是一个没有守卫的隐含前提：
任何一天有人给调用链加上外层事务，失败次数与锁定就会随外层一起回滚，**5 次/15 分钟的支付密码锁定会静默失效**。

**修复**：改用 `TransactionTemplate` 显式 `REQUIRES_NEW`，见 §0.1。现在 `checkOldPassword` 无论被谁调用都新开事务，失败计数一定提交。

### 3.2 `java:S6437`（VULNERABILITY）—— 批量导入影院共用硬编码口令

**原位置**：`service/CinemaService.java:139`（常量在 `:56`，`DEFAULT_PASSWORD = "cinema123"`）

名录导入创建的影院账号全部使用同一口令，用户名规则 `amap_+poiId` 可推导，且登录无失败锁定。Sonar 判为 VULNERABILITY 是准确的。

**修复**：逐账号随机初始口令 + 一次性返回，见 §0.1。`insertCinema` 也不再回退默认口令（空口令 400）。

### 3.3 `java:S2245`（安全热点）—— 取票码用 `ThreadLocalRandom`

**原位置**：`service/OrderedService.java:638`（`generatePickupCode`）

取票码是匿名核销端点 `POST /tickets/redeem` 的**唯一凭证**。`ThreadLocalRandom` 不是密码学安全随机数，内部种子可预测；「31⁸ ≈ 8.5e11」只说明暴力枚举难，不说明不可预测。

**修复**：统一走 `common/SecureCodes`（`SecureRandom`），见 §0.1。既有格式/字母表测试保持通过。

### 3.4 `java:S2276`（BLOCKER）—— 结论：有意为之，不建议按规则改

**位置**：`common/config/AmapConfig.java:60`

规则要求「持锁时用 `wait()` 代替 `Thread.sleep()`」。这里的 `sleep` 在 `synchronized awaitSlot()` 里，**阻塞就是限流本身**：改成 `wait()` 会释放监视器，并发请求会一起通过，最小请求间隔失效。
**判定**：误报（按意图）。可加一行说明或改 `Semaphore` 重写，但不必为规则牺牲语义。

---

## 4 后端剩余（20 条）

### 4.1 `java:S6809`（14 条）—— 自调用，但传播行为相同

抓的全是 `@Transactional` 的 `xxxScoped(...)` 内部调用继承自 `BaseService` 的 `add/update/delete/deleteBatch`。
这些基类方法都是 `REQUIRED`，外层事务已存在，**行为正确**；与 §3.1 那个 `REQUIRES_NEW` 不是一回事（那两处已随 BL-50 消失，`PayPasswordService` 的 2 条也已清零）。
**建议**：不必逐条改（收益低、翻动面大）；若要消警，在基类方法上注明「允许自调用，传播行为与调用方一致」，或统一改为直接调 mapper。

### 4.2 单条与误报

| 规则 | 位置 | 判断 |
| --- | --- | --- |
| `java:S1172` | `service/RecordService.java:195` 的 `role/userId` 未用 | 注释已说明「只做存在性校验」；保留签名可接受，消警可加 `@SuppressWarnings` |
| `java:S3011` | `common/BaseController.java:131` 反射 `setAccessible(true)` | 有意为之；可换 `ReflectionUtils.makeAccessible` 或 `BeanWrapper` |
| `java:S135` | `service/TmdbBackfillService.java:45` 循环内多个 `continue` | 低优先，改早退会降低可读性 |
| `java:S3077` | `common/TmdbClient.java:54` `volatile Map` | **误报**：`Map.copyOf` 不可变快照 + volatile 发布，是正确写法 |
| `java:S6548` | `common/OrderStatusCriteria.java:28` 单例告警 | **误报**：无状态共享实例，注释已解释 |
| `java:S2276` | `common/config/AmapConfig.java:60` | **有意为之**，见 §3.4 |

---

## 5 前端剩余（12 条）

| 规则 | 数量 | 位置与判断 |
| --- | --- | --- |
| `Web:S6819` | 8 | `role="group"/"button"/"img"/"status"`；Element Plus 场景下多为可接受，至少 `manage/Film.vue:205` 的 `div role="button"` 建议换 `<button>`（需产品/样式确认） |
| `javascript:S3776` | 2 | `views/front/FilmDetail.vue:291`（23）、`views/manage/Film.vue:369`（18）。可拆函数，非缺陷 |
| `javascript:S1481` | 1 | `back/Person.vue:68` `_token` 解构排除字段 —— **误报**（有意保留，注释已说明） |
| `Web:MouseEventWithoutKeyboardEquivalent` | 1 | `manage/Film.vue:205` —— **误报**：模板已有 `@keyup.enter`，Sonar 不识别 Vue 指令 |
| `javascript:S5852` | 2（热点） | `BuyTicket.vue:373/488` 的 `/(\d+)排/`、`/排(\d+)座/`。输入是服务端生成的座位标签（长度 ≤ 十余字符），**无 DoS 面**；可不改 |

前端没有发现 `v-html`、`eval`、硬编码密钥一类的安全问题。

---

## 6 与债务/安全清单的映射

| 本次发现 | 对应条目 | 状态 |
| --- | --- | --- |
| `S6437` 硬编码默认口令 | 原安全章节「连带发现」第 3 条 | **已修复**（逐账号随机口令，一次性返回） |
| `S2229` 自调用使 `REQUIRES_NEW` 失效 | **BL-50** | **已修复**（TransactionTemplate） |
| `S2245` 取票码非加密随机数 | **BL-51** | **已修复**（SecureCodes） |
| `S1874` 弃用异常 | **BL-52** | **已修复**（父类型 + 两参构造器） |
| `S2259` TMDB 空 body | 本报告 §4.1（原） | **已修复** + 守卫测试 |
| `S1192`/`S1128`/`S6213`/`S3863`/`S6068`/`S5838` | 清理项 | **已清零** |
| 前端 12 条 | `audit/frontend-audit.md` | 建议登记 FE-67 起（复杂度/ARIA） |

---

## 7 剩余建议

1. **`S6819`（8 条）**：按页面上报的 `role` 逐条确认；`manage/Film.vue:205` 优先换原生 `<button>`，其余多为 Element Plus 组件的语义补充。
2. **`S3776`（2 条）**：`FilmDetail.vue` 的详情加载与 `manage/Film.vue` 的导入流程可拆成 2–3 个小函数；属可维护性，不是缺陷。
3. **`S6809`（14 条）**：批量消警收益低，建议在 `BaseService` 一处注释说明，不做逐条改动。
4. **`S1172`/`S3011`/`S135`**：保留现状并注明理由，或按上表替换为框架工具。
5. **4 条误报**（`S2276`/`S3077`/`S6548`/MouseEvent）：在 Sonar 面板标记 `Won't Fix` 并写理由，避免下一轮重复审查。
6. **安全专项仍是主线**：SEC-01（充值回调无验签）、SEC-02（JWT 密钥硬编码）、SEC-03（明文回退）未修，优先级高于本报告剩余全部条目。

---

## 8 如何复现

```bash
# 1) 启动（Docker Hub 不可达时用镜像站，首次约 817MB）
docker run -d --name sonarqube -p 9000:9000 \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  docker.m.daocloud.io/library/sonarqube:10.7-community

# 2) 后端编译 + 生成依赖类路径
cd xm_film/springboot
mvn -o -DskipTests test-compile
mvn -o dependency:build-classpath -Dmdep.outputFile=cp.txt

# 3) 在仓库根放一个扫描用 POM（仅本次，用完删除），关键属性：
#    sonar.sources        = xm_film/springboot/src/main/java,xm_film/vue/src
#    sonar.tests          = xm_film/springboot/src/test/java
#    sonar.java.binaries  = xm_film/springboot/target/classes
#    sonar.java.test.binaries = xm_film/springboot/target/test-classes
#    sonar.java.libraries / sonar.java.test.libraries = cp.txt 内容（逗号分隔）
#    sonar.exclusions     = **/node_modules/**,**/dist/**
mvn -f scan-pom.xml org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar \
  -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<local-token>

# 4) 用完清理
docker stop sonarqube && docker rm sonarqube
```

本次的 `scan-pom.xml` 与 `cp.txt` 是临时文件，已删除，未提交。

---

## 附录：全部问题清单（最终 32 条，按规则分组）

### `java:S2276`（BLOCKER / BUG，1 条）
- `common/config/AmapConfig.java:60`

### `javascript:S3776`（CRITICAL / CODE_SMELL，2 条）
- `vue/views/front/FilmDetail.vue:291`
- `vue/views/manage/Film.vue:369`

### `java:S6809`（MAJOR / CODE_SMELL，14 条）
- `service/CinemaService.java:275`
- `service/FilmService.java:134`
- `service/RecordService.java:220`
- `service/RecordService.java:249`
- `service/RecordService.java:264`
- `service/RecordService.java:279`
- `service/RoomService.java:94`
- `service/RoomService.java:116`
- `service/RoomService.java:125`
- `service/RoomService.java:138`
- `service/UserService.java:162`
- `service/UserService.java:167`
- `service/UserService.java:178`
- `service/UserService.java:191`

### `Web:S6819`（MAJOR / CODE_SMELL，8 条）
- `vue/views/front/BuyTicket.vue:57`
- `vue/views/front/BuyTicket.vue:113`
- `vue/views/front/Account.vue:36`
- `vue/views/front/Account.vue:135`
- `vue/views/front/Cinema.vue:29`
- `vue/views/front/Cinema.vue:47`
- `vue/views/front/CinemaDetail.vue:85`
- `vue/views/manage/Film.vue:205`

### `java:S3011`（MAJOR / CODE_SMELL，1 条）
- `common/BaseController.java:131`

### `java:S1172`（MAJOR / CODE_SMELL，1 条）
- `service/RecordService.java:195`

### `java:S135`（MINOR / CODE_SMELL，1 条）
- `service/TmdbBackfillService.java:45`

### `java:S3077`（MINOR / BUG，1 条）
- `common/TmdbClient.java:54`

### `javascript:S1481`（MINOR / CODE_SMELL，1 条）
- `vue/views/back/Person.vue:68`

### `Web:MouseEventWithoutKeyboardEquivalentCheck`（MINOR / BUG，1 条）
- `vue/views/manage/Film.vue:205`

### `java:S6548`（INFO / CODE_SMELL，1 条）
- `common/OrderStatusCriteria.java:28`

### 安全热点（2 条，未计入问题数）
- `javascript:S5852` `vue/views/front/BuyTicket.vue:373`
- `javascript:S5852` `vue/views/front/BuyTicket.vue:488`

> 两条均为无锚点正则的 O(n²) 理论风险；输入是服务端生成的座位标签，长度受控，无 DoS 面。已在人工复核中判定为安全。
