<template>
  <div class="film-detail">
    <!-- 1. 加载状态提示 -->
    <div v-if="loading" class="page-hint page-hint--loading">
      正在加载电影详情...
    </div>

    <!-- 2. 错误提示 -->
    <div v-else-if="errorMsg" class="page-hint page-hint--error">
      <div>{{ errorMsg }}</div>
      <el-button class="page-hint__action" type="primary" @click="goBackToList">返回影片列表</el-button>
    </div>

    <!-- 3. 电影内容区域 -->
    <div v-else>
      <!-- 3.1 电影头部信息区域 -->
      <div class="film-hero">
        <div class="film-hero__inner">
          <!-- 电影海报 -->
          <div>
            <img :src="film.img" alt="电影海报" class="film-hero__img">
          </div>

          <!-- 电影基本信息 -->
          <div class="film-hero__info">
            <div class="film-hero__title">{{ film.title || '未知电影' }}</div>
            <div class="film-hero__meta">{{ film.english || '无英文标题' }}</div>
            <div class="film-hero__meta">{{ film.types.join(' / ') || '未知类型' }}</div>
            <div class="film-hero__meta">{{ film.area || '未知地区' }} / {{ film.time || '未知时长' }}</div>
            <div class="film-hero__meta">{{ film.language || '未知语言' }} / {{ film.resolution || '未知格式' }}</div>
            <div class="film-hero__meta">{{ film.start || '未知上映时间' }} 开始上映</div>
            <!-- 新增：显示单个演员的基础信息（从film.actorInfo获取） -->
            <div class="film-hero__meta" v-if="film.actorInfo">
              主演：{{ film.actorInfo.split(':')[1] || '未知演员' }}
            </div>

            <el-button
                class="film-hero__action"
                :disabled="film.status !== '已上映'"
                @click="goToFilmCinema(film.id)"
            >
              {{ film.status === '已上映' ? '立即购票' : '暂未上映' }}
            </el-button>
          </div>

          <!-- 评分和票房 -->
          <div class="film-hero__stats">
            <div>
              <div>影片口碑</div>
              <div class="film-hero__stat-value">{{ formatScore(film.score) }}</div>
            </div>
            <div class="film-hero__stat-spacer">
              <div>累计票房</div>
              <div class="film-hero__stat-value">{{ formatBoxOffice(film.boxOffice) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3.2 电影详细信息区域 -->
      <div class="detail-section">
        <div class="detail-panel">
          <!-- 左侧信息区 -->
          <div class="detail-panel__main">
            <h3 class="section-title">剧情简介</h3>
            <p class="section-text section-text--indent">
              {{ film.intro || '暂无剧情简介' }}
            </p>

            <!-- 演职人员区域（核心修改：适配单个actorId+多角色区分） -->
            <h3 class="section-title section-title--spaced">演职人员</h3>
            <div v-if="loadingCast" class="cast-hint">
              正在加载演职人员信息...
            </div>
            <div v-else-if="castError" class="cast-hint cast-hint--error">
              {{ castError }}
            </div>
            <!-- 演职人员列表展示（按角色分类，圆形头像+名字布局） -->
            <div v-else class="cast-body">
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
              <div class="cast-group cast-group--spaced" v-if="actorList.length > 0">

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
              <div class="cast-group cast-group--spaced" v-if="screenwriterList.length > 0">

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
              <div class="cast-group cast-group--spaced" v-if="supportActorList.length > 0">

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
                   class="cast-empty">
                暂无演职人员信息
              </div>
            </div>

            <h3 class="section-title section-title--spaced">出品信息</h3>
            <p class="section-text">
              {{ film.production || '暂无出品方介绍' }}
            </p>
          </div>

          <!-- 右侧视频区 -->
          <div class="detail-panel__aside">
            <h3 class="section-title">预告视频</h3>
            <!-- 关键修改：用video标签替代div，实现视频播放 -->
            <div class="video-frame">
              <!-- 有视频URL时显示播放器 -->
              <video v-if="film.video" :src="film.video" controls class="video-player">
                <track kind="captions" src="/subtitles/trailer-zh.vtt" srclang="zh" label="中文字幕" />
                您的浏览器不支持HTML5视频播放，请升级浏览器。
              </video>
              <!-- 无视频URL时显示提示 -->
              <div v-else class="video-placeholder">
                暂无预告视频
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3.3 用户热评：与影评页同一条「赞数降序」查询，这里只取前 3 条，不是第二条排序 -->
      <div class="marks-section">
        <h3 class="section-title">
          用户热评
          <span class="section-title__count">（{{ marksTotal }} 条）</span>
        </h3>
        <div v-if="loadingMarks" class="marks-hint">正在加载评价...</div>
        <div v-else-if="marksError" class="marks-hint marks-hint--error">数据加载失败，请稍后重试</div>
        <div v-else-if="!hotMarks.length" class="marks-hint">暂无数据</div>
        <template v-else>
          <div class="marks-list">
            <div v-for="item in hotMarks" :key="item.id" class="mark-item">
              <div class="mark-item__main">
                <div class="mark-item__head">
                  <span class="mark-item__user">{{ item.userName || '匿名用户' }}</span>
                  <span class="mark-item__score">{{ item.score }} 分</span>
                </div>
                <div v-if="item.mark" class="mark-item__text">{{ item.mark }}</div>
                <div class="mark-item__meta">赞 {{ item.likeCount ?? 0 }}</div>
              </div>
            </div>
          </div>
          <el-button class="marks-section__more" @click="goToFilmMarks">
            查看全部 {{ marksTotal }} 条评价
          </el-button>
        </template>
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
import { API_PATHS, MARK_API, apiById } from '@/constants';
import { formatBoxOffice, formatScore } from '@/utils/format.js';


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
  score: null,
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

// 4. 计算属性（按角色类型筛选演职人员，核心修改）
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

// 5. 核心接口请求函数（核心修改：适配单个actorId查询）
/**
 * 请求电影详情数据
 * 作用：获取电影基础信息+单个actorId+actorInfo
 */
const goBackToList = () => {
  router.push('/front/movie');
}

const fetchFilmDetail = () => {
  if (!filmId || Number.isNaN(Number(filmId))) {
    errorMsg.value = '电影ID无效，请返回列表重试';
    return;
  }
  loading.value = true;
  errorMsg.value = '';
  request.get(apiById(API_PATHS.FILMS, filmId)).then(res => {
        if (res.code === '200' && res.data) {
          const data = res.data;
          // 电影数据映射
          Object.assign(film, {
            id: data.id,
            title: data.title?.trim() || '未知电影',
            img: data.img || '默认海报地址（可选）',
            score: data.score,
            start: data.start || '未知上映时间',
            types: (data.typeList || []).map(t => t.title),
            area: data.areaName || '未知地区',
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
 * 按单个actorId查询演职人员详情
 * 逻辑：根据单个演员ID，获取该演员的角色类型（导演/主演等）
 */
const fetchCastBySingleId = (actorId) => {
  // 前置校验：演员ID无效时直接提示
  if (!actorId || Number.isNaN(Number(actorId))) {
    castError.value = '演员ID无效，无法加载演职人员信息';
    return;
  }

  loadingCast.value = true;
  castError.value = '';
  castList.value = []; // 清空历史数据

  // 调用后端接口：按单个演员ID查询详情（含角色类型）
  request.get(apiById(API_PATHS.ACTORS, actorId))
      .then(res => {
        if (res.code === '200' && res.data) {
          const actorData = res.data;
          // 验证演员数据是否包含必要字段
          if (actorData.id && actorData.actorName) {
            // 将单个演员信息转为数组，统一存入castList
            castList.value = [
              {
                id: actorData.id,
                actor: actorData.actorName, // 演员/导演名称（actor表的actor列）
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

// 6. 用户热评（公开数据）：by-film 这条查询按赞数降序，取前 3 条即热评；
//    完整列表与发表/点赞都在影评页 FilmMarks.vue，这里只做只读展示。
const hotMarks = ref([]);     // 热评前 3 条
const marksTotal = ref(0);    // 该片评价总数（含本人，由后端给出）
const loadingMarks = ref(false);
const marksError = ref(false);

const fetchMarks = () => {
  if (!filmId || Number.isNaN(Number(filmId))) return;
  loadingMarks.value = true;
  marksError.value = false;
  request.get(MARK_API.BY_FILM, { params: { filmId, pageNum: 1, pageSize: 3 } })
      .then(res => {
        if (res.code === '200' && res.data) {
          hotMarks.value = res.data.list || [];
          marksTotal.value = res.data.total || 0;
        } else {
          marksError.value = true;
        }
      })
      .catch(() => {
        marksError.value = true;
        hotMarks.value = [];
      })
      .finally(() => {
        loadingMarks.value = false;
      });
};

const goToFilmMarks = () => {
  // 本仓库的路由一律没有 name（meta.name 是标题文案），导航一律走 path ——
  // 写 { name: 'filmMarks' } 会在运行时抛 "No match for ..."，编译与构建都查不出来
  router.push(`/front/filmMarks/${filmId}`);
};

// 7. 页面初始化：加载电影详情、演职人员与评价
onMounted(() => {
  fetchFilmDetail();
  fetchMarks();
});
</script>

<style scoped>
.film-detail {
  width: 100%;
  margin: 0 auto;
}

/* 加载 / 错误提示 */
.page-hint {
  text-align: center;
  color: var(--el-text-color-regular);
}

.page-hint--loading {
  padding: var(--space-48);
}

.page-hint--error {
  padding: var(--space-20);
  color: var(--el-color-danger);
}

.page-hint__action {
  margin-top: var(--space-16);
}

/* ---------- 3.1 电影头部（深色表面，规范 §3.5） ---------- */
.film-hero {
  background-color: var(--dark-bg-hero);
}

.film-hero__inner {
  display: flex;
  width: 60%;
  max-width: 1200px;
  margin: 0 auto;
}

.film-hero__img {
  width: 250px;
  height: 300px;
  object-fit: cover;
}

.film-hero__info {
  flex: 2;
  margin-top: var(--space-16);
  margin-left: var(--space-20);
  color: var(--dark-text);
}

.film-hero__title {
  font-size: var(--fs-2xl);
  font-weight: var(--fw-bold);
}

.film-hero__meta {
  margin: var(--space-4) 0;
}

/* 深底上的 CTA：实底主色 + 白字（白字压 #BF352D 为 5.58:1） */
.film-hero__action {
  width: 70%;
  height: 45px;
  margin-top: var(--space-20);
  border: none;
  background-color: var(--el-color-primary);
  color: var(--color-on-accent);
  font-size: var(--fs-lg);
}

.film-hero__stats {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  color: var(--dark-text);
  text-align: center;
}

.film-hero__stat-value {
  margin: var(--space-12) 0;
  font-size: var(--fs-3xl);
}

.film-hero__stat-spacer {
  margin-top: var(--space-32);
}

/* ---------- 3.2 详细信息 ---------- */
.detail-section {
  min-height: 400px;
  padding: var(--space-32) 0;
  background-color: var(--el-fill-color-lighter);
}

.detail-panel {
  display: flex;
  width: 60%;
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-20);
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.detail-panel__main {
  flex: 2;
}

.detail-panel__aside {
  flex: 1;
  margin-left: var(--space-40);
}

/* 区块标题：左侧品牌红竖条（装饰，非文字） */
.section-title {
  margin: 0;
  padding-left: var(--space-12);
  border-left: 4px solid var(--color-brand);
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

.section-title--spaced {
  margin-top: var(--space-32);
}

.section-title__count {
  font-size: var(--fs-base);
  font-weight: var(--fw-regular);
  color: var(--el-text-color-regular);
}

.section-text {
  margin: var(--space-16) 0 0;
  color: var(--el-text-color-primary);
  line-height: var(--lh-loose);
  white-space: pre-line;
}

.section-text--indent {
  text-indent: 2em;
}

.cast-hint {
  margin-top: var(--space-16);
  padding: var(--space-20);
  color: var(--el-text-color-regular);
  text-align: center;
}

.cast-hint--error {
  color: var(--el-color-danger);
}

.cast-body {
  margin-top: var(--space-16);
}

.cast-empty {
  margin-top: var(--space-16);
  padding: var(--space-12);
  color: var(--el-text-color-regular);
}

.video-frame {
  margin-top: var(--space-16);
  border-radius: var(--el-border-radius-base);
  overflow: hidden;
}

.video-player {
  width: 100%;
  height: 200px;
  object-fit: cover;
  background-color: var(--dark-bg-video);
}

.video-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  border-radius: var(--el-border-radius-base);
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  line-height: var(--lh-loose);
}

/* ---------- 3.3 用户热评 ---------- */
/* 条目样式（.marks-list / .marks-hint / .mark-item*）已收进 front-pages.scss：
 * 热评与影评页共用一份，不再两处各写一套。这里只留本页专有的区块布局。 */
.marks-section {
  width: 60%;
  max-width: 1200px;
  margin: var(--space-32) auto 0;
}

.marks-section__more {
  margin-top: var(--space-16);
}

/* 演职人员样式：圆形头像+卡片布局 */
.cast-group {
  margin-bottom: var(--space-16);
}

.cast-group--spaced {
  margin-top: var(--space-20);
}

/* 演职人员卡片容器：横向排列，自动换行 */
.cast-card-container {
  display: inline-flex;
  flex-wrap: wrap;
  gap: var(--space-16);
  vertical-align: top;
}

/* 单个演职人员卡片：垂直排列（头像+文字） */
.cast-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 80px;
}

/* 圆形头像容器 */
.cast-avatar {
  width: 60px;
  height: 60px;
  margin-bottom: var(--space-4);
  border-radius: var(--el-border-radius-circle);
  overflow: hidden;
}

/* 头像图片：填充容器且不变形 */
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cast-info {
  text-align: center;
}

.cast-name {
  width: 100%;
  font-size: var(--fs-base);
  white-space: nowrap;
}

.cast-role {
  margin-bottom: var(--space-4);
  font-size: var(--fs-base);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}
</style>

