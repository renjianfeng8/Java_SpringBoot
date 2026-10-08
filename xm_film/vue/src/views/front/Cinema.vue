<template>
  <div class="page-narrow">
    <!-- 影院列表：整卡是 router-link，可直接进入影院详情（此前整页没有任何跳转入口） -->
    <div class="cinema-list">
      <router-link
          v-for="cinema in data.filmData"
          :key="cinema.id"
          class="cinema-card"
          :to="`/front/cinemaDetail/${cinema.id}`"
      >
        <div class="cinema-card__poster">
          <img :src="cinema.avatar" :alt="`${cinema.name || '影院'} 图片`" class="cinema-card__img">
        </div>

        <div class="cinema-card__info">
          <h3 class="cinema-card__name">{{ cinema.name }}</h3>

          <!-- 影院服务标签：用功能色，白字压其上均达 AA -->
          <div class="cinema-card__tags">
            <span class="service-tag service-tag--refund">退票无忧</span>
            <span class="service-tag service-tag--promo">儿童优惠</span>
            <span class="service-tag service-tag--wifi">WiFi覆盖</span>
          </div>

          <div class="cinema-card__detail">
            <p class="cinema-card__detail-row">
              <span class="cinema-card__detail-label">电话:</span>{{ cinema.phone }}
            </p>
            <p class="cinema-card__detail-row">
              <span class="cinema-card__detail-label">邮箱:</span>{{ cinema.email }}
            </p>
            <p class="cinema-card__detail-row">
              <span class="cinema-card__detail-label">地址:</span>{{ cinema.address }}
            </p>
          </div>
        </div>
      </router-link>
    </div>

    <div v-if="data.error" class="empty-hint">数据加载失败，请稍后重试</div>
    <div v-else-if="!data.filmData.length" class="empty-hint">暂无数据</div>

    <div v-if="data.total" class="cinema-list__pagination">
      <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :page-sizes="[5, 10, 15]"
          background
          layout="total, prev, pager, next"
          :total="data.total"
      />
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue';
import request from "@/utils/request.js";
import { ElMessage } from 'element-plus';
import { API_PATHS, apiPage } from '@/constants';

const data = reactive({
  pageNum: 1,
  pageSize: 12,
  total: 0,
  filmData: [], // 影院数据
  error: false
})

const load = () => {
  data.error = false
  request.get(apiPage(API_PATHS.CINEMAS), {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
    }
  }).then(res => {
    if (res.code === '200') {
      data.filmData = res.data.list
      data.total = res.data.total
    } else {
      // 业务码非 200 不经过响应拦截器（它只管网络/超时/HTTP 状态），故这里补一次提示
      ElMessage.error(res.msg)
      data.error = true
    }
  }).catch(err => {
    // 网络与 5xx 那类失败的提示由 request.js 的响应拦截器给出，这里只落错误态，
    // 否则同一次失败会弹两次
    console.error(err)
    data.error = true
  })
}

const handleSizeChange = (newSize) => {
  data.pageSize = newSize;
  data.pageNum = 1; // 重置为第一页
  load();
}

const handleCurrentChange = (newPage) => {
  data.pageNum = newPage;
  load();
};

load()
</script>

<style scoped>
.cinema-list {
  padding: var(--space-16) 0;
}

.cinema-card {
  display: flex;
  margin-bottom: var(--space-16);
  padding: var(--space-16);
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  color: inherit;
  text-decoration: none;
  transition: transform 200ms ease-in-out, box-shadow 200ms ease-in-out;
}

.cinema-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--el-box-shadow-lighter);
}

.cinema-card__poster {
  flex-shrink: 0;
  width: 200px;
}

.cinema-card__img {
  display: block;
  width: 100%;
  aspect-ratio: 10 / 7;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
  object-fit: cover;
}

.cinema-card__info {
  flex: 1;
  min-width: 0;
  margin-left: var(--space-16);
}

.cinema-card__name {
  margin: 0;
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

.cinema-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-8);
  margin-top: var(--space-12);
}

/* 底色由共享层的 .service-tag--* 提供（与影院详情页同色），这里只管形状 */
.service-tag {
  padding: var(--space-4) var(--space-12);
  border-radius: var(--el-border-radius-base);
  font-size: var(--fs-xs);
  color: var(--color-on-accent);
}

.cinema-card__detail {
  margin-top: var(--space-12);
  color: var(--el-text-color-regular);
}

.cinema-card__detail-row {
  display: flex;
  align-items: center;
  margin: var(--space-8) 0;
}

/* 标签文字承载内容，用主色（5.58:1）而非品牌红（3.81:1） */
.cinema-card__detail-label {
  margin-right: var(--space-8);
  color: var(--el-color-primary);
}

.cinema-list__pagination {
  margin-top: var(--space-16);
}
</style>
