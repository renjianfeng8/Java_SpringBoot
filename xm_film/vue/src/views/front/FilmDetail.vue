<template>
  <div style="width: 100%; margin: 0 auto;">
    <!-- 1. 加载状态提示 -->
    <div v-if="loading" style="padding: 50px; text-align: center; color: #666;">
      正在加载电影详情...
    </div>

    <!-- 2. 错误提示 -->
    <div v-else-if="errorMsg" style="padding: 20px; color: #ef4238; text-align: center;">
      {{ errorMsg }}
    </div>

    <!-- 3. 电影内容区域 -->
    <div v-else>
      <!-- 3.1 电影头部信息区域 -->
      <div style="background-color: #41036a;">
        <div style="display: flex; width: 60%; margin: 0 auto;">
          <!-- 电影海报 -->
          <div>
            <img :src="film.img" alt="电影海报" style="width: 250px; height: 300px; object-fit: cover;">
          </div>

          <!-- 电影基本信息 -->
          <div style="color: white; margin-left: 20px; margin-top: 15px; flex: 2;">
            <div style="font-size: 24px; font-weight: bold">{{ film.title || '未知电影' }}</div>
            <div style="margin: 5px 0">{{ film.english || '无英文标题' }}</div>
            <div style="margin: 5px 0">{{ film.types.join(' / ') || '未知类型' }}</div>
            <div style="margin: 5px 0">{{ film.area || '未知地区' }} / {{ film.time || '未知时长' }}</div>
            <div style="margin: 5px 0">{{ film.language || '未知语言' }} / {{ film.resolution || '未知格式' }}</div>
            <div style="margin: 5px 0">{{ film.start || '未知上映时间' }} 开始上映</div>
            <!-- 新增：显示单个演员的基础信息（从film.actorInfo获取） -->
            <div style="margin: 5px 0" v-if="film.actorInfo">
              主演：{{ film.actorInfo.split(':')[1] || '未知演员' }}
            </div>

            <el-button
                type="warning"
                plain
                style="width: 70%; height: 45px; font-size: 18px; margin-top: 20px; border-color: #ef4238; color: #ef4238;"
                :disabled="film.status !== '已上映'"
                @click="goToFilmCinema(film.id)"
            >
              {{ film.status === '已上映' ? '立即购票' : '暂未上映' }}
            </el-button>
          </div>

          <!-- 评分和票房 -->
          <div style="flex: 1; color: white; text-align: center; display: flex; flex-direction: column; justify-content: center;">
            <div>
              <div>影片口碑</div>
              <div style="font-size: 28px; margin: 10px 0">{{ film.score || 0 }}分</div>
            </div>
            <div style="margin-top: 30px;">
              <div>累计票房</div>
              <div style="font-size: 28px; margin: 10px 0">{{ formatBoxOffice(film.boxOffice) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3.2 电影详细信息区域 -->
      <div style="min-height: 400px; padding: 30px 0; background-color: #fafafa;">
        <div style="width: 60%; display: flex; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
          <!-- 左侧信息区 -->
          <div style="flex: 2;">
            <h3 style="font-size: 18px; color: #333; border-left: 4px solid #ef4238; padding-left: 10px;">剧情简介</h3>
            <p style="margin-top: 15px; line-height: 1.8; color: #333; white-space: pre-line; text-indent: 2em;">
              {{ film.intro || '暂无剧情简介' }}
            </p>

            <!-- 演职人员区域（核心修改：适配单个actorId+多角色区分） -->
            <h3 style="font-size: 18px; color: #333; border-left: 4px solid #ef4238; padding-left: 10px; margin-top: 30px;">演职人员</h3>
            <div v-if="loadingCast" style="margin-top: 15px; color: #666; padding: 20px; text-align: center;">
              正在加载演职人员信息...
            </div>
            <div v-else-if="castError" style="margin-top: 15px; color: #ef4238; padding: 20px; text-align: center;">
              {{ castError }}
            </div>
            <!-- 演职人员列表展示（按角色分类，圆形头像+名字布局） -->
            <div v-else style="margin-top: 15px;">
              <!-- 导演组 -->
              <div class="cast-group" v-if="directorList.length > 0">
                <div class="cast-card-container"> <!-- 卡片容器：横向排列 -->
                  <div v-for="(director, idx) in directorList" :key="director.id" class="cast-card">
                    <!-- 圆形头像 -->
                    <div class="cast-avatar">
                      <img :src="director.picture" :alt="`${director.actor}的头像`" class="avatar-img">
                    </div>
                    <!-- 名字+角色（换行显示） -->
                    <div class="cast-info">
                      <div class="cast-name">{{ director.actor }}</div>
                      <div class="cast-role">导演</div> <!-- 固定角色为“导演” -->
                    </div>
                  </div>
                </div>
              </div>

              <!-- 主演组 -->
              <div class="cast-group" style="margin-top: 20px;" v-if="actorList.length > 0">

                <div class="cast-card-container">
                  <div v-for="(actor, idx) in actorList" :key="actor.id" class="cast-card">
                    <div class="cast-role">{{ actor.role || '主演' }}</div>
                    <div class="cast-avatar">
                      <img :src="actor.picture" :alt="`${actor.actor}的头像`" class="avatar-img">
                    </div>
                    <div class="cast-info">
                      <div class="cast-name">{{ actor.actor }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 编剧组 -->
              <div class="cast-group" style="margin-top: 20px;" v-if="screenwriterList.length > 0">

                <div class="cast-card-container">
                  <div v-for="(writer, idx) in screenwriterList" :key="writer.id" class="cast-card">
                    <div class="cast-avatar">
                      <img :src="writer.picture" :alt="`${writer.actor}的头像`" class="avatar-img">
                    </div>
                    <div class="cast-info">
                      <div class="cast-name">{{ writer.actor }}</div>
                      <div class="cast-role">编剧</div> <!-- 固定角色为“编剧” -->
                    </div>
                  </div>
                </div>
              </div>

              <!-- 二级演员组 -->
              <div class="cast-group" style="margin-top: 20px;" v-if="supportActorList.length > 0">

                <div class="cast-card-container">
                  <div v-for="(actor, idx) in supportActorList" :key="actor.id" class="cast-card">
                    <div class="cast-avatar">
                      <img :src="actor.picture" :alt="`${actor.actor}的头像`" class="avatar-img">
                    </div>
                    <div class="cast-info">
                      <div class="cast-name">{{ actor.actor }}</div>
                      <div class="cast-role">{{ actor.role || '配角' }}</div> <!-- 显示具体角色 -->
                    </div>
                  </div>
                </div>
              </div>

              <!-- 无演职人员兜底 -->
              <div v-if="!directorList.length && !actorList.length && !screenwriterList.length && !supportActorList.length"
                   style="margin-top: 15px; color: #666; padding: 10px;">
                暂无演职人员信息
              </div>
            </div>

            <h3 style="font-size: 18px; color: #333; border-left: 4px solid #ef4238; padding-left: 10px; margin-top: 30px;">出品信息</h3>
            <p style="margin-top: 15px; line-height: 1.8; color: #333; white-space: pre-line;">
              {{ film.production || '暂无出品方介绍' }}
            </p>
          </div>

          <!-- 右侧视频区 -->
          <div style="flex: 1; margin-left: 40px;">
            <h3 style="font-size: 18px; color: #333; border-left: 4px solid #ef4238; padding-left: 10px;">预告视频</h3>
            <!-- 关键修改：用video标签替代div，实现视频播放 -->
            <div style="margin-top: 15px; border-radius: 4px; overflow: hidden;">
              <!-- 有视频URL时显示播放器 -->
              <video v-if="film.video" :src="film.video" controls style="width: 100%; height: 200px; object-fit: cover; background-color: #000;">
                您的浏览器不支持HTML5视频播放，请升级浏览器。
              </video>
              <!-- 无视频URL时显示提示 -->
              <div v-else style="line-height: 1.8; color: #333; background-color: #f5f5f5; height: 200px; display: flex; align-items: center; justify-content: center; border-radius: 4px;">
                暂无预告视频
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref, onMounted, computed} from 'vue';
import {useRoute} from 'vue-router';
import {ElMessage} from 'element-plus';
import request from "@/utils/request.js";
import 'element-plus/theme-chalk/el-button.css';
// 导入路由跳转函数
import { useRouter } from 'vue-router';

// 初始化路由实例
const router = useRouter();

// 1. 路由参数获取（电影ID）
const route = useRoute();
const filmId = route.params.id; // 从路由获取当前电影ID

// 2. 页面核心状态管理
const loading = ref(false); // 电影详情加载状态
const errorMsg = ref('');   // 电影详情错误提示
const film = reactive({     // 电影基础数据（默认值避免渲染空白）
  id: '',
  title: '',
  img: '',
  score: 0,
  start: '',
  types: [],
  area: '',
  time: '',
  language: '',
  resolution: '',
  boxOffice: 0,
  status: '',
  intro: '',
  english: '',
  production: '',
  video: '',
  actorId: '', // 单个演员ID（从后端film表获取）
  actorInfo: ''// 拼接后的演员信息（如"9:成龙"，从后端film接口返回）
});

// 3. 演职人员状态管理（适配单个actorId+多角色）
const castList = ref([]);    // 演职人员列表（存储所有查询到的人员，含多角色）
const loadingCast = ref(false); // 演职人员加载状态
const castError = ref('');   // 演职人员加载错误

// 4. 字典映射表（新增：角色类型映射，需与后端actor表的roleType字段匹配）
const roleTypeMap = {
  1: '导演',
  2: '主演',
  3: '编剧',
  4: '二级演员'
};

// 电影类型映射（匹配接口返回的typeIds）
const typeMap = {
  1: '记录',
  4: '恐怖',
  5: '喜剧',
  6: '动漫',
  7: '伦理',
  13: '爱情',
  14: '动作',
  15: '灾难',
  16: '体育',
  17: '动画',
  18: '历史',
  19: '犯罪',
  20: '科幻',
  21: '悬疑',
  22: '剧情',
  23: '冒险',
  24: '家庭',
};

// 电影地区映射
const areaMap = {
  1: '中国大陆',
  2: '美国',
  3: '日本',
  4: '德国',
  5: '法国',
  6: '韩国',
  7: '中国香港',
  8: '波兰',
  9: '西班牙',
  10: '意大利',
  11: '印度',
  12: '俄罗斯',
  13: '英国',
  14: '中国台湾',
  15: '葡萄牙',
};

// 5. 工具函数
// 票房格式化（万级单位转换）
const formatBoxOffice = (value) => {
  if (value === 0 || !value) return '暂无数据';
  return value >= 10000
      ? `${(value / 10000).toFixed(1)}万`
      : `${value}元`;
};

// 6. 计算属性（按角色类型筛选演职人员，核心修改）
// 筛选导演列表（roleType=1）
const directorList = computed(() => {
  return castList.value.filter(item => item.roleType === 1);
});

// 筛选主演列表（roleType=2，默认单个actorId对应主演）
const actorList = computed(() => {
  return castList.value.filter(item => item.roleType === 2);
});

// 筛选编剧列表（roleType=3）
const screenwriterList = computed(() => {
  return castList.value.filter(item => item.roleType === 3);
});

// 筛选二级演员列表（roleType=4）
const supportActorList = computed(() => {
  return castList.value.filter(item => item.roleType === 4);
});

// 7. 核心接口请求函数（核心修改：适配单个actorId查询）
/**
 * 请求电影详情数据
 * 作用：获取电影基础信息+单个actorId+actorInfo
 */
const fetchFilmDetail = () => {
  if (!filmId || isNaN(Number(filmId))) {
    errorMsg.value = '电影ID无效，请返回列表重试';
    return;
  }
  loading.value = true;
  errorMsg.value = '';
  request.get(`/film/selectById/${filmId}`).then(res => {
        if (res.code === '200' && res.data) {
          const data = res.data;
          // 电影数据映射
          Object.assign(film, {
            id: data.id,
            title: data.title?.trim() || '未知电影',
            img: data.img || '默认海报地址（可选）',
            score: data.score || 0,
            start: data.start || '未知上映时间',
            types: data.typeIds
                ? (() => { try { return JSON.parse(data.typeIds).map(id => typeMap[id] || `未知类型(${id})`); } catch { return []; } })()
                : [],
            area: data.areaId
                ? (areaMap[data.areaId] || `未知地区(${data.areaId})`)
                : '未知地区',
            time: data.time ? `${data.time}分钟` : '未知时长',
            language: data.language || '未知语言',
            resolution: data.resolution || '未知格式',
            boxOffice: data.boxOffice || 0,
            status: data.status || '未知状态',
            intro: data.content || '暂无剧情简介',
            english: data.english || '无英文标题',
            production: data.employee || '暂无出品信息',
            video: data.video || '', // 视频URL赋值
            actorId: data.actorId || '',
            actorInfo: data.actorInfo || ''
          });
          if (film.actorId) {
            fetchCastBySingleId(film.actorId);
          } else {
            castError.value = '暂无演职人员关联信息';
          }
        } else {
          errorMsg.value = `加载失败：${res.msg || '未找到该电影信息'}`;
        }
      })
      .catch(err => {
        errorMsg.value = '网络异常，无法加载电影详情';
        ElMessage.error('网络错误，请稍后重试');
      })
      .finally(() => {
        loading.value = false;
      });
};


/**
 * 按单个actorId查询演职人员详情（适配后端selectById接口）
 * 逻辑：根据单个演员ID，获取该演员的角色类型（导演/主演等）
 */
const fetchCastBySingleId = (actorId) => {
  // 前置校验：演员ID无效时直接提示
  if (!actorId || isNaN(Number(actorId))) {
    castError.value = '演员ID无效，无法加载演职人员信息';
    return;
  }

  loadingCast.value = true;
  castError.value = '';
  castList.value = []; // 清空历史数据

  // 调用后端接口：按单个演员ID查询详情（含角色类型）
  request.get(`/actor/selectById/${actorId}`)
      .then(res => {
        if (res.code === '200' && res.data) {
          const actorData = res.data;
          // 验证演员数据是否包含必要字段
          if (actorData.id && actorData.actor) {
            // 将单个演员信息转为数组，统一存入castList
            castList.value = [
              {
                id: actorData.id,
                actor: actorData.actor, // 演员/导演名称（actor表的actor字段）
                roleType: actorData.roleType || 2, // 角色类型（默认2=主演）
                role: actorData.role || '' ,// 具体角色名称（如"Jackie"）
                picture: actorData.picture,//演员图片
                video: actorData.video //预告视频
              }
            ];
          } else {
            castError.value = '演职人员信息不完整';
          }
        } else {
          castError.value = `演职人员加载失败：${res.msg || '未找到该演员信息'}`;
        }
      })
      .catch(err => {
        console.error('演职人员详情请求异常：', err);
        castError.value = '演职人员信息加载失败，请稍后重试';
        ElMessage.error('演职人员查询错误，请重试');
      })
      .finally(() => {
        loadingCast.value = false;
      });
};

const goToFilmCinema = (filmId) => {
  if (!filmId) {
    ElMessage.warning('电影ID无效');
    return;
  }
  router.push({
    path: `/front/filmCinema/${filmId}`
  });
};

// 8. 页面初始化：加载电影详情和演职人员
onMounted(() => {
  fetchFilmDetail();
});
</script>

<style scoped>

/* 演职人员样式：圆形头像+卡片布局 */
.cast-group {
  margin-bottom: 15px;
}

/* 角色标签（如“导演：”“主演：”） */
.cast-label {
  font-weight: bold;
  color: #666;
  margin-right: 8px;
  display: inline-block;
  width: 60px; /* 固定标签宽度，对齐更美观 */
  vertical-align: top; /* 与卡片容器顶部对齐 */
}

/* 演职人员卡片容器：横向排列，自动换行 */
.cast-card-container {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 16px; /* 卡片之间的间距 */
  vertical-align: top;
}

/* 单个演职人员卡片：垂直排列（头像+文字） */
.cast-card {
  display: flex;
  flex-direction: column;
  align-items: center; /* 水平居中 */
  width: 80px; /* 卡片宽度（与头像直径一致） */
}

/* 圆形头像容器 */
.cast-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden; /* 隐藏图片超出圆形的部分 */
  margin-bottom: 5px;
}

/* 头像图片：填充容器且不变形 */
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 保持图片比例，填充容器 */
}

/* 卡片文字区域（名字+角色） */
.cast-info {
  text-align: center; /* 文字居中 */
}

/* 演职人员名字 */
.cast-name {
  font-size: 14px;
  white-space: nowrap; /* 名字不换行 */
  width: 100%; /* 限制宽度，避免名字过长 */
}

/* 演职人员角色（如“主演”） */
.cast-role {
  font-size: 14px;
  color: #100d0f;
  font-weight: bold;
  margin-bottom: 2px;

}
</style>
