<template>
  <div class="film-marks">
    <!-- 1. 加载状态提示 -->
    <div v-if="loading" class="page-hint page-hint--loading">
      正在加载影评...
    </div>

    <!-- 2. 错误提示（影片 404 与请求失败同一文案，见规范 §11.2） -->
    <div v-else-if="errorMsg" class="page-hint page-hint--error">
      <div>{{ errorMsg }}</div>
      <el-button class="page-hint__action" type="primary" @click="goBackToFilmList">返回影片列表</el-button>
    </div>

    <!-- 3. 内容区 -->
    <div v-else>
      <!-- 3.1 影片头部（深色横幅，与影片/影院详情页同款，规范 §3.5） -->
      <div class="marks-hero">
        <div class="marks-hero__inner">
          <img :src="film.img" :alt="`${film.title}的海报`" class="marks-hero__img">
          <div class="marks-hero__info">
            <div class="marks-hero__title">{{ film.title }}</div>
            <div class="marks-hero__score">影片口碑 {{ film.score }} 分</div>
          </div>
        </div>
      </div>

      <!-- 3.2 评价区 -->
      <section class="marks-section">
        <div class="marks-toolbar">
          <h3 class="marks-title">
            全部评价
            <span class="marks-title__count">（{{ marks.total }} 条）</span>
          </h3>
          <!-- 只渲染点得动的控件：后台角色评价必被 403 拦，因此干脆不出现写入口 -->
          <div v-if="!isBackOffice" class="marks-toolbar__action">
            <el-button v-if="my" type="primary" @click="openEdit">修改评价</el-button>
            <el-button v-else-if="isGuest" type="primary" @click="promptLogin">写评价</el-button>
            <el-button v-else-if="reviewable" type="primary" @click="openCreate">写评价</el-button>
            <span v-else class="marks-toolbar__hint">取票后才能评价</span>
          </div>
        </div>

        <!-- 列表三态：加载 / 失败（文案固定且与空态区分，§11.2） / 空 -->
        <div v-if="listLoading" class="marks-hint">正在加载评价...</div>
        <div v-else-if="listError" class="marks-hint marks-hint--error">数据加载失败，请稍后重试</div>
        <div v-else-if="!marks.list.length" class="marks-hint">暂无数据</div>
        <div v-else class="marks-list">
          <div v-for="item in marks.list" :key="item.id" class="mark-item">
            <el-avatar class="mark-item__avatar" :size="40" :src="item.avatar || ''"
                       :alt="`${item.userName || '匿名用户'}的头像`">
              {{ initial(item) }}
            </el-avatar>
            <div class="mark-item__main">
              <div class="mark-item__head">
                <span class="mark-item__user">{{ item.userName || '匿名用户' }}</span>
                <el-tag v-if="item.mine" class="mark-item__badge" size="small" type="primary">我的</el-tag>
                <span class="mark-item__score">{{ item.score }} 分</span>
              </div>
              <div v-if="item.mark" class="mark-item__text">{{ item.mark }}</div>
            </div>
            <div class="mark-item__actions">
              <!-- 自己那条只给「修改」：自己赞自己没有意义 -->
              <el-button v-if="item.mine" link type="primary" @click="openEdit">修改</el-button>
              <el-button
                  v-else-if="!isBackOffice"
                  link
                  class="mark-item__like"
                  :class="{ 'mark-item__like--on': item.liked }"
                  :aria-pressed="String(!!item.liked)"
                  :disabled="togglingId === item.id"
                  @click="onLike(item)"
              >
                {{ item.liked ? '已赞' : '赞' }} {{ item.likeCount ?? 0 }}
              </el-button>
            </div>
          </div>
        </div>

        <div v-if="marks.total > marks.pageSize" class="marks-pagination">
          <el-pagination
              @size-change="handleSizeChange"
              @current-change="handlePageChange"
              :current-page="marks.pageNum"
              :page-sizes="[5, 10, 15]"
              :page-size="marks.pageSize"
              layout="total, sizes, prev, pager, next"
              :total="marks.total"
          />
        </div>
      </section>
    </div>

    <!-- 4. 写 / 改评价弹窗（与购票记录页原来是同一个表单，现收在这里） -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '修改评价' : '发表评价'" width="480" destroy-on-close>
      <el-form label-width="70px" status-icon @submit.prevent>
        <el-form-item label="影片">
          <span>{{ film.title }}</span>
        </el-form-item>
        <el-form-item label="评分" prop="score">
          <el-input-number v-model="form.score" :min="0" :max="10" :step="0.1" :precision="1"
                           class="field-full" @keyup.enter="submit"/>
        </el-form-item>
        <el-form-item label="评语" prop="mark">
          <el-input v-model="form.mark" type="textarea" :rows="3" maxlength="255" show-word-limit
                    placeholder="说说你的观后感（选填）"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">保 存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '@/utils/request.js';
