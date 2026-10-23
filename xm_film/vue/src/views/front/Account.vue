<!--
  我的账户：余额 + 充值申请 + 充值单据 + 余额走势 + 资金流水。

  充值流程刻意分成两步，与真实支付网关一致：
    1. 提交充值申请 → 生成「处理中」单据，余额不变
    2. 触发模拟支付回调（正式环境由支付平台异步回调）→ 成功才入账

  排版取控制台口径：页头一行放标题与余额大字、下面压一条发丝线收口，
  分区卡片扁平化（1px 淡边框替阴影）、表头灰底弱化小字、去斑马纹改行 hover。
  不借的两处：统计块与图表内容是控制台页的主体，但本页数据源只有分页列表，
  拿一页求和当累计值是错数，故只做从账本可直接重建的余额走势图。
-->
<template>
  <div class="account-page">
    <!-- 余额是这一页的主角，放在页头右侧的大字槽位，不再单独占一张卡 ——
         那样同一个数会在页面上出现两次。 -->
    <header class="account-masthead">
      <div class="account-masthead__inner measure-inline--wide">
        <h1 class="account-masthead__title">我的账户</h1>

        <span class="account-masthead__amount">
          <span class="account-masthead__unit">¥</span>{{ money(balance) }}
        </span>
      </div>
    </header>

    <div class="page-wide">
      <section class="card page-card account-section">
        <div class="section-head">
          <h2 class="section-head__title">账户充值</h2>
        </div>

        <!-- 快捷档位与自由输入并存，二者都受后端金额校验约束。
             复用「全部 / 城市 / 区域」那套 .filter-chip：选中态是填充 + 加粗，
             不只靠颜色传递状态；aria-pressed 让读屏也知道当前选中哪一档。 -->
        <fieldset class="filter-options tier-options" aria-label="快捷充值金额">
          <button
              v-for="tier in TIERS"
              :key="tier"
              type="button"
              class="filter-chip"
              :class="{ 'filter-chip--active': Number(amount) === tier }"
              :aria-pressed="Number(amount) === tier"
              @click="amount = tier"
          >{{ tier }} 元</button>
        </fieldset>

        <div class="amount-row">
          <span class="amount-label">充值金额</span>
          <!-- 旁边的「充值金额」是普通 span，与控件没有 for/aria-labelledby 关联，故补 aria-label -->
          <el-input-number v-model="amount"
                           aria-label="充值金额"
                           :min="0.01"
                           :max="50000"
                           :precision="2"
                           :step="10"
                           class="amount-input" />
          <el-button type="primary" :loading="submitting" @click="submitRecharge">
            提交充值申请
          </el-button>
        </div>
      </section>

      <section class="card page-card account-section">
        <div class="section-head">
          <h2 class="section-head__title">我的充值单据</h2>
        </div>

        <!-- 列宽配比见下方「资金流水」表头前的说明。本表与资金流水表同一套对齐口径：
             每列 align 与 header-align 都取 center，同一列的表头与内容必然同侧。
             金额 / 状态 / 操作三列定宽保留 —— 富余宽度全给单号与时间戳这类长文本列，
             它们比数值列更需要宽度。 -->
        <el-table v-loading="loadingRecharges" :data="recharges">
          <el-table-column label="充值单号" prop="rechargeNo" min-width="170" show-overflow-tooltip
                           align="center" header-align="center" />
          <el-table-column label="金额（元）" width="110" align="center" header-align="center">
            <template #default="scope">¥{{ money(scope.row.amount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center" header-align="center">
            <template #default="scope">
              <el-tag :type="getRechargeStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" prop="createTime" min-width="120" show-overflow-tooltip
                           align="center" header-align="center" />
          <el-table-column label="完成时间" prop="finishTime" min-width="120" show-overflow-tooltip
                           align="center" header-align="center" />
          <el-table-column label="备注" prop="remark" min-width="110" show-overflow-tooltip
                           align="center" header-align="center" />
          <el-table-column label="模拟支付回调" width="170" align="center" header-align="center">
            <template #default="scope">
              <!-- 只有「处理中」单据可被回调；已处理单据后端也会拒绝重复回调。
                   这是演示控件而非业务动作，故取常规文字色（6.11:1）压到次要位置、
                   hover 才转主色 —— 原用 success / danger 是把「状态色」当「动作色」使。 -->
              <div v-if="scope.row.status === '处理中'" class="row-actions">
                <button class="callback-action" type="button" @click="triggerCallback(scope.row, true)">
                  <el-icon><CircleCheck /></el-icon>模拟成功
                </button>
                <button class="callback-action" type="button" @click="triggerCallback(scope.row, false)">
                  <el-icon><CircleClose /></el-icon>模拟失败
                </button>
              </div>
              <span v-else class="muted">已处理</span>
            </template>
          </el-table-column>

          <!-- 失败与空态文案不同：请求失败不该说成「确实没有充值记录」 -->
          <template #empty>
            <p class="table-empty">{{ rechargeError ? '数据加载失败，请稍后重试' : '暂无充值记录' }}</p>
          </template>
        </el-table>

        <!-- total 为 0 时不出分页器，空表下方不留一条孤零零的「共 0 条」 -->
        <div v-if="rechargePage.total" class="table-pagination">
          <el-pagination
              v-model:current-page="rechargePage.pageNum"
              background
              layout="total, prev, pager, next"
              :total="rechargePage.total"
              :disabled="loadingRecharges"
              @current-change="loadRecharges"
          />
        </div>
      </section>

      <!-- 走势图由资金流水重建，故紧贴它的数据源、放在流水表上方当概述。
           无流水时整块不渲染 —— 空图表框没有意义，下方流水表已负责报空态与失败态。 -->
      <section v-if="trend" class="card page-card account-section">
        <div class="section-head">
          <h2 class="section-head__title">余额走势</h2>
        </div>

        <div class="trend">
          <!-- 折线对读屏用户不可达；结论已由下方 caption 完整给出，
               故图表本身是装饰，不再用 role="img" 重复朗读一遍 -->
          <svg class="trend__svg"
               :viewBox="`0 0 ${TREND_VIEW.w} ${TREND_VIEW.h}`"
               aria-hidden="true">
            <path class="trend__line" :d="trend.path" />
          </svg>
          <p class="trend__caption">{{ trend.caption }}</p>
        </div>
      </section>

      <section class="card page-card account-section">
        <div class="section-head">
          <h2 class="section-head__title">资金流水</h2>
        </div>

        <!-- 列宽配比（两表同一套口径）—— EP 的算法是：
               显式 width 的列定宽；**没有 width 的列一律算 flex 列**，富余宽度按各列 min-width
               的比例分摊（table/src/table-layout.mjs 的 updateColumnsWidth）。
               所以这里的 min-width 表达的是「配比」，不是「下限」：
                 充值单据表：金额 / 状态 / 回调 三列定宽，其余给 min-width；
                 资金流水表：全列 min-width，无定宽列。
               只有一列是 flex 列时它会独吞全部富余 —— 本表原先只有「发生时间」没有 width，
               于是它一列涨到 550px，其余列全被挤到右侧、左边空出一大片，正是这个原因。
               约束：各表 min-width 与 width 之和必须不超过最窄情形的内容宽（视口 1120px → 页宽 85%
               → 扣掉卡片 1px 边框与 24px 内边距后约 902px），超出会让表内出现横向滚动条。
               本表合计 170 + 90 + 120 + 140 + 140 = 660，充值单据表 380 + 520 = 900，
               两者都在 902 以内。 -->
        <!-- 列序：标识在前（发生时间 / 业务来源 / 关联单据ID）、数值在后。
             数值只留两列：变动金额 + 变动后余额。
             变动前余额不展示 —— 它与「往下数一行」的变动后余额恒为同值（账本要求每笔余额
             变动都归入一个来源，见 FundSource，故流水连续完整），两列其实是同一串数字错开
             一行，属重复展示；银行流水也一律只留「发生额 + 余额」。
             数据层的 balanceBefore 保留不动，走势图的起点仍在用它。
             一行读下来收在「这笔之后余额是多少」，故变动后余额留在最后一列。 -->
        <el-table v-loading="loadingFlows" :data="flows">
          <el-table-column label="发生时间" prop="createTime" min-width="170" show-overflow-tooltip
                           align="center" header-align="center" />
          <el-table-column label="业务来源" prop="source" min-width="90" align="center" header-align="center" />
          <el-table-column label="关联单据ID" prop="relatedId" min-width="120" align="center" header-align="center" />
          <el-table-column label="变动金额" min-width="140" align="center" header-align="center">
            <template #default="scope">
              <!-- 加粗 + 出入账配色：它是全行唯一的「动作」，右边紧跟变动后的余额 -->
              <span class="flow-amount"
                    :class="Number(scope.row.changeAmount) >= 0 ? 'flow-amount--in' : 'flow-amount--out'">
                {{ Number(scope.row.changeAmount) >= 0 ? '+' : '' }}{{ money(scope.row.changeAmount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="变动后余额" min-width="140" align="center" header-align="center">
            <template #default="scope">¥{{ money(scope.row.balanceAfter) }}</template>
          </el-table-column>

          <template #empty>
            <p class="table-empty">{{ flowError ? '数据加载失败，请稍后重试' : '暂无资金流水' }}</p>
          </template>
        </el-table>

        <div v-if="flowPage.total" class="table-pagination">
          <el-pagination
              v-model:current-page="flowPage.pageNum"
              background
              layout="total, prev, pager, next"
              :total="flowPage.total"
              :disabled="loadingFlows"
              @current-change="loadFlows"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { isSuccess, ACCOUNT_API, FUND_FLOW_API, RECHARGE_API, getRechargeStatusType } from '@/constants';
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { CircleCheck, CircleClose } from '@element-plus/icons-vue';
import request from '@/utils/request.js';

/** 快捷档位；与自由输入并存，二者都受后端金额校验约束 */
const TIERS = [20, 30, 50, 100];

/**
 * 走势图的取样窗口。表格每页只有 10 条，画不出趋势，故按上限另取一页流水。
 * 每行都带 balanceBefore / balanceAfter / createTime（FundFlowView），
 * 所以曲线是从账本直接重建的，不是估算 —— 窗口就是图上注明的「最近 N 笔」。
 */
const TREND_WINDOW = 100;
/** 折线的 viewBox 尺寸。实际宽度交给 CSS 等比缩放，端点与线宽因此不会变形。 */
const TREND_VIEW = { w: 1000, h: 180 };
const TREND_PAD_Y = 12;

const balance = ref(0);
const recharges = ref([]);
const flows = ref([]);
const trends = ref([]);
const amount = ref(50);
const submitting = ref(false);
const loadingRecharges = ref(false);
const loadingFlows = ref(false);
// 回调进行中的单一守卫：按钮是自绘的，没有 EP 的 disabled 通道，
// 双击会发两次 POST，第二次必被后端拒绝并弹一条无谓的失败提示
const callbacking = ref(false);
// 失败态与「确实没有记录」分开：空表可能是一次请求失败，而非真的没有资金凭证
const rechargeError = ref(false);
const flowError = ref(false);

// 两表各自的分页状态。此前两处都硬编码 pageNum:1 / pageSize:50，累计超过 50 条后
// 更早的记录在页面上再无入口 —— 对账场景下资金凭证被截断。
// 后端两个 /page 均按 id DESC 返回、且已按当前用户归属过滤，故新记录必然落在第 1 页。
const rechargePage = reactive({ pageNum: 1, pageSize: 10, total: 0 });
const flowPage = reactive({ pageNum: 1, pageSize: 10, total: 0 });

// 递增序号：只让最后一次 load 的结果落地。翻页请求与写操作后的重载会重叠，
// 慢响应后到会覆盖新页的数据与 loading（useCrud.js 同款守卫）。
let rechargeSeq = 0;
let flowSeq = 0;
let trendSeq = 0;

const money = (value) => Number(value ?? 0).toFixed(2);

const loadSummary = async () => {
  try {
    const res = await request.get(ACCOUNT_API.SUMMARY);
    if (isSuccess(res)) {
      balance.value = res.data?.balance ?? 0;
    } else {
      // 与非 200 的业务码同样补一次提示，否则余额会静默停在旧值上
      ElMessage.error(res.msg || '余额加载失败');
    }
  } catch (error) {
    // request.js 已统一提示
  }
};

const loadRecharges = async () => {
  const seq = ++rechargeSeq;
  loadingRecharges.value = true;
  rechargeError.value = false;
  try {
    const res = await request.get(RECHARGE_API.PAGE, {
      params: { pageNum: rechargePage.pageNum, pageSize: rechargePage.pageSize },
    });
    if (seq !== rechargeSeq) return;
    if (isSuccess(res)) {
      recharges.value = res.data?.list || [];
      rechargePage.total = res.data?.total ?? 0;
    } else {
      // 业务码非 200 不经过响应拦截器（它只管网络 / 超时 / HTTP 状态），故这里补一次提示
      ElMessage.error(res.msg || '充值单据加载失败');
      rechargeError.value = true;
      recharges.value = [];
      rechargePage.total = 0;
    }
  } catch (error) {
    if (seq !== rechargeSeq) return;
    // 网络与 5xx 那类失败由 request.js 提示，这里只落错误态，否则同一次失败弹两次
    rechargeError.value = true;
    // 清掉上一次的结果，否则失败文案下面还挂着旧页的数据
    recharges.value = [];
    rechargePage.total = 0;
  } finally {
    if (seq === rechargeSeq) loadingRecharges.value = false;
  }
};

const loadFlows = async () => {
  const seq = ++flowSeq;
  loadingFlows.value = true;
  flowError.value = false;
  try {
    const res = await request.get(FUND_FLOW_API.PAGE, {
      params: { pageNum: flowPage.pageNum, pageSize: flowPage.pageSize },
    });
    if (seq !== flowSeq) return;
    if (isSuccess(res)) {
      flows.value = res.data?.list || [];
      flowPage.total = res.data?.total ?? 0;
    } else {
      ElMessage.error(res.msg || '资金流水加载失败');
      flowError.value = true;
      flows.value = [];
      flowPage.total = 0;
    }
  } catch (error) {
    if (seq !== flowSeq) return;
    flowError.value = true;
    flows.value = [];
    flowPage.total = 0;
  } finally {
    if (seq === flowSeq) loadingFlows.value = false;
  }
};

const loadTrend = async () => {
  const seq = ++trendSeq;
  try {
    const res = await request.get(FUND_FLOW_API.PAGE, {
      params: { pageNum: 1, pageSize: TREND_WINDOW },
    });
    if (seq !== trendSeq) return;
    // 后端按 id DESC 返回（新的在前），折线要从旧画到新
    trends.value = isSuccess(res) ? (res.data?.list || []).reverse() : [];
  } catch (error) {
    if (seq !== trendSeq) return;
    // 走势图是补充信息，取不到就不出这一块；同一批流水在下方流水表里仍会报失败态
    trends.value = [];
  }
};

const loadAll = async () => {
  await Promise.all([loadSummary(), loadRecharges(), loadTrend(), loadFlows()]);
};

/** 折线几何：在固定 viewBox 内算好点位，渲染尺寸交给 CSS */
const trend = computed(() => {
  const rows = trends.value;
  if (!rows.length) {
    return null;
  }

  // 起点取最早一笔的「变动前余额」，其后每笔取「变动后余额」——
  // 这样单笔流水也有两个点（只有一个点时 polyline 什么都不画）
  const values = [
    Number(rows[0].balanceBefore ?? 0),
    ...rows.map((row) => Number(row.balanceAfter ?? 0)),
  ];
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min;
  const innerHeight = TREND_VIEW.h - TREND_PAD_Y * 2;

  const points = values.map((value, index) => ({
    x: (index / (values.length - 1)) * TREND_VIEW.w,
    // 余额一位不动时 span 为 0，除零会算出 NaN，故直接落在中线上
    y: span === 0
        ? TREND_VIEW.h / 2
        : TREND_PAD_Y + (1 - (value - min) / span) * innerHeight,
  }));

  const count = rows.length;
  const from = rows[0].createTime;
  const to = rows[count - 1].createTime;

  return {
    path: points
        .map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(2)} ${point.y.toFixed(2)}`)
        .join(' '),
    caption: `最近 ${count} 笔流水 · ${from} → ${to} · 区间 ¥${money(min)} – ¥${money(max)}`,
  };
});

// 提交充值 / 触发回调后回到第 1 页：新单据与它产生的流水都按 id DESC 排在最前，
// 提交完仍停在第 3 页重载，会让用户以为「点了没反应」
const reloadFromFirstPage = () => {
  rechargePage.pageNum = 1;
  flowPage.pageNum = 1;
  return loadAll();
};

const submitRecharge = async () => {
  if (submitting.value) {
    return;
  }
  const value = Number(amount.value);
  if (!value || value <= 0) {
    ElMessage.warning('请输入大于 0 的充值金额');
    return;
  }
  submitting.value = true;
  try {
    const res = await request.post(RECHARGE_API.CREATE, { amount: value });
    if (isSuccess(res)) {
      ElMessage.success('充值申请已提交，等待支付回调入账');
      await reloadFromFirstPage();
    } else {
      ElMessage.error(res.msg || '提交失败');
    }
  } catch (error) {
    // request.js 已统一提示
  } finally {
    submitting.value = false;
  }
};

/** 演示环境手动触发回调；正式环境由支付网关调用同一端点 */
const triggerCallback = async (order, success) => {
  if (callbacking.value) {
    return;
  }
  callbacking.value = true;
  try {
    const res = await request.post(RECHARGE_API.CALLBACK(order.id), {
      success,
      remark: success ? '' : '模拟支付回调失败',
    });
    if (isSuccess(res)) {
      ElMessage.success(success ? '回调成功，充值已入账' : '回调失败，单据已置为已失败');
      await reloadFromFirstPage();
    } else {
      ElMessage.error(res.msg || '回调处理失败');
    }
  } catch (error) {
    // request.js 已统一提示
  } finally {
    callbacking.value = false;
  }
};

onMounted(loadAll);
</script>

<style scoped>
/* 全页一条竖线：页头文字、每个分区标题、表格首末列都落在同一个水平内缩上。
   卡片内容自「1px 边框 + --space-24 内边距」处起，这个值就是全页基准内缩。 */
.account-page {
  --page-inset: calc(1px + var(--space-24));
  /* 表格行高：表头与表体是两个独立 <table>，行高各算各的，必须钉成同一值 */
  --table-row-height: 44px;
}

/* ---------- 页头 ---------- */
/* 页头内层与下方分区同一测度：测度在 front-pages.scss 的 .measure-inline--wide，
   这里只补布局与内边距（.page-wide 自带纵向 margin，页头里不能用）。 */
.account-masthead__inner {
  position: relative;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-24);
  padding: var(--space-32) var(--page-inset) var(--space-20);
}

/* 发丝线用伪元素而不是 border-bottom：border 连着左右内边距一起铺，
   会比下方每条 .section-head 的通栏线各宽 25px。改成按同一内缩收进来之后，
   页头这条线与三条分区线同宽同起点 —— 横线全页只有一种规格。 */
.account-masthead__inner::after {
  content: '';
  position: absolute;
  left: var(--page-inset);
  right: var(--page-inset);
  bottom: 0;
  height: 1px;
  background-color: var(--el-border-color-lighter);
}

/* line-height: 1 让标题盒高等于字高，与同样 line-height: 1 的余额大字
   共用同一条基线 —— 否则标题盒底下多出的半行距会把它顶高约 7px */
.account-masthead__title {
  margin: 0;
  font-size: var(--fs-3xl);
  font-weight: var(--fw-bold);
  line-height: 1;
  color: var(--el-text-color-primary);
}

/* 余额用 --color-brand（白底 3.81:1）—— 这个档位只够「大文本」：
   36px 粗体在 WCAG 里算大文本（阈值 3:1），故达 AA。
   字号若降到 24px 以下就不再是大文本，必须换 --el-color-primary（5.58:1） */
.account-masthead__amount {
  display: flex;
  flex-shrink: 0;
  align-items: baseline;
  gap: var(--space-4);
  font-size: var(--fs-5xl);
  font-weight: var(--fw-bold);
  line-height: 1;
  color: var(--color-brand);
}

/* ¥ 只有 18px，不属大文本，故不能跟着用品牌红（3.81:1 < 4.5:1），
   改取常规文字色（6.11:1）。顺带把货币符号压到数字之后，视线落在数字上。 */
.account-masthead__unit {
  font-size: var(--fs-lg);
  font-weight: var(--fw-regular);
  color: var(--el-text-color-regular);
}

/* ---------- 分区 ---------- */
/* 控制台口径：卡片靠 1px 淡边框成形，不用阴影 —— 阴影让它看着浮起，
   而这页是一叠并列的面板。间距也从 .page-card 的 --space-8 提到 --space-16。 */
.account-section {
  margin-bottom: var(--space-16);
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: none;
}

/* 表头样式（灰底 + 常规文字色 + 700 字重）已提到 global.css，全站统一（FE-63），
   这里只保留本页特有的表格间距：给分区标题的通栏线留出呼吸，
   EP 的表头自带灰底，紧贴那条线会连成两道横杠。 */
.account-section :deep(.el-table) {
  margin-top: var(--space-16);
}

/* 表头与表体的垂直口径钉成同一组值。EP 把表头与表体渲染成两个独立的 <table>
   （.el-table__header 与 .el-table__body，且 border-collapse: separate），
   两边行高各算各的 —— 只要两侧内容盒有任何差别，就会出现表头偏上、单元格偏下这种
   表头与内容对不齐的观感。行高给同一个值、垂直对齐都取 middle，两侧就不可能再错开。
   顺带把两表行高统一：充值单据表因 el-tag 原本比资金流水表高 1px。 */
.account-section :deep(.el-table th.el-table__cell),
.account-section :deep(.el-table td.el-table__cell) {
  height: var(--table-row-height);
  vertical-align: middle;
}

/* 两表全列居中，故一律保留 EP 每个单元格那 12px 水平内边距（.el-table .cell 的
   padding: 0 12px）—— 它左右对称，居中才不会偏。若像早先那样为对齐分区标题而只消掉
   首列的左侧内边距，该列内容就会偏离列心 6px（其余列仍是真居中）。
   另注：给某列挂钩不要走 :last-child，改挂列自身的 class-name / label-class-name ——
   th / td 由 EP 自己渲染，行末有没有多出的单元格由库决定，按 nth 位置猜不可靠。
   （曾以为 EP 恒定渲染一个补滚动条宽度的 gutter 单元格，该断言在 element-plus 2.14.2
   上不成立 —— 见「前端表格设计规范.md」§3。） */

/* 操作列的按钮组是 flex 容器，text-align 管不到它的主轴分布：
   列的 align="center" 只让 .cell 里的行内内容居中，两个按钮仍会贴左。
   故这里补 justify-content，否则该列的表头居中、按钮却在左边。 */
.account-section :deep(.row-actions) {
  justify-content: center;
}

.tier-options {
  margin-top: var(--space-16);
}

.amount-row {
  display: flex;
  align-items: center;
  gap: var(--space-12);
  margin-top: var(--space-16);
}

.amount-input {
  width: 180px;
}

.amount-label {
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

/* 回调是演示控件：常规文字色（6.11:1）→ hover 主色（5.58:1），两态都达 AA */
.callback-action {
  display: inline-flex;
  align-items: center;
  gap: var(--space-4);
  padding: 0;
  border: none;
  background: none;
  font-family: inherit;
  font-size: var(--fs-sm);
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: color 100ms ease-out;
}

.callback-action:hover {
  color: var(--el-color-primary);
}

.callback-action:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
  border-radius: var(--el-border-radius-base);
}

/* 表格空态。字体规格对齐 EP 默认空文案，只把颜色换成常规文字色（6.11:1） */
.table-empty {
  margin: 0;
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

.table-pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--space-20);
}

.muted {
  font-size: var(--fs-sm);
  color: var(--el-text-color-regular);
}

/* ---------- 余额走势 ---------- */
.trend {
  margin-top: var(--space-16);
}

/* viewBox 固定、宽高随容器等比缩放：线和端点因此不会横向拉伸变形
   （即不需要 preserveAspectRatio="none" + non-scaling-stroke 那套）。
   代价是高度随视口变：1200px 宽时约 216px，800px 时约 144px。 */
.trend__svg {
  display: block;
  width: 100%;
  height: auto;
}

/* 折线是图形，只需 ≥3:1，品牌红白底 3.81:1 达标 */
.trend__line {
  fill: none;
  stroke: var(--color-brand);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.trend__caption {
  margin: var(--space-12) 0 0;
  font-size: var(--fs-sm);
  color: var(--el-text-color-regular);
}

/* 流水金额承载文字，用达 AA 的成功 / 危险色 —— 前台主题已把 EP 的
 * success #67c23a / danger #f56c6c 覆盖为 #007E1D（5.25:1）/ #C43232（5.46:1），
 * 见 assets/css/index.scss。加粗使一行里视线先落在变动额上。 */
.flow-amount {
  font-weight: var(--fw-bold);
}

.flow-amount--in {
  color: var(--el-color-success);
}

.flow-amount--out {
  color: var(--el-color-danger);
}
</style>
