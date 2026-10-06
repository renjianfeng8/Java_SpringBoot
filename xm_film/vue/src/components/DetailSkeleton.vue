<!--
  详情页首屏骨架（影片详情 / 选择影院 / 影院详情共用）。

  形状与真实页头一致（海报 + 若干文字行），数据到位时不会跳高。
  样式在 front-pages.scss 的 .detail-skeleton 一组里，本组件只承载标记 ——
  三处此前各自复制了同一段 7 行标记，改一次要动三个文件。

  纯装饰，故整块 aria-hidden；灰块不是数据，不违反「禁止用假数据填充」（规范 §11.2）。
-->
<template>
  <div class="detail-skeleton" aria-hidden="true">
    <div class="detail-skeleton__poster"></div>
    <div class="detail-skeleton__lines">
      <div class="detail-skeleton__line detail-skeleton__line--title"></div>
      <div class="detail-skeleton__line"></div>
      <div class="detail-skeleton__line"></div>
      <div class="detail-skeleton__line detail-skeleton__line--short"></div>
    </div>
  </div>
</template>
