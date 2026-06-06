<template>
  <div style="width: 75%; margin: 20px auto; display:flex">

    <!-- 左侧内容（完善跳转逻辑） -->
    <div style="flex: 1" >
      <!-- 正在热播区域 -->
      <div style="display: flex; align-items: center">
        <div style="flex: 1; font-size: 22px; color: #ef4238">正在热播 ({{data.data1.length}}) 部</div>
        <!-- 「全部」按钮：跳转到电影列表页（展示所有已上映电影） -->
        <div
            style="width: 60px;text-align: right; color: #ef4238; cursor: pointer;"
            @click="goToMovieList('playing')"
        >
          全部 >
        </div>
      </div>

      <div style="margin-top: 20px">
        <el-row :gutter="15">
          <!-- 正在热播电影：海报和购票按钮均跳转详情页 -->
          <el-col :span="6" v-for="item in data.playingData" :key="item.id" style="margin-bottom: 20px">
            <!-- 海报点击跳转详情 -->
            <div
                style="cursor: pointer; margin-bottom: 8px;"
                @click="goToFilmDetail(item.id)"
            >
              <img :src="item.img" alt="电影海报" style="width: 100%; height: 260px; border-radius: 5px; transition: all 0.3s;">
            </div>
            <!-- 购票按钮：跳转到详情页（后续可在详情页跳转选座） -->
            <el-button
                type="warning"
                plain
                style="width: 100%; height: 35px; border-color: #ef4238; color: #ef4238;"
                @click="goToFilmDetail(item.id)"
            >
              购票
            </el-button>
          </el-col>
        </el-row>
      </div>

      <!-- 即将上映区域 -->
      <div style="flex: 1; margin-top: 25px">
        <div style="display: flex; align-items: center">
          <div  style="flex: 1; font-size: 22px; color: #5b92e6">即将上映 ({{data.data2.length}}) 部</div>
          <!-- 「全部」按钮：跳转到电影列表页（展示所有待上映电影） -->
          <div
              style="width: 60px;text-align: right; color: #5b92e6; cursor: pointer;"
              @click="goToMovieList('upcoming')"
          >
            全部 >
          </div>
        </div>

        <div style="margin-top: 20px">
          <el-row :gutter="15">
            <!-- 即将上映电影：整卡片点击跳转详情页 -->
            <el-col :span="6" v-for="item in data.noPlayData" :key="item.id" style="margin-bottom: 20px; cursor: pointer;">
              <div @click="goToFilmDetail(item.id)" style="transition: all 0.3s;">
                <img :src="item.img" alt="电影海报" style="width: 100%; height: 260px; border-radius: 5px;">
                <div style="margin-top: 5px; font-size: 20px;font-weight: bold;font-style: italic">{{item.title}}</div>
                <div style="margin-top: 5px; font-size: 16px; color: orange">{{item.start}} 上映</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
    </div>

    <!-- 右侧内容（完善票房/评分列表跳转） -->
    <div style="width: 280px; margin-left: 50px">
      <!-- 1. 总票房Top 10（添加电影标题跳转详情） -->
      <div>
        <div style="margin: 8px 0; font-size: 22px; color: #ef4238">总票房Top 10</div>
        <div style="border: 1px solid #ef4238; border-radius: 5px; padding: 10px 5px;">
          <div v-if="loading.boxOffice" style="padding: 5px 0;">
            <el-skeleton :rows="10" :columns="3" avatar style="--el-skeleton-avatar-size: 24px;" />
          </div>
          <div v-else>
            <div v-for="(movie, index) in boxOfficeTop10" :key="movie.id" style="display: flex; align-items: center; padding: 8px 5px; transition: background-color 0.2s; cursor: pointer;" @click="goToFilmDetail(movie.id)">
              <!-- 排名标识 -->
              <div
                  style="width: 24px; height: 24px; line-height: 24px; text-align: center; border-radius: 50%; font-weight: bold; font-size: 14px;"
                  :style="{
                  backgroundColor: index < 3 ? ['#ff4d4f', '#faad14', '#1890ff'][index] : '#f5f5f5',
                  color: index < 3 ? '#fff' : '#666'
                }">
                {{ index + 1 }}
              </div>

              <div style="flex: 1; margin-left: 10px;">
                <!-- 电影标题点击跳转 -->
                <div style="font-weight: bold; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #333;">{{ movie.title }}</div>
                <div style="font-size: 12px; color: #666; margin-top: 3px;">
                  {{ movie.types?.join(' / ') || '未知类型' }} | {{ movie.start || '未知时间' }}
                </div>
              </div>

              <div style="text-align: right; font-weight: bold; color: #ef4238;">{{ formatBoxOffice(movie.boxOffice) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 2. 今日票房（保持不变） -->
      <div style="display: flex; margin-top: 40px; border-radius: 5px; overflow: hidden; ">
        <div style="width: 40px; background-color: #ef4238; display: flex; justify-content: center; align-items: center; padding: 10px;">
          <div style="display: flex; flex-direction: column; align-items: center; color: white; font-weight: bold; line-height: 1.8;">
            <span>今</span><span>日</span><span>票</span><span>房</span>
          </div>
        </div>
        <div style="flex: 1; background-color: #eee; display: flex; flex-direction: column; justify-content: center; padding: 0 20px;">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div style="font-size: 28px; font-weight: bold; color: #333;">{{ totalPrice.total }}亿</div>
            <el-button type="text" style="color: #ef4238;" @click="refreshTodayBoxOffice">
              <el-icon><Refresh /></el-icon>
              <span style="margin-left: 3px">刷新</span>
            </el-button>
          </div>
          <div style="font-size: 12px; color: #666; margin-top: 8px;">北京时间 : {{ updateTime }}</div>
        </div>
      </div>

      <!-- 3. 评分Top 5（添加电影标题/海报跳转详情，星级改为分数） -->
      <div style="margin-top: 40px">
        <div style="display: flex; align-items: center">
          <div style="flex: 1; font-size: 22px; color:#ef4238">评分Top 5</div>
          <!-- 「查看完整榜单」跳转排行榜页 -->
          <div
              style="width: 100px; text-align: right; color: #ef4238; cursor: pointer;"
              @click="goToRankPage()"
          >
            查看完整榜单>
          </div>
        </div>
        <div style="border: 1px solid #ef4238; border-radius: 5px; margin-top: 20px; padding: 10px 5px;">
          <div v-if="loading.mark" style="padding: 5px 0;">
            <el-skeleton :rows="5" :columns="3" avatar style="--el-skeleton-avatar-size: 80px;" />
          </div>
          <div v-else>
            <div v-for="(movie, index) in ratingTop5" :key="movie.id" style="display: flex; align-items: center; padding: 10px 5px; border-bottom: 1px dashed #c5c1c1; transition: background-color 0.2s; cursor: pointer;" @click="goToFilmDetail(movie.id)">
              <!-- 排名标识 -->
              <div
                  style="width: 24px; height: 24px; line-height: 24px; text-align: center; border-radius: 50%; font-weight: bold; font-size: 14px;"
                  :style="{
                  backgroundColor: index < 3 ? ['#ff4d4f', '#faad14', '#1890ff'][index] : '#f5f5f5',
                  color: index < 3 ? '#fff' : '#666'
                }">
                {{ index + 1 }}
              </div>

              <!-- 海报点击跳转 -->
              <div style="width: 60px; margin-left: 10px;">
                <img :src="movie.img" alt="电影海报" style="width: 100%; height: 80px; border-radius: 3px; object-fit: cover;">
              </div>

              <div style="flex: 1; margin-left: 10px;">
                <!-- 电影标题点击跳转 -->
                <div style="font-weight: bold; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #333;">{{ movie.title }}</div>
                <div style="font-size: 12px; color: #666; margin-top: 3px;">{{ movie.types?.join(' / ') || '未知类型' }}</div>
                <!-- 评分：移除el-rate星级，改为分数显示 -->
                <div style="font-size: 14px; color: orange; font-weight: 500; margin-top: 3px;">
                  {{ movie.score || 0 }} 分
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import request from "@/utils/request.js";
import { API_PATHS, FILM_API } from '@/constants';
// 引入Element Plus样式（移除el-rate相关样式）
import 'element-plus/theme-chalk/el-skeleton.css';
import 'element-plus/theme-chalk/el-button.css';
// 引入刷新图标组件（若之前未引入需补充）
import { Refresh } from '@element-plus/icons-vue';

// 路由实例初始化
const router = useRouter();

// 左侧正在热播/即将上映数据（保持不变）
const data = reactive({
  data1: [], // 已上映电影
  data2: [], // 待上映电影
  playingData: [], // 正在热播展示数据（前8条）
  noPlayData: [] // 即将上映展示数据（前8条）
});

// 右侧接口数据（保持不变）
const boxOfficeTop10 = reactive([]); // 总票房Top10
const ratingTop5 = reactive([]);     // 评分Top5
const totalPrice = reactive({        // 今日票房
  total: 1.28,
  change: 5.3
});
const updateTime = ref(new Date().toLocaleString()); // 刷新时间
const loading = reactive({           // 加载状态
  boxOffice: false,
  mark: false
});

/**
 * 1. 跳转到电影详情页
 * @param {number} filmId - 电影ID
 */
const goToFilmDetail = (filmId) => {
  if (!filmId) {
    ElMessage.warning('电影ID无效');
    return;
  }
  router.push({
    path: `/front/filmDetail/${filmId}`
  });
};

/**
 * 2. 跳转到电影列表页（区分“正在热播”和“即将上映”）
 * @param {string} type - 类型（playing：已上映；upcoming：待上映）
 */
const goToMovieList = (type) => {
  // 跳转到 /front/movie，并携带查询参数（用于筛选电影类型）
  router.push({
    path: '/front/movie',
    query: { type }
  });
};

const goToRankPage = () => {
  router.push('/front/rank');
};

// 票房金额格式化
const formatBoxOffice = (num) => {
  if (!num && num !== 0) return '0元';
  const unit = '元';
  const numericValue = Number(num);
  const formattedNum = isNaN(numericValue)
      ? String(num)
      : numericValue.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `${formattedNum}${unit}`;
};

// 加载总票房Top10数据
const loadFilmBoxOfficeTop = () => {
  loading.boxOffice = true;
  request.get(FILM_API.BOX_OFFICE_TOP, {
    params: {topNum: 10}
  }).then(res => {
    if (res.code === '200') {
      boxOfficeTop10.length = 0;
      boxOfficeTop10.push(...(res.data || []).slice(0, 10));
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

// 加载评分Top5数据
const loadFilmMarkTop = () => {
  loading.mark = true;
  request.get(FILM_API.MARK_TOP, {
    params: {topNum: 10}
  }).then(res => {
    if (res.code === '200') {
      ratingTop5.length = 0;
      ratingTop5.push(...(res.data || []).slice(0, 5));
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

// 模拟今日票房刷新
const refreshTodayBoxOffice = () => {
  setTimeout(() => {
    totalPrice.total = parseFloat((totalPrice.total + (Math.random() - 0.5) * 0.1).toFixed(2));//使用随机数模拟
    updateTime.value = new Date().toLocaleString();
    ElMessage.success('已更新最新今日票房数据');
  }, 500);
};

// 电影数据加载
const load = () => {
  request.get(API_PATHS.FILMS).then(res => {
    if (res.code === '200') {
      data.data1 = res.data.filter(v => v.status === '已上映');
      data.data2 = res.data.filter(v => v.status === '待上映');
      // 最多展示8条数据，避免页面过长
      data.playingData = data.data1.length > 8 ? data.data1.slice(0, 8) : data.data1;
      data.noPlayData = data.data2.length > 8 ? data.data2.slice(0, 8) : data.data2;
    } else {
      ElMessage.error(res.msg);
    }
  });
};

// 页面初始化加载所有数据
load();
loadFilmBoxOfficeTop();
loadFilmMarkTop();
</script>
