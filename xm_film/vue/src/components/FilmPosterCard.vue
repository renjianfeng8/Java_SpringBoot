<!--
  影片海报卡（前台共享）。

  首页「正在热播 / 即将上映」与影片列表页的卡片是同一个形状：2:3 海报 + 片名 + 各自一行元信息。
  此前三处各写一份 markup，海报高度已漂移成 260px / 240px 两档，且没有破图兜底。
  元信息差异较大（购票按钮 / 上映日期 / 状态标签 + 评分），所以只把「海报 + 片名」这一层
  收进本组件，元信息留给默认插槽由各页自己写 —— 只抽象真正同形的部分。

  海报与片名整体是 router-link（渲染成原生 <a>，天然可 Tab 聚焦、可回车触发），
  因此元信息插槽必须放在 link 之外：<a> 内部不能再嵌 button 等交互内容。

  父组件用法：
    <FilmPosterCard :film="item">
      <el-button ...>购票</el-button>
    </FilmPosterCard>
-->
<template>
  <article class="poster-card">
    <router-link :to="`/front/filmDetail/${film.id}`" class="poster-card__link">
      <div class="poster-card__poster">
        <img
            v-if="!posterFailed"
            :src="film.img"
            :alt="`《${film.title}》海报`"
            class="poster-card__img"
            loading="lazy"
            @error="posterFailed = true"
        >
        <!-- 破图兜底取片名首字，与该项目的头像兜底同一手法（front-pages.scss .mark-item__avatar） -->
        <div v-else class="poster-card__fallback" aria-hidden="true">{{ firstChar }}</div>

        <!-- 评分徽章压在图上：--color-rating 可用于「图片叠加」，
             垫一层 --overlay-mask 使任意海报底色下都可读 -->
        <span v-if="scoreText" class="poster-card__score">{{ scoreText }}</span>
      </div>
      <div class="poster-card__title" :title="film.title">{{ film.title }}</div>
    </router-link>

    <div v-if="$slots.default" class="poster-card__meta">
      <slot />
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue';
import { formatScoreBadge } from '@/utils/format.js';

const props = defineProps({
  film: { type: Object, required: true },
});

const posterFailed = ref(false);
const firstChar = computed(() => props.film.title?.charAt(0) || '影');

// 无评价时为空串，据此不渲染徽章
const scoreText = computed(() => formatScoreBadge(props.film.score));
</script>

<style scoped>
.poster-card {
  display: flex;
  flex-direction: column;
}

.poster-card__link {
  display: block;
  color: inherit;
  text-decoration: none;
}

.poster-card__poster {
  position: relative;
  /* 海报比例的唯一来源。此前两页各自写死 height（260 / 240px），换图源就变形 */
  aspect-ratio: 2 / 3;
  overflow: hidden;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
  transition: transform 200ms ease-in-out, box-shadow 200ms ease-in-out;
}

/* 卡片 hover 取 Shadow 1（静态卡片无阴影，hover 浮起用 lighter） */
.poster-card:hover .poster-card__poster {
  transform: translateY(-4px);
  box-shadow: var(--el-box-shadow-lighter);
}

.poster-card__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.poster-card__fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: var(--fs-2xl);
  color: var(--el-text-color-regular);
}

.poster-card__score {
  position: absolute;
  left: var(--space-8);
  bottom: var(--space-8);
  padding: var(--space-4) var(--space-8);
  border-radius: var(--el-border-radius-small);
  background-color: var(--overlay-mask);
  color: var(--color-rating);
  font-size: var(--fs-xs);
  /* 纯数字，可用 500 */
  font-weight: var(--fw-medium);
  line-height: var(--lh-loose);
}

.poster-card__title {
  margin-top: var(--space-8);
  font-size: var(--fs-base);
  /* 片名可能含中文，只允许 400 / 700 */
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
  /* 单行截断：长片名不撑破网格，完整名走 title 属性 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.poster-card__meta {
  margin-top: var(--space-4);
}
</style>
