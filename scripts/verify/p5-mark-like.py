"""点赞与热评排序的真实库验证（评审专项 9）。

为什么必须打真库：本功能的 Java 逻辑已被 Mockito 覆盖，但 Mockito 测的是桩，不是谓词 ——
分页总数、赞数聚合、ORDER BY likeCount DESC 的并列裁决、匿名视角的 liked/mine、
主键对并发重复点赞的去重、外键 ON DELETE CASCADE，全都只在 MySQL 真库上才成立。
本脚本因此按 p4 的骨架在**备用端口 + 临时库**上打真实 HTTP 接口，并直接 SELECT 核对行数。

本脚本验证：
  1. 主键去重 —— 同一用户对同一评价连点两次，库里始终只有一行，两次都报 liked=true
  2. 并发幂等 —— 5 线程同时点赞同一条评价，库里只有一行，5 个响应拿到同一份真相
  3. 匿名投影 —— by-film 匿名可读，所有行 liked/mine 恒 false，且响应体里**没有 userId 键**
  4. 视角投影 —— 登录后 liked 只为自己点过的行为 true，mine 只对本人写的那一条为 true
  5. 取消赞幂等 —— liked=false 把赞数归零、删掉关系行，再点一次仍 200 且 liked=false
  6. 授权 —— 评价不存在 404 / 影院角色 403 / 未登录 401
  7. 排序 —— 赞数降序，同赞数按 id 降序（确定性并列裁决）
  8. 热评即同一查询的头部 —— pageSize=3 的前三条与 pageSize=10 的前三条 id 完全一致
  9. total 语义 —— 等于库中该片评价真实条数（含本人），且 pageSize 足够时等于 list 长度
 10. CASCADE —— ADMIN 删掉一条带赞的评价，其 mark_like 行随之消失，其余排序/总数保持一致
 11. reviewable —— 未取票 USER=false、已取票 USER=true、游客=false
 12. liked 跟随状态 —— 同一访问者：赞前 false → 赞后 true → 取消后 false

前置（与 p4-account-wallet-e2e.py 同款，不触碰开发库 xm-film 与本机 9090/5173）：

    # 1. 建临时库并导入结构与种子数据
    mysql -uroot -p --default-character-set=utf8mb4 -e "DROP DATABASE IF EXISTS xm_film_verify; \
CREATE DATABASE xm_film_verify DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    mysql -uroot -p --default-character-set=utf8mb4 xm_film_verify < xm_film/sql/schema.sql
    mysql -uroot -p --default-character-set=utf8mb4 xm_film_verify < xm_film/sql/data.sql

    # 注：data.sql 不预置放映场次/订单/评价（一律由真实接口产生），本脚本自己准备一个
    # 独占场次（单价 39.50），并自建所需 USER 账号。评价与点赞只经 POST /api/v1/marks
    # 与 PUT /api/v1/marks/{id}/like 产生，**绝不手写 mark / mark_like 行**（BUG-046 的教训）。

    # 2. 用临时库、备用端口起后端（不要动 9090）
    cd xm_film/springboot
    DB_NAME=xm_film_verify java -jar target/springboot-0.0.1-SNAPSHOT.jar --server.port=9191

    # 3. 跑本脚本（BASE 可用环境变量 E2E_BASE 覆盖），可反复运行
    python scripts/verify/p5-mark-like.py

环境变量：E2E_BASE / E2E_DB / DB_PASSWORD
"""
import json
import os
import subprocess
import sys
import threading
import urllib.error
import urllib.request
from datetime import datetime, timedelta

sys.stdout.reconfigure(encoding='utf-8')

BASE = os.environ.get('E2E_BASE', 'http://localhost:9191')
DB = os.environ.get('E2E_DB', 'xm_film_verify')
DB_PASSWORD = os.environ.get('DB_PASSWORD', '123456')

results = []

# 本机开着 HTTP_PROXY，localhost 请求必须绕过代理，否则返回 502（见 Bug.md 预防清单第 2 条）
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def sql(statement):
    subprocess.run(['mysql', '--default-character-set=utf8mb4', '-uroot', '-p' + DB_PASSWORD, DB, '-e', statement],
                   capture_output=True, check=True)


