# 前端表格设计规范

> 2026-10-18：本文件原为仓库根的 `前端表格设计规范.md`，已移入 `audit/` 并改名 `table-spec.md`（`git mv` 保留历史）。内容未改。

el-table 表格的列宽、对齐、表头与配套口径。改动任何表格前先读本文。

本文只写标准，不含实现进度与待迁移清单。参考实现：xm_film/vue/src/views/front/Account.vue。

## 1 适用范围

适用于前台消费端的列表表格。后台管理端与影院端表格不打算按本规范迁移（只在改动对应页面时顺手对齐）。

## 2 列宽

EP 按列是否声明 width 把列分成两类，宽度分配规则完全不同（element-plus/es/components/table/src/table-layout.mjs 的 updateColumnsWidth）。

| 列类型 | 声明方式 | 宽度 |
| --- | --- | --- |
| 定宽列 | 给出 width | 恒为该值 |
| 配比列 | 只给 min-width，不给 width | 有富余时按各配比列 min-width 的比例分摊富余；无富余时等于自己的 min-width |

三条硬约束：

1. 配比列不得只剩一个。只剩一个时该列独吞全部富余（该分支公式为 minWidth + 全部富余），会把其余列挤到一侧、另一侧空出一大片。配比列至少两个。
2. 各配比列的 min-width 与各定宽列的 width 之和，不得超过表格在最窄情形下的内容宽 902px（推算见下）。超出会使表格自己长出横向滚动条。
3. 字符串写法不影响判断，width="110" 会被 EP 的 parseWidth 归一为数字。

902px 的推算：前台外壳的 front-header 设 min-width 1120px，页面最窄按视口 1120px 算；内容区 .page-wide 取 85% 得 952px；减去卡片 1px 边框两侧与 24px 内边距两侧共 50px，得 902px。视口窄于 1120px 时页面本身已横向滚动，表格随之收窄并可能内部滚动，属既有边界而非本规范涵盖范围。

分配原则：定宽给内容长度固定的列，配比给长度可变的列。这样富余宽度流向真正需要它的列（单号、时间戳、备注），而不是被均分掉。

EP 没有列宽上限（无 max-width）。列一旦参与配比就随容器变宽，不要指望用配比把某列压住。

## 3 横向对齐

每一列必须同时给出 align 与 header-align，且取值相同。

EP 把它俩分别打成单元格上的 is-center / is-right 类（table-column/render-helper.mjs:20），align 管内容、header-align 管表头。只给其中一个，另一侧就回落到默认左对齐，同一列的表头与内容各朝一边。

本仓前台一律取 center：各列内容宽度不等（金额、时间戳），居中后不会把参差的右缘暴露出来。

单元格里放 flex 容器时（例如操作列的按钮组），列的 align 只作用于 .cell 内的行内内容，管不到 flex 的主轴分布，必须另给 justify-content: center。否则该列出现表头居中、按钮贴左的表头与内容错位。

列的定位不要用 :last-child，改走列自身的 class-name / label-class-name。EP 自己渲染 th / td，行末有没有多出的单元格由库决定，不该按 nth 位置猜。

（本节原据「EP 在每行行末恒定渲染一个 gutter 单元格」立论，该断言在 element-plus 2.14.2 上不成立，已按实测改正：整个 es/ 树里没有任何渲染路径产出 th.gutter / td.gutter / col[name=gutter]；table-layout.mjs 的 gutterWidth 初始化为 0 后全程不再赋值；layout-observer.mjs 里那几句 querySelectorAll 因而恒选空，其后的 display 赋值也从不生效。EP 已改用内部滚动条，gutter 机制是旧实现的残留。结论不变：挂钩走 class-name，理由改为「行结构由库决定」而非「行末必有 gutter 格」。）

## 4 竖向对齐

表头行与表体行必须钉成同一行高、同一垂直对齐。

EP 把表头与表体渲染成两个独立的 table（.el-table__header 与 .el-table__body，均 table-layout: fixed; border-collapse: separate），两边行高各算各的。不显式钉死，两侧内容盒一旦有差别就会出现表头偏上、单元格偏下的错位。

做法是给 th.el-table__cell 与 td.el-table__cell 同一个 height（页面级令牌，现值 44px）与 vertical-align: middle。顺带统一同一页里各表行高：行高不定死时，含 el-tag 的行会比纯文本行高出几像素。