import { API_PATHS, MARK_API, apiById } from '@/constants';
import { useAuth } from '@/composables/useAuth';

const route = useRoute();
const router = useRouter();
const { user, isAdmin, isCinema } = useAuth();

// 路由参数（影片ID）
const filmId = route.params.id;

// 「取不到数据」的统一文案；失败提示由 request.js 的拦截器弹出，页面只落错误态（§11.2）
const LOAD_FAILED = '数据加载失败，请稍后重试';

const loading = ref(false);       // 整页三态外壳（同时覆盖影片与评价两条请求）
const listLoading = ref(false);   // 仅列表（翻页时重取的加载态）
const errorMsg = ref('');
const listError = ref(false);     // 列表重取失败

const film = reactive({ id: '', title: '', img: '', score: 0 });
const marks = reactive({ list: [], pageNum: 1, pageSize: 10, total: 0 });
const reviewable = ref(false);    // 够格发表（对该片有已取票订单），是否已评过看 my
const my = ref(null);             // 本人对该片的评价

// 角色可见性：后台两角色评价必被后端 403 拦，因此一律不渲染写/点赞入口
const isBackOffice = computed(() => isAdmin.value || isCinema.value);
const isGuest = computed(() => !user.value?.role);

const goBackToFilmList = () => {
  router.push('/front/movie');
};

const initial = (item) => (item.userName || '匿名用户').charAt(0);

const loadFilm = async () => {
  const res = await request.get(apiById(API_PATHS.FILMS, filmId));
  if (res.code !== '200' || !res.data) {
    throw new Error('影片不存在');
  }
  const data = res.data;
  Object.assign(film, {
    id: data.id,
    title: data.title?.trim() || '未知电影',
    img: data.img || '',
    score: data.score || 0,
  });
};

const loadMarks = async () => {
  const res = await request.get(MARK_API.BY_FILM, {
    params: { filmId, pageNum: marks.pageNum, pageSize: marks.pageSize },
  });
  if (res.code !== '200' || !res.data) {
    throw new Error('评价加载失败');
  }
  marks.total = res.data.total || 0;
  marks.list = res.data.list || [];
  reviewable.value = !!res.data.reviewable;
  my.value = res.data.my || null;
};

const load = async () => {
  if (!filmId || Number.isNaN(Number(filmId))) {
    errorMsg.value = LOAD_FAILED;
    return;
  }
  loading.value = true;
  errorMsg.value = '';
  listError.value = false;
  try {
    await Promise.all([loadFilm(), loadMarks()]);
  } catch (error) {
    errorMsg.value = LOAD_FAILED;
  } finally {
    loading.value = false;
  }
};

// 翻页只重取列表，影片头部不动
const refetchMarks = async () => {
  listLoading.value = true;
  listError.value = false;
  try {
    await loadMarks();
  } catch (error) {
    listError.value = true;
  } finally {
    listLoading.value = false;
  }
};

const handlePageChange = (pageNum) => {
  marks.pageNum = pageNum;
  refetchMarks();
};

const handleSizeChange = (pageSize) => {
  marks.pageSize = pageSize;
  marks.pageNum = 1;
  refetchMarks();
};