def query(statement):
    # 必须显式 encoding='utf-8'：中文 Windows 上 text=True 会按 GBK 解码（locale），
    # 而库是 utf8mb4 —— 结果含中文时解码线程抛 UnicodeDecodeError，stdout 变成 None。
    out = subprocess.run(['mysql', '-N', '-B', '--default-character-set=utf8mb4',
                          '-uroot', '-p' + DB_PASSWORD, DB, '-e', statement],
                         capture_output=True, text=True, encoding='utf-8', check=True).stdout
    return [line.split('\t') for line in out.splitlines() if line.strip()]


def call(method, path, token=None, body=None):
    data = json.dumps(body).encode('utf-8') if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header('Content-Type', 'application/json;charset=utf-8')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req) as resp:
            return resp.status, json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode('utf-8')
        try:
            return exc.code, json.loads(raw)
        except json.JSONDecodeError:
            return exc.code, {'raw': raw}


def call_raw(method, path, token=None):
    """返回原始响应文本 —— 第 3 节要在**文本**上断言 userId 键不存在，而不是只看解析后的对象。"""
    req = urllib.request.Request(BASE + path, method=method)
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with opener.open(req) as resp:
            return resp.status, resp.read().decode('utf-8')
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read().decode('utf-8')


def check(name, ok, detail=''):
    results.append((name, ok, detail))
    print(('  PASS  ' if ok else '  FAIL  ') + name + (('  -> ' + str(detail)) if detail and not ok else ''))


def ensure_user(username, password):
    """登录；账号不存在则注册再登录。返回 (token, userId)。注册的用户名为昵称、余额为 0。"""
    def login():
        _, res = call('POST', '/api/v1/auth/login',
                      body={'username': username, 'password': password, 'role': 'USER'})
        data = res.get('data') or {}
        return (data.get('token'), data.get('id')) if res.get('code') == '200' else (None, None)

    token, uid = login()
    if token:
        return token, uid
    _, reg = call('POST', '/api/v1/auth/register',
                  body={'username': username, 'password': password, 'role': 'USER'})
    if reg.get('code') != '200':
        print('  FAIL  注册用户 %s 失败: %s' % (username, reg))
        sys.exit(2)
    token, uid = login()
    if not token:
        print('  FAIL  用户 %s 注册后仍无法登录' % username)
        sys.exit(2)
    return token, uid


def ensure_record(film_id):
    """准备本脚本独占的场次（单价 39.50）；已存在可复用的就复用。座位规模需 ≥2 排 × ≥4 列。"""
    _, cinema = call('POST', '/api/v1/auth/login',
                     body={'username': 'asks', 'password': 'cinema123', 'role': 'CINEMA'})
    cinema_token = (cinema.get('data') or {}).get('token')
    if not cinema_token:
        print('  FAIL  影院账号登录失败，无法准备场次: %s' % cinema)
        sys.exit(2)
    room = query('SELECT r.id FROM room r WHERE r.seat_rows >= 2 AND r.seat_cols >= 4 '
                 "AND r.cinema_id = (SELECT id FROM cinema WHERE username = 'asks') ORDER BY r.id LIMIT 1")
    if not room:
        print('  FAIL  找不到座位规模足够的影厅，无法准备场次')
        sys.exit(2)
    room_id = int(room[0][0])
    existing = query('SELECT id FROM record WHERE room_id = %d AND film_id = %d AND price = 39.5 '
                     "AND status = '正常' AND start > NOW() ORDER BY id DESC LIMIT 1" % (room_id, film_id))
    if existing:
        return int(existing[0][0])
    start = (datetime.now() + timedelta(days=30)).strftime('%Y-%m-%d 19:30:00')
    _, res = call('POST', '/api/v1/records', cinema_token,
                  {'roomId': room_id, 'filmId': film_id, 'start': start, 'price': 39.5})
    if res.get('code') != '200':
        print('  FAIL  创建场次失败: %s' % res)
        sys.exit(2)
    rows = query("SELECT id FROM record WHERE room_id = %d AND film_id = %d AND start = '%s' "
                 'ORDER BY id DESC LIMIT 1' % (room_id, film_id, start))
    return int(rows[0][0])


