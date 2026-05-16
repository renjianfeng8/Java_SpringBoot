<template>
  <div style="width: 100%; margin: 0 auto;">
    <!-- 1. 加载状态提示 -->
    <div v-if="loading" style="padding: 50px; text-align: center; color: #666;">
      正在加载电影及影院信息...
    </div>

    <!-- 2. 错误提示 -->
    <div v-else-if="errorMsg" style="padding: 20px; color: #ef4238; text-align: center;">
      {{ errorMsg }}
    </div>

    <!-- 3. 核心内容：电影完整信息 + 目标影院列表 -->
    <div v-else>
      <!-- 3.1 电影详情头部（完整信息展示） -->
      <div style="background-color: #41036a;">
        <div style="display: flex; width: 60%; margin: 0 auto; align-items: flex-start;">
          <!-- 电影海报 -->
          <div style="margin-top: 5px;">
            <img :src="film.img " alt="电影海报" style="width: 250px; height: 300px;">
          </div>

          <!-- 电影基本信息 -->
          <div style="color: white; margin-left: 25px; margin-top: 15px; flex: 2;">
            <div style="font-size: 26px; font-weight: bold">{{ film.title || '未知电影' }}</div>
            <div style="margin: 8px 0; font-size: 14px;">{{ film.english || '无英文标题' }}</div>
            <div style="margin: 8px 0; font-size: 14px;">{{ film.types.join(' / ') || '未知类型' }}</div>
            <div style="margin: 8px 0; font-size: 14px;">
              {{ film.area || '未知地区' }} / {{ film.time || '未知时长' }} / {{ film.language || '未知语言' }}
            </div>
            <div style="margin: 8px 0; font-size: 14px;">
              上映时间：{{ film.start || '未知' }} / 格式：{{ film.resolution || '未知' }}
            </div>
            <el-button
                type="warning"
                plain
                style="width: 70%; height: 45px; font-size: 18px; margin-top: 20px; border-color: #ef4238; color: #ef4238;"
                @click="goToFilmDetail(film.id)"
            >
              查看更多电影详情
            </el-button>
          </div>

          <!-- 评分和票房（突出展示） -->
          <div style="flex: 1; color: white; text-align: center; display: flex; flex-direction: column; justify-content: center; margin-top: 20px;">
            <div style="margin-bottom: 30px;">
              <div style="font-size: 16px; opacity: 0.8;">影片口碑</div>
              <div style="font-size: 36px; margin: 10px 0; font-weight: bold;">{{ film.score || 0 }}分</div>
            </div>
            <div>
              <div style="font-size: 16px; opacity: 0.8;">累计票房</div>
              <div style="font-size: 36px; margin: 10px 0; font-weight: bold;">{{ formatBoxOffice(film.boxOffice) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3.2 核心：当前电影上映影院列表（与后端筛选逻辑对齐） -->
      <div style="padding: 30px 0; background-color: #fafafa;">
        <div style="width: 60%; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
          <!-- 影院列表标题（明确关联当前电影） -->
          <h3 style="font-size: 18px; color: #333; border-left: 4px solid #ef4238; padding-left: 10px; margin-bottom: 20px;">
            【{{ film.title || '当前电影' }}】上映影院列表
          </h3>

          <!-- 影院列表内容 -->
          <div>
            <!-- 无影院数据提示（优化文案） -->
            <div v-if="cinemaData.filmData.length === 0" style="text-align: center; padding: 50px; color: #999; border: 1px dashed #eee; border-radius: 8px;">
              <div style="font-size: 16px; margin-bottom: 10px;">暂无该电影的上映影院信息</div>
              <div style="font-size: 14px; opacity: 0.7;">可能该电影尚未排片或暂无合作影院</div>
            </div>

            <!-- 循环渲染影院卡片（优化布局和交互） -->
            <!-- 核心修改：外层flex增加align-items: stretch，让子容器高度一致 -->
            <div v-for="(cinema, index) in cinemaData.filmData" :key="index"
                 style="border: 1px solid #e0e0e0; border-radius: 8px; padding: 15px; margin-bottom: 15px; display: flex; align-items: stretch; transition: all 0.3s ease; gap: 15px;"
                 class="cinema-card">
              <!-- 1. 图片容器（固定高度，保持原样式） -->
              <div style="width: 180px; flex-shrink: 0; height: 145px; display: flex; align-items: center; justify-content: center; overflow: hidden;">
                <img :src="cinema.avatar" alt="影院图片"
                     style="width: 100%; height: 100%; object-fit: cover; border-radius: 5px;"
                >
              </div>

              <!-- 2. 信息容器（占据中间宽度，保持原内容） -->
              <div style="flex: 1; min-height: 120px; display: flex; flex-direction: column; justify-content: space-between;">
                <!-- 影院名称 -->
                <div style="display: flex; align-items: center; justify-content: space-between;">
                  <div style="font-size: 18px; font-weight: bold; color: #333;">{{ cinema.name || '未知影院' }}</div>
                </div>

                <!-- 影院服务标签（优化间距） -->
                <div style="margin: 8px 0; display: flex; gap: 8px; flex-wrap: wrap;">
                  <div style="padding: 2px 8px; border-radius: 12px; font-size: 12px; color: #fff; background-color: #ef4238;">退票无忧</div>
                  <div style="padding: 2px 8px; border-radius: 12px; font-size: 12px; color: #fff; background-color: #fa8c16;">儿童优惠</div>
                  <div style="padding: 2px 8px; border-radius: 12px; font-size: 12px; color: #fff; background-color: #1890ff;">WiFi覆盖</div>
                  <div style="padding: 2px 8px; border-radius: 12px; font-size: 12px; color: #fff; background-color: #52c41a;">免费停车</div>
                </div>

                <!-- 影院详细信息（优化排版，紧凑布局） -->
                <div style="color: #666; font-size: 14px; display: flex; flex-direction: column; gap: 4px; margin-top: 4px; flex: 1;">
                  <!-- 电话 -->
                  <div style="display: flex; align-items: center;">
                    <span style="color: #ef4238; margin-right: 6px; min-width: 50px;">电话：</span>
                    <span>{{ cinema.phone || '暂无' }}</span>
                  </div>

                  <!-- 邮箱 -->
                  <div style="display: flex; align-items: center;">
                    <span style="color: #ef4238; margin-right: 6px; min-width: 50px;">邮箱：</span>
                    <span>{{ cinema.email || '暂无' }}</span>
                  </div>

                  <!-- 地址（控制单行显示，避免高度过高） -->
                  <div style="display: flex; align-items: center; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                    <span style="color: #ef4238; margin-right: 6px; min-width: 50px;">地址：</span>
                    <span style="flex: 1; word-break: break-all;">{{ cinema.address || '暂无' }}</span>
                  </div>
                </div>
              </div>

              <!-- 3. 购票按钮容器（新增！固定在最右侧，与图片/信息区域垂直对齐） -->
              <div style="flex-shrink: 0; width: 120px; display: flex; align-items: center; justify-content: center;">
                <el-button
                    type="primary"
                    size="default"
                @click="goCinemaDetail(cinema.id, film.id)"
                style="width: 100%; height: 40px; font-size: 16px;"
                >
                立即购票
                </el-button>
              </div>
            </div>
          </div>

          <!-- 分页组件（确保与后端参数同步） -->
          <div style="margin-top: 20px; text-align: right;" v-if="cinemaData.total > 0">
            <el-pagination
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                :current-page="cinemaData.pageNum"
                :page-sizes="[5, 10, 15]"
                :page-size="cinemaData.pageSize"
                :total="cinemaData.total"
                background
                layout="total, prev, pager, next, jumper"
                :disabled="cinemaData.total === 0"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref, onMounted} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {ElMessage} from 'element-plus';
import request from "@/utils/request.js";
import 'element-plus/theme-chalk/el-pagination.css';
import 'element-plus/theme-chalk/el-button.css';

// 1. 路由相关（获取电影ID + 路由跳转）
const route = useRoute();
const router = useRouter();
const filmId = route.params.id; // 从路由参数获取当前电影ID（核心筛选条件）

// 2. 电影信息状态（补充完整字段，与后端返回对齐）
const loading = ref(false);
const errorMsg = ref('');
const film = reactive({
  id: '',
  title: '',
  english: '',
  img: '',
  score: 0,
  boxOffice: 0,
  types: [],
  area: '',
  time: '',
  language: '',
  resolution: '',
  start: '', // 上映时间
});

// 3. 影院列表状态（与后端分页参数严格对齐）
const cinemaData = reactive({
  filmData: [], // 影院列表数据
  pageNum: 1,   // 当前页码（默认1，与后端@RequestParam(defaultValue="1")对齐）
  pageSize: 10, // 每页条数（默认10，与后端@RequestParam(defaultValue="10")对齐）
  total: 0      // 总影院数（后端返回的PageInfo.total）
});

// 4. 工具函数
// 票房格式化（保持原逻辑，适配后端数值）
const formatBoxOffice = (value) => {
  if (value === 0 || !value) return '暂无数据';
  return value >= 10000 ? `${(value / 10000).toFixed(1)}万` : `${value}元`;
};

// 5. 路由跳转函数
// 跳转到电影详情页
const goToFilmDetail = (filmId) => {
  if (!filmId || isNaN(Number(filmId))) {
    ElMessage.warning('电影ID无效，无法查看详情');
    return;
  }
  router.push({path: `/front/filmDetail/${filmId}`});
};


const goCinemaDetail = (cinemaId, filmId) => {
  if (!cinemaId || !filmId) {
    ElMessage.warning('参数无效，无法查看影院详情');
    return;
  }
  // 跳转到cinemaDetail页面，使用影院ID作为路径参数，同时携带电影ID作为查询参数
  router.push({
    path: `/front/cinemaDetail/${cinemaId}`,  // 路径参数：影院ID
    query: { filmId: filmId }                 // 查询参数：电影ID（可选，根据需要）
  });
};

// 6. 加载电影完整信息（补充所有字段，与后端返回对齐）
const fetchFilmFullInfo = () => {
  // 前置校验：电影ID无效直接提示
  if (!filmId || isNaN(Number(filmId))) {
    errorMsg.value = '电影ID无效，请返回电影列表重试';
    return Promise.reject('电影ID无效');
  }

  return request.get(`/film/selectById/${filmId}`)
      .then(res => {
        if (res.code === '200' && res.data) {
          const data = res.data;
          // 类型映射表（与后端typeIds返回值对齐）
          const typeMap = {
            1: '记录', 4: '恐怖', 5: '喜剧', 6: '动漫', 7: '伦理', 13: '爱情', 14: '动作',
            15: '灾难', 16: '体育', 17: '动画', 18: '历史', 19: '犯罪', 20: '科幻', 21: '悬疑',
            22: '剧情', 23: '冒险', 24: '家庭'
          };
          // 地区映射表（与后端areaId返回值对齐）
          const areaMap = {
            1: '中国大陆', 2: '美国', 3: '日本', 4: '德国', 5: '法国', 6: '韩国', 7: '中国香港',
            8: '波兰', 9: '西班牙', 10: '意大利', 11: '印度', 12: '俄罗斯', 13: '英国', 14: '中国台湾', 15: '葡萄牙'
          };

          // 完整赋值电影信息（覆盖所有前端展示字段）
          Object.assign(film, {
            id: data.id,
            title: data.title?.trim() || '未知电影',
            english: data.english || '无英文标题',
            img: data.img,
            score: data.score || 0,
            boxOffice: data.boxOffice || 0,
            // 处理类型数组（后端返回typeIds为JSON字符串，需解析）
            types: data.typeIds ? (() => { try { return JSON.parse(data.typeIds).map(id => typeMap[id] || `未知类型(${id})`); } catch { return []; } })() : [],
            // 处理地区（后端返回areaId，映射为地区名称）
            area: data.areaId ? (areaMap[data.areaId] || `未知地区(${data.areaId})`) : '未知地区',
            time: data.time ? `${data.time}分钟` : '未知时长',
            language: data.language || '未知语言',
            resolution: data.resolution || '未知格式',
            start: data.start || '未知'
          });
          return Promise.resolve(); // 加载成功，允许后续加载影院
        } else {
          const errMsg = `电影信息加载失败：${res.msg || '未找到该电影'}`;
          errorMsg.value = errMsg;
          ElMessage.error(errMsg);
          return Promise.reject(errMsg);
        }
      })
      .catch(err => {
        const errMsg = `网络异常：${err.message || '无法加载电影信息'}`;
        errorMsg.value = errMsg;
        ElMessage.error(errMsg);
        return Promise.reject(errMsg);
      });
};

// 7. 核心功能：加载“当前电影上映的影院”（与后端接口参数严格对齐）
const loadCinemaList = () => {
  // 前置校验：电影ID无效不发起请求
  if (!filmId || isNaN(Number(filmId))) {
    loading.value = false;
    errorMsg.value = '电影ID无效，无法筛选影院';
    return;
  }

  loading.value = true;
  // 发起请求：携带 pageNum、pageSize、filmId 三个参数（与后端Controller参数对齐）
  request.get('/cinema/selectPage', {
    params: {
      pageNum: cinemaData.pageNum,   // 分页页码
      pageSize: cinemaData.pageSize, // 每页条数
      filmId: Number(filmId)         // 电影ID（转为数字，与后端Integer类型对齐）
    }
  })
      .then(res => {
        if (res.code === '200' && res.data) {
          // 后端返回PageInfo对象，提取list和total（与后端Service返回的PageInfo对齐）
          cinemaData.filmData = res.data.list || [];
          cinemaData.total = res.data.total || 0;
        } else {
          const errMsg = `影院列表加载失败：${res.msg || '未知错误'}`;
          ElMessage.error(errMsg);
          cinemaData.filmData = [];
          cinemaData.total = 0;
        }
      })
      .catch(err => {
        const errMsg = `网络异常：${err.message || '无法加载影院列表'}`;
        ElMessage.error(errMsg);
        cinemaData.filmData = [];
        cinemaData.total = 0;
      })
      .finally(() => {
        loading.value = false; // 无论成功失败，关闭加载状态
      });
};

// 8. 分页事件处理（与后端分页逻辑联动）
// 每页条数改变
const handleSizeChange = (newSize) => {
  cinemaData.pageSize = newSize;
  cinemaData.pageNum = 1; // 重置为第一页
  loadCinemaList(); // 重新加载筛选后的影院列表
};

// 页码改变
const handleCurrentChange = (newPage) => {
  cinemaData.pageNum = newPage;
  loadCinemaList(); // 重新加载筛选后的影院列表
};

// 9. 页面初始化：先加载电影信息，再加载对应影院（确保筛选条件有效）
onMounted(() => {
  // 先加载电影信息，成功后再加载影院列表
  fetchFilmFullInfo().then(() => {
    loadCinemaList();
  });
});
</script>

<style scoped>

</style>