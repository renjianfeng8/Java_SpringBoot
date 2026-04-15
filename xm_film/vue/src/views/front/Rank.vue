<template>
  <div style="width: 80%; margin: 20px auto;">
    <!-- 页面标题 -->
    <h2 style="text-align: center; margin-bottom: 28px; font-size: 26px; color: #333; font-weight: 600;">电影排行榜</h2>

    <!-- 双列布局：Flex 自动等高 -->
    <div style="display: flex; gap: 4%;">
      <!-- 票房排行榜卡片 -->
      <div style="flex: 1; border-radius: 5px; border: 1px solid black; padding: 20px;">
        <div style="display: flex;
            justify-content: center;
            align-items: center;
            border-bottom: 1px solid #c5c1c1;
            padding: 10px 0 12px;
            margin-bottom: 20px;">
          <span style="font-size: 22px; color: #fd4444; font-weight: 500;">总票房Top榜</span>
        </div>

        <div v-if="loading.boxOffice" style="padding: 30px 0;">
          <el-skeleton avatar :rows="10" :columns="3" />
        </div>

        <div v-else style="padding-right: 10px;">
          <div
              v-for="(film, index) in data.boxOfficeData"
              :key="film.id"
              style="display: flex; align-items: center; padding: 12px 0; border-bottom: 1px dashed #c5c1c1; position: relative;">
            <div
                style="width: 24px; height: 24px; line-height: 24px; text-align: center; border-radius: 50%; font-size: 14px; font-weight: bold; margin-right: 15px;"
                :style="{
                   backgroundColor: index < 3 ? ['#ff4d4f', '#faad14', '#1890ff'][index] : '#f5f5f5',
                   color: index < 3 ? '#fff' : '#666'
                 }">
              {{ index + 1 }}
            </div>

            <div style="flex: 1; display: flex; align-items: center;">
              <el-image
                  :src="film.img"
                  fit="cover"
                  placeholder
                  style="width: 60px; height: 80px; border-radius: 4px; object-fit: cover; margin-right: 15px;"
              />
              <div style="flex: 1;">
                <h4 style="font-size: 16px; color: #333; margin: 0 0 8px 0; line-height: 1.4;">
                  {{ film.title }}
                  <span style="font-size: 12px; color: #999; font-weight: normal; margin-left: 5px;">({{ film.english || '无英文标题' }})</span>
                </h4>
                <div style="font-size: 12px; color: #666; line-height: 1.5;">
                  <span style="display: block; margin-bottom: 4px;">类型：{{ film.types?.join(' / ') || '未知类型' }}</span>
                  <span style="margin-right: 15px;">上映时间：{{ film.start || '未知时间' }}</span>
                  <span>时长：{{ film.time || '未知时长' }}</span>
                </div>
              </div>
            </div>

            <div style="width: 120px; text-align: right; font-size: 14px; color: #ff4d4f; font-weight: 500;">
              {{ formatBoxOffice(film.boxOffice) }}
            </div>
          </div>
        </div>
      </div>

      <!-- 评分排行榜卡片 -->
      <div style="flex: 1; border-radius: 5px; border: 1px solid black; padding: 20px;">
        <div style="display: flex;
            justify-content: center;
            align-items: center;
            border-bottom: 1px solid #c5c1c1;
            padding: 10px 0 12px;
            margin-bottom: 20px;">
          <span style="font-size: 22px; color: #fd4444; font-weight: 500;">评分Top榜</span>
        </div>

        <div v-if="loading.mark" style="padding: 30px 0;">
          <el-skeleton avatar :rows="10" :columns="3" />
        </div>

        <div v-else-if="!data.markData.length" style="padding: 60px 0; text-align: center;">
          <el-empty description="暂无评分数据" />
        </div>

        <div v-else style="padding-right: 10px;">
          <div
              v-for="(film, index) in data.markData"
              :key="film.id"
              style="display: flex; align-items: center; padding: 12px 0; border-bottom: 1px dashed #c5c1c1; position: relative;"
          >
            <div
                style="width: 24px; height: 24px; line-height: 24px; text-align: center; border-radius: 50%; font-size: 14px; font-weight: bold; margin-right: 15px;"
                :style="{
                   backgroundColor: index < 3 ? ['#ff4d4f', '#faad14', '#1890ff'][index] : '#f5f5f5',
                   color: index < 3 ? '#fff' : '#666'
                 }">
              {{ index + 1 }}
            </div>

            <div style="flex: 1; display: flex; align-items: center;">
              <el-image
                  :src="film.img"
                  fit="cover"
                  placeholder
                  style="width: 60px; height: 80px; border-radius: 4px; object-fit: cover; margin-right: 15px;"
              />
              <div style="flex: 1;">
                <h4 style="font-size: 16px; color: #333; margin: 0 0 8px 0; line-height: 1.4;">
                  {{ film.title }}
                  <span style="font-size: 12px; color: #999; font-weight: normal; margin-left: 5px;">({{ film.english || '无英文标题' }})</span>
                </h4>
                <div style="font-size: 12px; color: #666; line-height: 1.5;">
                  <span style="display: block; margin-bottom: 4px;">类型：{{ film.types?.join(' / ') || '未知类型' }}</span>
                  <span style="margin-right: 15px;">上映时间：{{ film.start || '未知时间' }}</span>
                  <span>时长：{{ film.time || '未知时长' }}</span>
                </div>
              </div>
            </div>

            <div style="width: 120px; text-align: right; font-size: 16px; color: orange; font-weight: bold;">
              {{ film.score || 0 }} 分
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue';
import request from "@/utils/request.js";
import { ElMessage } from 'element-plus';
import 'element-plus/theme-chalk/el-skeleton.css';
import 'element-plus/theme-chalk/el-empty.css';
import 'element-plus/theme-chalk/el-image.css';
import 'element-plus/theme-chalk/el-button.css';

const data = reactive({
  boxOfficeData: [],
  markData: [],
});

const loading = reactive({
  boxOffice: false,
  mark: false,
});

const formatBoxOffice = (num) => {
  if (!num && num !== 0) return '0元';
  const unit = '元';
  const numericValue = Number(num);
  const formattedNum = isNaN(numericValue)
      ? String(num)
      : numericValue.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `${formattedNum}${unit}`;
};

const loadFilmBoxOfficeTop = () => {
  loading.boxOffice = true;
  request.get('/film/getAllBoxOfficeTop', {
    params: { topNum: 10 }
  }).then(res => {
    if (res.code === '200') {
      data.boxOfficeData = (res.data || []).slice(0, 10);
    } else {
      ElMessage.error(`票房数据加载失败：${res.msg || '未知错误'}`);
    }
  }).catch(err => {
    console.error('票房接口请求异常：', err);
    ElMessage.error('网络异常，无法加载票房数据');
  }).finally(() => {
    loading.boxOffice = false;
  });
};

const loadFilmMarkTop = () => {
  loading.mark = true;
  request.get('/film/getAllMarkTop', {
    params: { topNum: 10 }
  }).then(res => {
    if (res.code === '200') {
      data.markData = (res.data || []).slice(0, 10);
    } else {
      ElMessage.error(`评分数据加载失败：${res.msg || '未知错误'}`);
    }
  }).catch(err => {
    console.error('评分接口请求异常：', err);
    ElMessage.error('网络异常，无法加载评分数据');
  }).finally(() => {
    loading.mark = false;
  });
};

loadFilmBoxOfficeTop();
loadFilmMarkTop();
</script>

<style scoped>
</style>