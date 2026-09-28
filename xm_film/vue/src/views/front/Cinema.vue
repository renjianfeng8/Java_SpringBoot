<template>
  <div class="page-narrow">
    <!-- 影院列表 -->
    <div class="cinema-list">
      <!-- 循环渲染每个影院 -->
      <div v-for="(cinema, index) in data.filmData" :key="index" class="cinema-card">
        <!-- 左侧图片 -->
        <div class="cinema-card__poster">
          <img :src="cinema.avatar " alt="影院图片" class="cinema-card__img">
        </div>
        <!-- 右侧信息区域 -->
        <div class="cinema-card__info">
          <!-- 影院名称 -->
          <div class="cinema-card__name">{{ cinema.name }}</div>

          <!-- 影院服务标签：用功能色，白字压其上均达 AA（规范 §2.5） -->
          <div class="cinema-card__tags">
            <div class="service-tag service-tag--refund">
              退票无忧
            </div>
            <div class="service-tag service-tag--promo">
              儿童优惠
            </div>
            <div class="service-tag service-tag--wifi">
              WiFi覆盖
            </div>
            <div class="service-tag service-tag--parking">
              免费停车
            </div>
          </div>

          <!-- 详细信息 -->
          <div class="cinema-card__detail">

            <div class="cinema-card__detail-row">
              <div class="cinema-card__detail-label">电话:</div>
              <div>{{ cinema.phone }}</div>
            </div>

            <div class="cinema-card__detail-row">
              <div class="cinema-card__detail-label">邮箱:</div>
              <div>{{ cinema.email }}</div>
            </div>

            <div class="cinema-card__detail-row">
              <div class="cinema-card__detail-label">地址:</div>
              <div>{{ cinema.address }}</div>
            </div>
          </div>

        </div>

      </div>
    </div>

    <!-- 分页组件 -->
    <div class="cinema-list__pagination" v-if="data.total">
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
import {reactive} from 'vue';
import request from "@/utils/request.js";
import {ElMessage} from 'element-plus';
import { API_PATHS, apiPage } from '@/constants';

const data = reactive({
  cinemaData: null,
  pageNum: 1,
  pageSize: 12,
  total: 0,
  filmData: [], // 存储影院数据
  status: null
})

// 加载影院数据
const load = () => {
  request.get(apiPage(API_PATHS.CINEMAS), {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      cinemaId: data.cinemaData,
    }
  }).then(res => {
    if (res.code === '200') {
      data.filmData = res.data.list
      data.total = res.data.total
    } else {
      ElMessage.error(res.msg)
    }
  }).catch(err => {
    ElMessage.error('数据加载失败，请稍后重试')
    console.error(err)
  })
}

// 处理每页条数变化
const handleSizeChange = (newSize) => {
  data.pageSize = newSize;
  data.pageNum = 1; // 重置为第一页
  load();
}

// 处理页码变化
const handleCurrentChange = (newPage) => {
  data.pageNum = newPage;
  load();
};

// 初始加载数据
load()
</script>

<style scoped>
.cinema-list {
  padding: var(--space-16);
}

.cinema-card {
  display: flex;
  margin-bottom: var(--space-16);
  padding: var(--space-16);
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
}

.cinema-card__poster {
  flex-shrink: 0;
  width: 200px;
}

.cinema-card__img {
  width: 100%;
  height: 140px;
  border-radius: var(--el-border-radius-base);
  object-fit: cover;
}

.cinema-card__info {
  flex: 1;
  margin-left: var(--space-16);
}

.cinema-card__name {
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

.service-tag {
  padding: var(--space-4) var(--space-12);
  border-radius: var(--el-border-radius-base);
  font-size: var(--fs-xs);
  color: #ffffff;
}

.service-tag--refund {
  background-color: var(--el-color-primary);
}

.service-tag--promo {
  background-color: var(--el-color-warning);
}

.service-tag--wifi {
  background-color: var(--el-color-info);
}

.service-tag--parking {
  background-color: var(--el-color-success);
}

.cinema-card__detail {
  margin-bottom: var(--space-16);
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
  margin: var(--space-4);
  padding: var(--space-4);
}
</style>