EP 单元格自身的纵向内边距是 8px，无需改动。

## 5 表头样式

EP 表头的默认值有三处不合本仓要求，必须覆盖。

| 项目 | EP 默认 | 本仓取值 |
| --- | --- | --- |
| 底色 | --el-table-header-bg-color 取 --el-fill-color-blank，即纯白（表头与表体无分界） | --el-fill-color-light |
| 文字色 | --el-table-header-text-color 取 --el-text-color-secondary（#909399，白底 3.08:1，14px 常规字不达 AA） | --el-text-color-regular（#606266，白底 6.11:1） |
| 字重 | .el-table thead th 写死 600 | --fw-bold，即 700 |

600 与 tokens.scss 的「禁用 600」冲突：中文字体栈只有 400 与 700，写 600 会触发浏览器伪粗体。

底色与文字色走覆写 EP 的 CSS 变量（表头全部读这两个变量，改变量一处生效），字重没有变量，只能覆写 .el-table thead th。作用域挂在承载表格的容器类上，不写进全局样式。

不使用 stripe（斑马纹）。行区分靠 hover 高亮，EP 的 --el-table-row-hover-bg-color 默认即 --el-fill-color-light。

## 6 单元格内边距

保留 EP 的 .el-table .cell 那 12px 水平内边距，且必须左右对称。

只消掉单侧（例如为了让首列与页面左缘对齐而去掉 padding-left）会让该列内容偏离列心 6px，而其余列仍是真居中，这一列就不是。

## 7 列序

按「标识 → 数值 → 结论」排：

- 标识在前：发生时间、业务来源、单据号等用于定位这一行的字段
- 数值居中段，并按运算关系排：前值 → 变动 → 后值
- 结论在最后：这一行最终的落点，如变动后余额

判据：一行里若存在两个数值与一个由它们导出的差额，差额不得排在两个数值之前，否则从左到右读出来是 300 → 128 → 428 这种验不了算的顺序，读作 128 + 300 = 428 才成立。

删列只有一个理由：该列与另一列存在可证明的恒等关系。示例：资金流水表的变动前余额恒等于下一行的变动后余额（账本要求每笔余额变动都归入一个来源，故流水连续完整），属同一串数据错开一行。自由文本列（备注）与独立字段不因「看着重复」而删。

## 8 空态与失败态

表格必须给 #empty 槽，且请求失败与确实无数据必须用两句不同文案。

混用会把一次请求失败说成「确实没有记录」，这是本仓明确不接受的口径。失败时同时清空列表数据与 total，避免失败文案下面还挂着上一页的数据。

## 9 分页与加载

| 项 | 口径 |
| --- | --- |
| el-pagination | background；layout 含 total，即「共 N 条」 |
| disabled | 绑该表自己的 loading。重载期间旧分页器仍可点，会答出一次越界空请求 |
| total 为 0 | 不渲染分页器，免得空表下方挂一条「共 0 条」 |
| 加载态 | v-loading 绑各表自己的 loading 标志，同页两表不共用 |

写操作（提交、回调）成功后应把页码复位到第 1 页：新记录按 id DESC 排在最前，停在第 N 页重载会让用户以为没生效。

## 10 数值格式与配色

金额一律经 money() 输出，保留两位小数。

金额增减用 EP 的成功与危险色，但必须取本仓覆盖后的值：assets/css/index.scss 已把 EP 默认的 success #67c23a（白底 2.24:1）与 danger #f56c6c（2.90:1）覆盖为 #007E1D（5.25:1）与 #C43232（5.46:1）。按 EP 默认值写文字色不达 AA。

## 11 无障碍

承载内容的文字色必须达 AA，14px 常规字要求 4.5:1。

| 色 | 白底对比度 | 允许用途 |
| --- | --- | --- |
| --el-text-color-primary #303133 | 13.02:1 | 任意 |
| --el-text-color-regular #606266 | 6.11:1 | 任意 |
| --el-color-primary #BF352D | 5.58:1 | 任意 |
| --color-brand #ef4238 | 3.81:1 | 仅 ≥24px 大文本与图形 |
| --el-text-color-secondary #909399 | 3.08:1 | 不得用于文字 |

其余要求：

- 操作列若用按钮，按钮必须有可访问名。只放图标而无文字的按钮不可接受。
- 单元格内紧跟同义文字的图标属装饰，不需另设可访问名。
