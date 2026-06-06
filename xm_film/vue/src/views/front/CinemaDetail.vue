<template>
  <div style="width: 100%; margin: 0 auto;">
    <!-- 1. 加载状态提示 -->
    <div v-if="loading" style="padding: 50px; text-align: center; color: #666;">
      正在加载影院及电影信息...
    </div>

    <!-- 2. 错误提示 -->
    <div v-else-if="errorMsg" style="padding: 20px; color: #ef4238; text-align: center;">
      {{ errorMsg }}
    </div>

    <!-- 3. 核心内容：影院完整信息 + 上线电影列表 -->
    <div v-else>
      <!-- 3.1 影院详情头部 -->
      <div style="background-color: #41036a;">
        <div style="display: flex; width: 60%; margin: 0 auto; align-items: flex-start; padding: 20px 0;">
          <!-- 影院图 -->
          <div style="margin-top: 5px; flex-shrink: 0;">
            <img
                :src="cinema.avatar"
                alt="影院图片"
                style="width: 250px; height: 300px; object-fit: cover; border-radius: 8px;"
            >
          </div>

          <!-- 影院基本信息 -->
          <div style="color: white; margin-left: 25px; margin-top: 15px; flex: 2;">
            <div style="font-size: 26px; font-weight: bold;">{{ cinema.name || '未知影院' }}</div>

            <!-- 地址信息 -->
            <div style="margin: 8px 0; font-size: 14px; display: flex; align-items: center;">
              <i class="el-icon-location" style="margin-right: 6px;"></i>
              地址: {{ cinema.address || '暂无地址信息' }}
            </div>

            <!-- 电话信息 -->
            <div style="margin: 8px 0; font-size: 14px; display: flex; align-items: center;">
              <i class="el-icon-phone" style="margin-right: 6px;"></i>
              电话: {{ cinema.phone || '暂无联系电话' }}
            </div>

            <!-- 营业时间 -->
            <div style="margin: 8px 0; font-size: 14px; display: flex; align-items: center;">
              <i class="el-icon-time" style="margin-right: 6px;"></i>
              营业时间: {{ cinema.businessHours || '暂无营业时间信息' }}
            </div>

            <!-- 影院服务标题 -->
            <div style="margin: 12px 0 8px; font-size: 16px; font-weight: 500;">影院服务:</div>

            <!-- 服务标签容器 -->
            <div style="display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 10px;">
              <!-- 退票无忧 -->
              <div style="background-color: rgba(255,255,255,0.1); border-radius: 4px; padding: 8px 12px; width: calc(50% - 5px); box-sizing: border-box;">
                <div style="font-weight: 500; margin-bottom: 4px; color: #ffd700; display: flex; align-items: center;">
                  <i class="el-icon-refresh-left" style="margin-right: 6px; font-size: 14px;"></i>
                  退票无忧
                </div>
                <div style="font-size: 12px; opacity: 0.9; line-height: 1.5;">
                  未取票用户在放映前60分钟可退票
                </div>
              </div>

              <!-- 儿童优惠 -->
              <div style="background-color: rgba(255,255,255,0.1); border-radius: 4px; padding: 8px 12px; width: calc(50% - 5px); box-sizing: border-box;">
                <div style="font-weight: 500; margin-bottom: 4px; color: #4effb7; display: flex; align-items: center;">
                  <i class="el-icon-user" style="margin-right: 6px; font-size: 14px;"></i>
                  儿童优惠
                </div>
                <div style="font-size: 12px; opacity: 0.9; line-height: 1.5;">
                  1位成人可免费带1位不满1.3米儿童，儿童免票无座
                </div>
              </div>

              <!-- WiFi覆盖 -->
              <div style="background-color: rgba(255,255,255,0.1); border-radius: 4px; padding: 8px 12px; width: calc(50% - 5px); box-sizing: border-box;">
                <div style="font-weight: 500; margin-bottom: 4px; color: #4ee1ff; display: flex; align-items: center;">
                  <i class="el-icon-wifi" style="margin-right: 6px; font-size: 14px;"></i>
                  WiFi覆盖
                </div>
                <div style="font-size: 12px; opacity: 0.9; line-height: 1.5;">
                  全场免费高速WiFi，观影期间也可顺畅连接
                </div>
              </div>

              <!-- 免费停车 -->
              <div style="background-color: rgba(255,255,255,0.1); border-radius: 4px; padding: 8px 12px; width: calc(50% - 5px); box-sizing: border-box;">
                <div style="font-weight: 500; margin-bottom: 4px; color: #ffb84d; display: flex; align-items: center;">
                  <i class="el-icon-location-information" style="margin-right: 6px; font-size: 14px;"></i>
                  免费停车
                </div>
                <div style="font-size: 12px; opacity: 0.9; line-height: 1.5;">
                  停车场位于长江西路辅路乐客来地面停车场和乐客来生活馆地下负二层均免费停车
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3.2 核心：该影院上线的电影 -->
      <div style="padding: 30px 0; background-color: #fafafa;">
        <div style="width: 60%; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
          <!-- 电影列表标题 -->
          <h3 style="font-size: 18px; color: #333; border-left: 4px solid #ef4238; padding-left: 10px; margin-bottom: 20px;">
            【{{ cinema.name || '当前影院' }}】上映电影列表
          </h3>

          <!-- 电影列表内容 -->
          <div>
            <!-- 无电影数据提示 -->
            <div v-if="filmData.films.length === 0" style="text-align: center; padding: 50px; color: #999; border: 1px dashed #eee; border-radius: 8px;">
              <div style="font-size: 16px; margin-bottom: 10px;">暂无该影院的上映电影信息</div>
              <div style="font-size: 14px; opacity: 0.7;">该影院可能暂未排片或暂无合作电影</div>
            </div>

            <!-- 电影列表容器（纵向排列，每个电影块包含海报信息+横向扩展的放映记录） -->
            <div class="film-container">
              <div v-for="(film, index) in filmData.films" :key="film.id" class="film-block">
                <!-- 电影基础信息（海报、名称、评分）- 左侧固定宽度 -->
                <div class="film-base-info">
                  <!-- 电影海报 -->
                  <div style="width: 140px; height: 200px; display: flex; align-items: center; justify-content: center; overflow: hidden; margin-bottom: 8px; border-radius: 5px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                    <img
                        :src="film.img"
                        alt="电影海报"
                        style="width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s ease;"
                        :style="{ transform: hoveredPoster[index] ? 'scale(1.05)' : 'scale(1)' }"
                    >
                  </div>

                  <!-- 电影名称（图片下方） -->
                  <div style="font-size: 15px; font-weight: 500; color: #333; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 140px;">
                    {{ film.title || '未知电影' }}
                  </div>

                  <!-- 评分（名称下方） -->
                  <div style="color:#d5d529; font-weight: 600; font-size: 16px; margin-bottom: 8px; display: flex; align-items: center; width: 140px;">
                    <i class="el-icon-star-on" style="font-size: 14px; margin-right: 3px;"></i>
                    {{ film.score }}分
                  </div>
                </div>

                <!-- 放映记录区域（右侧横向扩展，占满剩余宽度） -->
                <div class="film-record-section">
                  <!-- 放映记录标题 -->
                  <div style="color: #666; font-weight: 500; margin-bottom: 6px; display: flex; align-items: center;">
                    <i class="el-icon-video-play" style="font-size: 12px; margin-right: 3px;"></i>
                    放映场次
                  </div>

                  <!-- 放映记录加载中 -->
                  <div v-if="recordLoading[film.id]" style="color: #999; text-align: center; padding: 20px 0; border: 1px dashed #eee; border-radius: 4px;">
                    <i class="el-icon-loading" style="font-size: 16px; animation: rotating 2s linear infinite;"></i>
                    <div style="margin-top: 5px;">加载中...</div>
                  </div>

                  <!-- 无放映记录 -->
                  <div v-else-if="!recordData[film.id] || recordData[film.id].list.length === 0" style="color: #999; text-align: center; padding: 20px 0; border: 1px dashed #eee; border-radius: 4px;">
                    <i class="el-icon-info" style="margin-right: 3px;"></i>暂无排片
                  </div>

                  <!-- 有放映记录（表格展开形式，横向铺满） -->
                  <div v-else style="border: 1px solid #eee; border-radius: 4px; overflow: hidden;">
                    <!-- 表格头部（灰色背景，固定布局） -->
                    <div style="display: flex; background-color: #f5f5f5; padding: 8px 15px; font-weight: 500; color: #666; border-bottom: 1px solid #eee;">
                      <div style="width: 25%; text-align: center;">放映时间</div>
                      <div style="width: 20%; text-align: center;">影厅</div>
                      <div style="width: 15%; text-align: center;">售价</div>
                      <div style="width: 40%; text-align: center;">操作</div>
                    </div>
                    <!-- 表格内容（滚动容器） -->
                    <div style="max-height: 200px; overflow-y: auto;">
                      <div v-for="record in recordData[film.id].list" :key="record.id" style="display: flex; padding: 10px 15px; border-bottom: 1px solid #f0f0f0; align-items: center;">
                        <!-- 放映时间（合并日期+时间） -->
                        <div style="width: 25%; text-align: center; color: #333; font-size: 13px;">
                          {{ formatDate(record.start) }}<br>{{ formatTime(record.start) }}
                        </div>
                        <!-- 影厅名称 -->
                        <div style="width: 20%; text-align: center; color: #666; font-size: 13px;">
                          {{ record.roomName || '未知影厅' }}
                        </div>
                        <!-- 售价 -->
                        <div style="width: 15%; text-align: center; color: #ef4238; font-weight: 500; font-size: 13px;">
                          {{ record.price }}
                        </div>
                        <!-- 操作：选座购票按钮/状态标签 -->
                        <div style="width: 40%; text-align: center; display: flex; justify-content: center; gap: 10px; align-items: center;">
                          <!-- 可购票状态：显示选座购票按钮（增加按钮尺寸和样式） -->
                          <button
                              v-if="record.status === '已上映' || record.status === '放映中' || record.status === '未开始'"
                              style="background-color: #ef4238; color: white; border: none; border-radius: 4px; padding: 4px 12px; font-size: 12px; cursor: pointer; transition: background-color 0.2s;"
                              @click="goToBuyTicket(cinemaId, film.id, record.id, record.roomId)"
                          >
                            选座购票
                          </button>
                          <!-- 其他状态：显示状态标签 + 不可点击提示 -->
                          <div v-else style="display: flex; align-items: center; gap: 8px;">
                            <div :style="getStatusStyle(record.status)" class="status-tag">
                              {{ record.status }}
                            </div>
                            <span style="color: #999; font-size: 11px;">不可购票</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 放映记录分页（底部居中，与表格对齐） -->
                    <div v-if="recordData[film.id] && recordData[film.id].total > recordData[film.id].pageSize"
                         style="margin-top: 8px; padding: 8px 0; text-align: center; border-top: 1px solid #eee; background-color: #fafafa;">
                      <el-pagination
                          @size-change="(val) => handleRecordSizeChange(film.id, val)"
                          @current-change="(val) => handleRecordCurrentChange(film.id, val)"
                          :current-page="recordData[film.id].pageNum"
                          :page-sizes="[5, 10, 15]"
                          :page-size="recordData[film.id].pageSize"
                          layout="total, sizes, prev, pager, next"
                          :total="recordData[film.id].total"
                          small
                      />
                    </div>
                  </div>
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
import {reactive, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {ElMessage} from 'element-plus';
import request from "@/utils/request.js";
import { API_PATHS, FILM_API, apiById, apiPage } from '@/constants';


// 1. 路由相关：参数提取与监听
const route = useRoute();
const router = useRouter();
const cinemaId = ref(Number(route.params.id));
const filmId = ref(route.query.filmId ? Number(route.query.filmId) : null);

// 2. 基础状态管理
const loading = ref(false);
const errorMsg = ref('');
const cinema = reactive({
  id: '',
  name: '',
  avatar: '',
  address: '',
  phone: '',
  rating: 0,
  hallCount: 0,
  todaySchedule: 0,
  businessHours: ''
});

// 3. 电影列表状态
const filmData = reactive({
  films: [],
  pageNum: 1,
  pageSize: 10,
  total: 0
});

// 4. 海报悬停效果
const hoveredPoster = ref([]);

// 5. 放映记录状态（按电影ID存储，支持多电影独立分页）
const recordLoading = ref({}); // 按电影ID存储加载状态
const recordData = reactive({}); // 格式：{ filmId: { list: [], pageNum: 1, pageSize: 5, total: 0 } }

// 6. 工具函数：日期格式化
const formatDate = (dateTimeStr) => {
  if (!dateTimeStr) return '未知日期';
  const date = new Date(dateTimeStr);
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
};

// 7. 工具函数：时间格式化
const formatTime = (dateTimeStr) => {
  if (!dateTimeStr) return '未知时间';
  const date = new Date(dateTimeStr);
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
};

// 8. 状态样式处理
const getStatusStyle = (status) => {
  switch (status) {
    case '未开始':
      return {
        color: '#409eff',
        backgroundColor: 'rgba(64, 158, 255, 0.1)',
        padding: '2px 6px',
        fontSize: '11px'
      };
    case '放映中':
      return {
        color: '#67c23a',
        backgroundColor: 'rgba(103, 194, 58, 0.1)',
        padding: '2px 6px',
        fontSize: '11px'
      };
    case '已结束':
      return {
        color: '#909399',
        backgroundColor: 'rgba(144, 147, 153, 0.1)',
        padding: '2px 6px',
        fontSize: '11px'
      };
    default:
      return {
        color: '#e6a23c',
        backgroundColor: 'rgba(230, 162, 60, 0.1)',
        padding: '2px 6px',
        fontSize: '11px'
      };
  }
};

// 9. 核心函数：加载影院详情
const fetchCinemaInfo = () => {
  const validCinemaId = Number(cinemaId.value);
  if (isNaN(validCinemaId) || validCinemaId <= 0) {
    errorMsg.value = `影院ID无效（当前值：${cinemaId.value}），请返回影院列表重试`;
    return Promise.reject('影院ID无效');
  }

  loading.value = true;
  return request.get(apiById(API_PATHS.CINEMAS, validCinemaId))
      .then(res => {
        if (res.code === '200' && res.data) {
          const {services, ...cinemaInfo} = res.data;
          Object.assign(cinema, cinemaInfo);
          return Promise.resolve();
        } else {
          const errMsg = `影院信息加载失败：${res.msg || '未找到该影院'}`;
          errorMsg.value = errMsg;
          ElMessage.error(errMsg);
          return Promise.reject(errMsg);
        }
      })
      .catch(err => {
        const errMsg = `网络异常：${err.message || '无法加载影院信息'}`;
        errorMsg.value = errMsg;
        ElMessage.error(errMsg);
        return Promise.reject(errMsg);
      })
      .finally(() => loading.value = false);
};

// 10. 核心函数：加载电影列表
const loadFilmList = () => {
  const validCinemaId = Number(cinemaId.value);
  if (isNaN(validCinemaId) || validCinemaId <= 0) {
    loading.value = false;
    errorMsg.value = `影院ID无效（当前值：${cinemaId.value}），无法加载电影列表`;
    return;
  }

  loading.value = true;
  const requestParams = {
    cinemaId: validCinemaId,
    pageNum: filmData.pageNum,
    pageSize: filmData.pageSize
  };
  if (filmId.value && !isNaN(Number(filmId.value))) {
    requestParams.filmId = Number(filmId.value);
  }

  request.get(FILM_API.BY_CINEMA, {params: requestParams})
      .then(res => {
        if (res.code === '200') {
          filmData.films = res.data || [];
          filmData.total = filmData.films.length;
          hoveredPoster.value = new Array(filmData.films.length).fill(false);

          // 加载每个电影的放映记录（默认每页5条，提升展示效率）
          filmData.films.forEach(film => {
            fetchRecordList(validCinemaId, film.id);
          });

          // 定位目标电影（滚动到对应电影块）
          if (filmId.value && filmData.films.length > 0) {
            const targetIndex = filmData.films.findIndex(film => Number(film.id) === filmId.value);
            if (targetIndex > -1) {
              setTimeout(() => {
                const targetItem = document.querySelectorAll('.film-block')[targetIndex];
                if (targetItem) {
                  targetItem.scrollIntoView({behavior: 'smooth', block: 'start'});
                }
              }, 300);
            } else {
              ElMessage.info(`未找到ID为${filmId.value}的电影排片`);
            }
          }
        } else {
          const errMsg = `电影列表加载失败：${res.msg || '未知错误'}`;
          ElMessage.error(errMsg);
          filmData.films = [];
          filmData.total = 0;
        }
      })
      .catch(err => {
        const errMsg = `网络异常：${err.message || '无法加载电影列表'}`;
        ElMessage.error(errMsg);
        filmData.films = [];
        filmData.total = 0;
      })
      .finally(() => loading.value = false);
};

// 11. 核心函数：加载指定影院+电影的放映记录
const fetchRecordList = (cinemaId, filmId) => {
  // 初始化当前电影的放映记录状态（默认每页5条）
  if (!recordData[filmId]) {
    recordData[filmId] = {
      list: [],
      pageNum: 1,
      pageSize: 5, // 优化：默认每页显示5条，减少分页操作
      total: 0
    };
  }

  recordLoading.value[filmId] = true;
  // 构造请求参数（匹配后端Record实体类）
  const requestParams = {
    cinemaId: cinemaId,   // 与后端 Record 实体的 cinemaId 字段对应
    filmId: filmId,       // 与后端 Record 实体的 filmId 字段对应
    pageNum: recordData[filmId].pageNum,
    pageSize: recordData[filmId].pageSize
  };
  request.get(apiPage(API_PATHS.RECORDS), {params: requestParams})
      .then(res => {
        if (res.code === '200' && res.data) {
          // 适配后端PageInfo格式：{ list: [], total: 0, pageNum: 1, pageSize: 5 }
          recordData[filmId].list = res.data.list || [];
          recordData[filmId].total = res.data.total || 0;
          recordData[filmId].pageNum = res.data.pageNum || 1;
          recordData[filmId].pageSize = res.data.pageSize || 5;
        } else {
          const errMsg = `放映记录加载失败：${res.msg || '未知错误'}`;
          ElMessage.warning(errMsg);
          recordData[filmId].list = [];
          recordData[filmId].total = 0;
        }
      })
      .catch(err => {
        const errMsg = `网络异常：${err.message || '无法加载放映记录'}`;
        ElMessage.error(errMsg);
        recordData[filmId].list = [];
        recordData[filmId].total = 0;
      })
      .finally(() => {
        recordLoading.value[filmId] = false;
      });
};

// 12. 放映记录分页事件处理
// 每页条数改变
const handleRecordSizeChange = (filmId, pageSize) => {
  recordData[filmId].pageSize = pageSize;
  recordData[filmId].pageNum = 1; // 重置为第一页
  fetchRecordList(cinemaId.value, filmId);
};

// 当前页改变
const handleRecordCurrentChange = (filmId, pageNum) => {
  recordData[filmId].pageNum = pageNum;
  fetchRecordList(cinemaId.value, filmId);
};

// 13. 购票跳转函数
const goToBuyTicket = (cinemaId, filmId, recordId, roomId) => {
  if (isNaN(cinemaId) || isNaN(filmId) || isNaN(recordId)) {
    ElMessage.warning('参数无效，无法购票');
    return;
  }

  // 补充roomId默认值（避免null/0，默认传1=一号厅）
  const finalRoomId = roomId && roomId > 0 ? roomId : 1;

  const query = {
    cinemaId: cinemaId.toString(),
    filmId: filmId.toString(),
    recordId: recordId.toString(),
    roomId: finalRoomId.toString() // 新增：携带影厅ID
  };

  router.push({path: '/front/buyTicket', query}).catch(err => {
    if (err.name !== 'NavigationDuplicated') ElMessage.error('跳转失败，请稍后重试');
  });
};

// 14. 路由监听（影院/电影ID变化时重新加载数据）
watch([() => route.params.id, () => route.query.filmId], ([newCinemaId, newFilmId]) => {
  cinemaId.value = Number(newCinemaId);
  filmId.value = newFilmId ? Number(newFilmId) : null;
  // 先加载影院信息，再加载电影列表，最后加载放映记录
  fetchCinemaInfo().then(() => loadFilmList());
}, {immediate: true, deep: true});
</script>

<style scoped>
/* 电影容器：纵向排列每个电影块 */
.film-container {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

/* 电影块：横向布局（基础信息+放映记录） */
.film-block {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  padding: 15px;
  border-radius: 8px;
  background-color: #fafafa;
  transition: box-shadow 0.3s ease;
}

/* 放映记录区域：右侧横向扩展，占满剩余宽度 */
.film-record-section {
  flex: 1;
  width: calc(100% - 160px);
}

/* 电影基础信息容器（左侧固定宽度） */
.film-base-info {
  width: 140px;
}

/* 状态标签通用样式（补充原有内联样式，保持一致性） */
.status-tag {
  border-radius: 2px;
  display: inline-block;
}
</style>