// 游客引导登录：与路由守卫同一套确认框 + 回跳（redirect 用当前完整路径）
const promptLogin = () => {
  ElMessageBox.confirm('请先登录后再进行此操作', '提示', {
    confirmButtonText: '去登录',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`);
  }).catch(() => {});
};

// 点赞：liked 是显式意图（不是本地取反后直接改数字），成功后把响应里的权威状态写回该行
const togglingId = ref(null);

const onLike = async (item) => {
  if (isGuest.value) {
    promptLogin();
    return;
  }
  if (togglingId.value !== null) return;
  togglingId.value = item.id;
  try {
    const res = await request.put(MARK_API.LIKE(item.id), { liked: !item.liked });
    if (res.code === '200' && res.data) {
      item.liked = !!res.data.liked;
      item.likeCount = res.data.likeCount;
    }
  } catch (error) {
    // request.js 已弹出后端消息，这里不重复提示
  } finally {
    togglingId.value = null;
  }
};

// 评价弹窗
const dialogVisible = ref(false);
const submitting = ref(false);
const form = reactive({ id: undefined, score: 8, mark: '' });

const openCreate = () => {
  form.id = undefined;
  form.score = 8;
  form.mark = '';
  dialogVisible.value = true;
};

const openEdit = () => {
  if (!my.value) return;
  form.id = my.value.id;
  form.score = my.value.score ?? 8;
  form.mark = my.value.mark || '';
  dialogVisible.value = true;
};

const submit = async () => {
  if (form.score === null || form.score === undefined) {
    ElMessage.warning('请先给出评分');
    return;
  }
  submitting.value = true;
  try {
    const payload = { score: form.score, mark: form.mark };
    const res = form.id
        ? await request.put(API_PATHS.MARKS, { id: form.id, ...payload })
        : await request.post(API_PATHS.MARKS, { filmId: Number(filmId), ...payload });
    if (res.code === '200') {
      ElMessage.success(form.id ? '评价已更新' : '评价发表成功');
      dialogVisible.value = false;
      // 影片均分与「我的评价」都会变，两条请求一起重取
      await Promise.allSettled([loadFilm(), loadMarks()]);
    } else {
      ElMessage.error(res.msg || '保存失败');
    }
  } catch (error) {
    // request.js 已弹出后端消息，这里不重复提示
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  load();
});
</script>

<style scoped>
.film-marks {
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

/* ---------- 3.1 影片头部（深色横幅，规范 §3.5） ---------- */
.marks-hero {
  background-color: var(--dark-bg-hero);
}

.marks-hero__inner {
  display: flex;
  align-items: center;
  gap: var(--space-20);
  width: 60%;
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-20) 0;
}

.marks-hero__img {
  width: 120px;
  height: 160px;
  border-radius: var(--el-border-radius-base);
  object-fit: cover;
}

.marks-hero__info {
  color: var(--dark-text);
}

.marks-hero__title {
  font-size: var(--fs-2xl);
  font-weight: var(--fw-bold);
}

/* 深底上的评分金（12.41:1 以上，规范 §3.6 允许用于深底/图片叠加） */
.marks-hero__score {
  margin-top: var(--space-12);
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--color-rating);
}

/* ---------- 3.2 评价区 ---------- */
.marks-section {
  width: 60%;
  max-width: 1200px;
  margin: var(--space-32) auto 0;
}

.marks-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-16);
}

/* 区块标题：左侧品牌红竖条（装饰，非文字），与影片详情页同构 */
.marks-title {
  margin: 0;
  padding-left: var(--space-12);
  border-left: 4px solid var(--color-brand);
  font-size: var(--fs-lg);
  font-weight: var(--fw-bold);
  color: var(--el-text-color-primary);
}

.marks-title__count {
  font-size: var(--fs-base);
  font-weight: var(--fw-regular);
  color: var(--el-text-color-regular);
}

.marks-toolbar__action {
  flex: 0 0 auto;
}

.marks-toolbar__hint {
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
}

.marks-pagination {
  margin-top: var(--space-20);
  text-align: center;
}
</style>