def buy_and_pickup(token, record_id, seat):
    """下单 → 余额支付 → 凭取票码在免登录的取票大厅自助核销（评价门槛是「已取票」）。"""
    _, created = call('POST', '/api/v1/orders/create', token, {'recordId': record_id, 'seat': seat})
    order_id = (created.get('data') or {}).get('id')
    if created.get('code') != '200' or order_id is None:
        return None, created
    _, paid = call('PUT', '/api/v1/orders/%d/pay' % order_id, token)
    if paid.get('code') != '200':
        return order_id, paid
    _, row = call('GET', '/api/v1/orders/%d' % order_id, token)
    pickup_code = (row.get('data') or {}).get('pickupCode')
    _, redeemed = call('POST', '/api/v1/tickets/redeem', body={'code': pickup_code})
    return order_id, redeemed


def write_review(token, film_id, score, text):
    return call('POST', '/api/v1/marks', token, {'filmId': film_id, 'score': score, 'mark': text})


def mark_id_of(film_id, user_id):
    rows = query('SELECT id FROM mark WHERE film_id = %d AND user_id = %d ORDER BY id DESC LIMIT 1'
                 % (film_id, user_id))
    return int(rows[0][0]) if rows else None


def like_rows(mark_id, user_id=None):
    where = 'mark_id = %d' % mark_id
    if user_id is not None:
        where += ' AND user_id = %d' % user_id
    return int(query('SELECT COUNT(*) FROM mark_like WHERE ' + where)[0][0])


def mark_count(film_id):
    return int(query('SELECT COUNT(*) FROM mark WHERE film_id = %d' % film_id)[0][0])


def by_film(film_id, token=None, page_num=1, page_size=10):
    _, res = call('GET', '/api/v1/marks/by-film?filmId=%d&pageNum=%d&pageSize=%d'
                  % (film_id, page_num, page_size), token)
    return (res.get('data') or {}), res


def set_like(mark_id, token, liked):
    return call('PUT', '/api/v1/marks/%d/like' % mark_id, token, {'liked': liked})


def row_of(data, mark_id):
    for item in (data.get('list') or []):
        if item.get('id') == mark_id:
            return item
    return None


print('=== 0. 准备：账号 / 场次 / 取票 ===')
token_a, uid_a = ensure_user('zhangsan', 'user123')   # A：种子用户（余额 100）
token_b, uid_b = ensure_user('wangwu', 'user123')     # B：种子用户
token_c, uid_c = ensure_user('lisi', '123')           # C：种子用户
token_d, uid_d = ensure_user('p5like_d', 'user123')   # D：临时用户（本脚本注册）
token_e, uid_e = ensure_user('p5like_e', 'user123')   # E：临时用户，**不购票**，用于 reviewable=false
for name, uid in (('A=zhangsan', uid_a), ('B=wangwu', uid_b), ('C=lisi', uid_c),
                  ('D=p5like_d', uid_d), ('E=p5like_e', uid_e)):
    check('账号就绪 %s' % name, bool(uid), uid)

film_rows = query("SELECT id FROM film WHERE status = '已上映' AND time > 0 ORDER BY id LIMIT 1")
if not film_rows:
    print('  FAIL  找不到已上映且片长已知的影片，无法准备评价')
    sys.exit(2)
FILM_ID = int(film_rows[0][0])
RECORD_ID = ensure_record(FILM_ID)
check('影片就绪（id=%d，已上映）' % FILM_ID, FILM_ID > 0, FILM_ID)
check('独占场次就绪（单价 39.50）', RECORD_ID > 0, RECORD_ID)

# 状态重置，保证脚本可在同一个临时库上反复运行：清掉该片评价（级联清 mark_like）、
# 该场次订单与该批用户的流水，并把余额复位到 500.00（够买 39.50 的票）。
sql('DELETE FROM mark WHERE film_id = %d;' % FILM_ID)
sql('DELETE FROM ordered WHERE record_id = %d;' % RECORD_ID)
test_ids = ','.join(str(u) for u in (uid_a, uid_b, uid_c, uid_d, uid_e))
sql('DELETE FROM fund_flow WHERE user_id IN (%s);' % test_ids)
sql('UPDATE user SET balance = 500.00 WHERE id IN (%s);' % test_ids)

seat_assignments = [(token_a, '1排1座'), (token_b, '1排2座'), (token_c, '1排3座'), (token_d, '1排4座')]
for index, (token, seat) in enumerate(seat_assignments):
    order_id, res = buy_and_pickup(token, RECORD_ID, seat)
    check('用户%s 购票并取票成功（座位 %s）' % ('ABCD'[index], seat),
          res.get('code') == '200', res)

print('=== 1. 发表评价（真实接口，一人一片一条） ===')
review_specs = [(token_a, uid_a, 8.0, 'A 的评价：值得一看'),
                (token_b, uid_b, 7.5, 'B 的评价：中规中矩'),
                (token_c, uid_c, 9.0, 'C 的评价：非常喜欢'),
                (token_d, uid_d, 6.5, 'D 的评价：勉强及格')]
review_ids = []
for index, (token, uid, score, text) in enumerate(review_specs):
    _, res = write_review(token, FILM_ID, score, text)
    check('用户%s 发表评价成功' % 'ABCD'[index], res.get('code') == '200', res)
    review_ids.append(mark_id_of(FILM_ID, uid))
rA, rB, rC, rD = review_ids
check('四条评价都落库且 id 互不相同',
      None not in review_ids and len(set(review_ids)) == 4, review_ids)
check('评价 id 递增（A<B<C<D，供并列裁决观察）',
      None not in review_ids and review_ids == sorted(review_ids), review_ids)

_, gate = write_review(token_e, FILM_ID, 9.0, 'E 没买票也想评价')
check('未取票用户发表评价被拒（服务端门槛，409）', gate.get('code') == '409', gate)

print('=== 2. 主键去重：同一用户重复点赞只留一行 ===')
_, like1 = set_like(rB, token_a, True)
_, like2 = set_like(rB, token_a, True)
check('第一次点赞返回 liked=true', (like1.get('data') or {}).get('liked') is True, like1)
check('第一次点赞返回 likeCount=1', (like1.get('data') or {}).get('likeCount') == 1, like1)
check('重复点赞仍返回 200 且 liked=true', like2.get('code') == '200'
      and (like2.get('data') or {}).get('liked') is True, like2)
check('重复点赞返回 likeCount 仍为 1（回读权威状态）',
      (like2.get('data') or {}).get('likeCount') == 1, like2)
check('库中 (评价B, 用户A) 的关系行恰为 1 行', like_rows(rB, uid_a) == 1, like_rows(rB, uid_a))
check('库中评价B 的赞数恰为 1', like_rows(rB) == 1, like_rows(rB))

print('=== 3. 并发：5 线程同时点赞同一条评价 ===')
outcomes = []
barrier = threading.Barrier(5)


def like_same_review():
    barrier.wait()
    _, body = call('PUT', '/api/v1/marks/%d/like' % rA, token_b, {'liked': True})
    # 线程完成顺序不确定，必须把响应体整体成对记录，不能靠 append 顺序对齐
    outcomes.append(body)


threads = [threading.Thread(target=like_same_review) for _ in range(5)]
for thread in threads:
    thread.start()
for thread in threads:
    thread.join()

check('5 个并发点赞响应全部为 200', all(o.get('code') == '200' for o in outcomes), outcomes)
check('库中 (评价A, 用户B) 的关系行恰为 1 行（主键去重）', like_rows(rA, uid_b) == 1, like_rows(rA, uid_b))
check('库中评价A 的赞数恰为 1', like_rows(rA) == 1, like_rows(rA))
# 下面两条断言的是 setLike 自己写明的契约（MarkLikeResult 类注释：两个字段是"写库后回读的
# 权威状态"、"并发下两个请求也应拿到同一份真相"）。它们会红 —— 见脚本末尾的说明。
check('5 个并发响应全部报 liked=true（写库后回读的权威状态）',
      all((o.get('data') or {}).get('liked') is True for o in outcomes), outcomes)
check('5 个并发响应拿到同一份真相 likeCount=1',
      all((o.get('data') or {}).get('likeCount') == 1 for o in outcomes), outcomes)
# 反证：写入是持久的，陈旧的只是事务内的回读。新起一次请求（新事务）就能读到真相。
b_fresh, _ = by_film(FILM_ID, token_b)
check('事务结束后重新拉取，B 视角评价A 行 liked=true（写入已持久化）',
      (row_of(b_fresh, rA) or {}).get('liked') is True, row_of(b_fresh, rA))

_, c_like = set_like(rA, token_c, True)
check('用户C 再点赞评价A → 赞数 2', (c_like.get('data') or {}).get('likeCount') == 2, c_like)

print('=== 4. 匿名投影：liked/mine 恒 false，响应体不含 userId ===')
status, raw = call_raw('GET', '/api/v1/marks/by-film?filmId=%d&pageNum=1&pageSize=10' % FILM_ID)
anon = json.loads(raw)
anon_data = anon.get('data') or {}
anon_list = anon_data.get('list') or []
check('匿名 by-film 可读（HTTP 200）', status == 200, status)
check('匿名返回了 4 条评价', len(anon_list) == 4, len(anon_list))
check('匿名所有行 liked=false', all(r.get('liked') is False for r in anon_list), anon_list)
check('匿名所有行 mine=false', all(r.get('mine') is False for r in anon_list), anon_list)
check('匿名响应体不含 "userId" 键（原始文本断言）', '"userId"' not in raw, raw[:400])
check('匿名 my 为 null', anon_data.get('my') is None, anon_data.get('my'))

print('=== 5. 视角投影：liked / mine 按访问者计算 ===')
a_data, _ = by_film(FILM_ID, token_a)
check('A 点过的评价B 行 liked=true', (row_of(a_data, rB) or {}).get('liked') is True, row_of(a_data, rB))
check('A 未点过的评价C 行 liked=false', (row_of(a_data, rC) or {}).get('liked') is False, row_of(a_data, rC))
mine_rows = [r.get('id') for r in (a_data.get('list') or []) if r.get('mine') is True]
check('A 视角下 mine=true 恰好只有 A 自己写的那一条', mine_rows == [rA], mine_rows)
check('A 视角下他人评价B 的 mine=false', (row_of(a_data, rB) or {}).get('mine') is False, row_of(a_data, rB))
check('A 视角 my 指向本人评价', (a_data.get('my') or {}).get('id') == rA, a_data.get('my'))

print('=== 6. 取消赞幂等 + liked 跟随状态（访问者 D 对本人评价 D） ===')
d_before, _ = by_film(FILM_ID, token_d)
check('赞前 D 视角 liked=false', (row_of(d_before, rD) or {}).get('liked') is False, row_of(d_before, rD))

_, d_on = set_like(rD, token_d, True)
check('点赞后 liked=true', (d_on.get('data') or {}).get('liked') is True, d_on)
check('点赞后 likeCount=1', (d_on.get('data') or {}).get('likeCount') == 1, d_on)
check('点赞后库中关系行为 1', like_rows(rD, uid_d) == 1, like_rows(rD, uid_d))

_, d_off = set_like(rD, token_d, False)
check('取消赞返回 200', d_off.get('code') == '200', d_off)
check('取消赞后 liked=false', (d_off.get('data') or {}).get('liked') is False, d_off)
check('取消赞后 likeCount=0', (d_off.get('data') or {}).get('likeCount') == 0, d_off)
check('取消赞后库中关系行归零', like_rows(rD, uid_d) == 0, like_rows(rD, uid_d))

_, d_off2 = set_like(rD, token_d, False)
check('重复取消赞仍返回 200 且 liked=false', d_off2.get('code') == '200'
      and (d_off2.get('data') or {}).get('liked') is False, d_off2)

d_after, _ = by_film(FILM_ID, token_d)
check('取消后 D 视角 liked 回到 false', (row_of(d_after, rD) or {}).get('liked') is False, row_of(d_after, rD))

print('=== 7. 授权边界 ===')
_, cinema_login = call('POST', '/api/v1/auth/login',
                       body={'username': 'asks', 'password': 'cinema123', 'role': 'CINEMA'})
cinema_token = (cinema_login.get('data') or {}).get('token')
check('影院账号登录成功', bool(cinema_token), cinema_login)

status_404, body_404 = set_like(99999999, token_a, True)
check('点赞不存在的评价 → 404', body_404.get('code') == '404', (status_404, body_404))

status_403, body_403 = set_like(rB, cinema_token, True)
check('影院角色点赞 → 403', body_403.get('code') == '403', (status_403, body_403))

status_401, body_401 = set_like(rB, None, True)
check('未登录点赞 → 401', status_401 == 401 and body_401.get('code') == '401',
      (status_401, body_401))

_, film_404 = by_film(99999999)
check('查询不存在影片的评价列表 → 404', (film_404.get('code') == '404'), film_404)

print('=== 8. 排序：赞数降序 → id 降序 ===')
order_data, _ = by_film(FILM_ID, page_size=10)
order_ids = [r.get('id') for r in (order_data.get('list') or [])]
order_counts = [r.get('likeCount') for r in (order_data.get('list') or [])]
check('赞数分布已就位（A=2 / B=1 / C=0 / D=0）', order_counts == [2, 1, 0, 0], order_counts)
check('排序为 A → B → D → C（同赞数按 id 降序）', order_ids == [rA, rB, rD, rC], order_ids)
check('两条零赞评价中 id 更大的 D 排在 C 之前（确定性并列裁决）',
      order_ids.index(rD) < order_ids.index(rC) and rC < rD, (rC, rD, order_ids))

print('=== 9. 热评即同一查询的头部 ===')
top3_data, _ = by_film(FILM_ID, page_size=3)
top3_ids = [r.get('id') for r in (top3_data.get('list') or [])]
check('pageSize=3 的前三条 id 与 pageSize=10 的前三条完全一致',
      top3_ids == order_ids[:3], (top3_ids, order_ids[:3]))

print('=== 10. total 语义 ===')
check('total 等于库中该片评价真实条数（含本人）', order_data.get('total') == mark_count(FILM_ID),
      (order_data.get('total'), mark_count(FILM_ID)))
check('pageSize 足够大时 total 等于 list 长度', order_data.get('total') == len(order_data.get('list') or []),
      (order_data.get('total'), len(order_data.get('list') or [])))

print('=== 11. reviewable：够格发表 = 对该片已取票 ===')
guest_data, _ = by_film(FILM_ID)
check('游客 reviewable=false', guest_data.get('reviewable') is False, guest_data.get('reviewable'))
a_reviewable, _ = by_film(FILM_ID, token_a)
check('已取票 USER(A) reviewable=true', a_reviewable.get('reviewable') is True, a_reviewable.get('reviewable'))
e_reviewable, _ = by_film(FILM_ID, token_e)
check('未取票 USER(E) reviewable=false', e_reviewable.get('reviewable') is False, e_reviewable.get('reviewable'))

print('=== 12. CASCADE：删评价带走其点赞关系 ===')
_, admin_login = call('POST', '/api/v1/auth/login',
                      body={'username': '999', 'password': '999', 'role': 'ADMIN'})
admin_token = (admin_login.get('data') or {}).get('token')
check('管理员登录成功', bool(admin_token), admin_login)
check('删除前评价A 有 2 行点赞关系', like_rows(rA) == 2, like_rows(rA))

_, deleted = call('DELETE', '/api/v1/marks/%d' % rA, admin_token)
check('ADMIN 删除他人带赞评价成功', deleted.get('code') == '200', deleted)
check('评价A 的点赞关系行随外键级联清空（0 行）', like_rows(rA) == 0, like_rows(rA))

after_data, _ = by_film(FILM_ID, page_size=10)
after_ids = [r.get('id') for r in (after_data.get('list') or [])]
check('删除后 total 变为 3', after_data.get('total') == 3, after_data.get('total'))
check('删除后库中该片评价为 3 条', mark_count(FILM_ID) == 3, mark_count(FILM_ID))
check('删除后剩余排序仍为 B → D → C', after_ids == [rB, rD, rC], after_ids)
check('删除后剩余赞数仍为 1 / 0 / 0',
      [r.get('likeCount') for r in (after_data.get('list') or [])] == [1, 0, 0],
      [r.get('likeCount') for r in (after_data.get('list') or [])])

print()
passed = sum(1 for _, ok, _ in results if ok)
total = len(results)
print('===== 结果: %d/%d 通过 =====' % (passed, total))
for name, ok, detail in results:
    if not ok:
        print('  FAILED: %s -> %s' % (name, detail))
sys.exit(0 if passed == total else 1)
