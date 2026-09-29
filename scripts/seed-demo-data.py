"""演示数据生成脚本 —— 全部经真实业务接口，落到真实的开发库。

为什么不用 SQL 直接 INSERT：
  订单是「一整套自洽的数据」——订单号、单价快照、支付凭证、余额扣减、资金流水
  任何一处对不上就是能被查出的假数据（旧 data.sql 种子正是如此：unit_price 为 NULL、
  fund_flow 里没有对应购票流水、zhangsan 余额没被那条 42 元订单扣减、
  订单号是 12 位纯数字而真实单号是 yyyyMMdd + 8 位十六进制）。
  所以这里一律走真实接口，由业务代码自己把这一套写完。

写入方式的分工：
  · 业务数据（排片 / 充值 / 下单 / 支付 / 取票 / 评价）—— 全部真实 HTTP 接口
  · 状态重置与读回主键 —— 用 SQL；它们不产生业务数据，只是把库复位到可重复运行的状态

用法：
    # 1. 起后端（建议另起备用端口，别打断你自己的 9090）
    #    例：DB_NAME=xm-film java -jar target/springboot-0.0.1-SNAPSHOT.jar --server.port=9191
    # 2. 先看计划，不写库：
    python scripts/seed-demo-data.py
    # 3. 确认计划无误后执行：
    python scripts/seed-demo-data.py --apply

环境变量：
    E2E_BASE      后端地址，默认 http://localhost:9090
    E2E_DB        目标库名，默认 xm-film
    DB_PASSWORD   数据库密码，默认 123456
"""
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta

sys.stdout.reconfigure(encoding='utf-8')

BASE = os.environ.get('E2E_BASE', 'http://localhost:9090')
DB = os.environ.get('E2E_DB', 'xm-film')
DB_PASSWORD = os.environ.get('DB_PASSWORD', '123456')

APPLY = '--apply' in sys.argv

# 演示账号：三个 USER 各出一条真实订单 + 一条真实评价
DEMO_USERS = [
    ('zhangsan', 'user123', 100.00),  # 种子预置 100 元，不需要充值
    ('lisi', '123', 0.00),            # 余额 0 → 先走真实充值流程
    ('wangwu', 'user123', 0.00),      # 余额 0 → 先走真实充值流程
]
RECHARGE_AMOUNT = 100.00
CINEMA_LOGIN = ('asks', 'cinema123')  # 排片、取票都要 CINEMA 角色
SLOT_PRICES = [45.0, 39.5, 52.0]      # 三个场次各自的票价
SLOT_OFFSET_DAYS = [5, 6, 7]          # 场次开映日 = 今天 + N 天
SLOT_TIME_OF_DAY = '19:30:00'

# 已从 data.sql 移除的旧种子行 id（v1 种子：record 15 条 / ordered 12 条 / mark 51 条）。
# 清理旧种子排片用「id + 开映日窗口 + 无订单引用」三重条件，避免在别的库上误伤自建排片 ——
# 仅凭 id 匹配，在一个全新库上会撞上真实业务刚建出来的低 id 记录。
SEED_RECORD_IDS = [1, 2, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19]
SEED_RECORD_START_FROM = '2026-10-01 00:00:00'
SEED_RECORD_START_TO = '2026-10-15 23:59:59'
SEED_RECORD_MATCH = ("id IN (%s) AND start BETWEEN '%s' AND '%s' "
                     'AND id NOT IN (SELECT record_id FROM ordered WHERE record_id IS NOT NULL)'
                     % (','.join(str(i) for i in SEED_RECORD_IDS),
                        SEED_RECORD_START_FROM, SEED_RECORD_START_TO))

REVIEWS = [
    (8.8, '真实购票后留下的评价：机舱封闭空间的调度相当紧张，全程没走神'),
    (9.2, '真实购票后留下的评价：熊猫幼崽太可爱了，孩子笑了一整场'),
    (9.0, '真实购票后留下的评价：群像立得住，战争片的分量感很足'),
]

results = []
# 本机可能开着 HTTP_PROXY，localhost 请求必须绕过代理，否则返回 502
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def sql(statement):
    subprocess.run(['mysql', '--default-character-set=utf8mb4', '-uroot', '-p' + DB_PASSWORD, DB,
                    '-e', statement], capture_output=True, check=True)


def query(statement):
    # 必须显式 encoding='utf-8'：中文 Windows 上 text=True 会按 GBK 解码（locale），
    # 而库是 utf8mb4 —— 一旦结果里含中文（如 status='已取票'）解码线程就抛
    # UnicodeDecodeError，stdout 变成 None，报错点会跑到调用方那行，极难排查。
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


def check(name, ok, detail=''):
    results.append((name, ok, detail))
    print(('  PASS  ' if ok else '  FAIL  ') + name + (('  -> ' + str(detail)) if detail and not ok else ''))


def login(username, password, role):
    _, res = call('POST', '/api/v1/auth/login',
                  body={'username': username, 'password': password, 'role': role})
    return res.get('data', {}).get('token')


def money(value):
    return round(float(value), 2)


def balance(token):
    _, res = call('GET', '/api/v1/account/summary', token)
    return money(res.get('data', {}).get('balance'))


def flows(token):
    _, res = call('GET', '/api/v1/fund-flows/page?pageNum=1&pageSize=50', token)
    return (res.get('data') or {}).get('list', []) or []


# ---------------------------------------------------------------- 计划
print('=== 0. 目标与计划 ===')
print('后端: %s   目标库: %s' % (BASE, DB))
if not APPLY:
    print('当前是【计划模式】，不会写库。确认后加 --apply 执行。\n')

user_ids = {}
for username, _, _ in DEMO_USERS:
    rows = query("SELECT id FROM user WHERE username = '%s'" % username)
    if rows:
        user_ids[username] = int(rows[0][0])
print('演示账号 -> id: %s' % user_ids)

cinema_rows = query("SELECT id FROM cinema WHERE username = '%s'" % CINEMA_LOGIN[0])
if not cinema_rows:
    print('找不到影院账号 %s，无法继续' % CINEMA_LOGIN[0])
    sys.exit(2)
cinema_id = int(cinema_rows[0][0])
room_rows = query('SELECT id FROM room WHERE cinema_id = %d ORDER BY id LIMIT 3' % cinema_id)
room_ids = [int(r[0]) for r in room_rows]
film_rows = query("SELECT id FROM film WHERE status = '已上映' ORDER BY id DESC LIMIT 3")
film_ids = [int(r[0]) for r in film_rows]
print('影院 id=%d（%s），影厅 %s，影片 %s' % (cinema_id, CINEMA_LOGIN[0], room_ids, film_ids))
if len(room_ids) < 3 or len(film_ids) < 3:
    print('影厅或已上映影片不足 3 个，无法生成 3 条演示订单')
    sys.exit(2)

demo_id_list = ','.join(str(i) for i in user_ids.values())

# 旧种子订单号是 12 位纯数字，真实单号是 yyyyMMdd + 8 位十六进制。
# 用它判断"这个库是不是从旧 data.sql 初始化出来的"——只有老库才需要清 v1 种子排片，
# 否则会误伤全新库里刚建出来的低 id 记录。
legacy = int(query("SELECT COUNT(*) FROM ordered WHERE orders REGEXP '^[0-9]{12}$'")[0][0])

print('\n将要删除的旧假数据：')
plan = [
    ('资金流水（演示账号）', 'SELECT COUNT(*) FROM fund_flow WHERE user_id IN (%s)' % demo_id_list),
    ('充值单据（演示账号）', 'SELECT COUNT(*) FROM recharge_order WHERE user_id IN (%s)' % demo_id_list),
    ('评价（演示账号）', 'SELECT COUNT(*) FROM mark WHERE user_id IN (%s)' % demo_id_list),
    ('订单（演示账号）', 'SELECT COUNT(*) FROM ordered WHERE user_id IN (%s)' % demo_id_list),
]
if legacy:
    plan.append(('v1 种子排片（订单先删，故可删）',
                 'SELECT COUNT(*) FROM record WHERE %s' % SEED_RECORD_MATCH))
for label, stmt in plan:
    print('  %-28s %s 条' % (label, query(stmt)[0][0]))
if not legacy:
    print('  未发现 v1 种子订单（单号非 12 位纯数字）→ 判定为全新库，跳过种子排片清理')

if not APPLY:
    print('\n计划模式结束，未做任何修改。')
    sys.exit(0)

# ---------------------------------------------------------------- 1. 登录
print('\n=== 1. 登录 ===')
u_tokens = {}
for username, password, _ in DEMO_USERS:
    u_tokens[username] = login(username, password, 'USER')
    check('%s 登录取得 token' % username, bool(u_tokens[username]))
cinema_token = login(*CINEMA_LOGIN, role='CINEMA')
check('%s 登录取得 token' % CINEMA_LOGIN[0], bool(cinema_token))
if not all(u_tokens.values()) or not cinema_token:
    sys.exit(1)

# ---------------------------------------------------------------- 2. 重置
print('\n=== 2. 重置（SQL，不产生业务数据） ===')
sql('DELETE FROM fund_flow WHERE user_id IN (%s);' % demo_id_list)
sql('DELETE FROM recharge_order WHERE user_id IN (%s);' % demo_id_list)
sql('DELETE FROM mark WHERE user_id IN (%s);' % demo_id_list)
sql('DELETE FROM ordered WHERE user_id IN (%s);' % demo_id_list)
if legacy:
    sql('DELETE FROM record WHERE %s;' % SEED_RECORD_MATCH)
    print('已清理 v1 种子排片')
else:
    print('全新库：无 v1 种子排片需要清理')
# 余额复位成确定值，好让下面的充值与购票流水前后一致
sql('UPDATE user SET balance = 0.00 WHERE id IN (%s);'
    % ','.join(str(user_ids[u]) for u, _, _ in DEMO_USERS))
sql('UPDATE user SET balance = 100.00 WHERE id = %d;' % user_ids['zhangsan'])
print('已重置演示账号的订单/评价/流水/充值单，并清理旧种子排片')

# ---------------------------------------------------------------- 3. 排片
print('\n=== 3. 排片（真实接口 /api/v1/records） ===')
record_ids = []
today = datetime.now().date()
for index, (room_id, film_id) in enumerate(zip(room_ids, film_ids)):
    start = '%s %s' % (today + timedelta(days=SLOT_OFFSET_DAYS[index]), SLOT_TIME_OF_DAY)
    # 可重跑：同影厅同影片已有一个可购票场次就复用，不重复建
    existing = query("SELECT id FROM record WHERE cinema_id = %d AND room_id = %d AND film_id = %d "
                     "AND status = '正常' AND start > NOW() ORDER BY id DESC LIMIT 1"
                     % (cinema_id, room_id, film_id))
    if existing:
        record_ids.append(int(existing[0][0]))
        print('  复用已有场次 id=%s' % existing[0][0])
        continue
    _, res = call('POST', '/api/v1/records', cinema_token, {
        'roomId': room_id, 'filmId': film_id, 'start': start, 'price': SLOT_PRICES[index],
    })
    if res.get('code') != '200':
        check('创建场次（影厅 %d / 影片 %d）' % (room_id, film_id), False, res)
        sys.exit(1)
    rows = query("SELECT id FROM record WHERE cinema_id = %d AND room_id = %d AND film_id = %d "
                 "AND start = '%s' ORDER BY id DESC LIMIT 1" % (cinema_id, room_id, film_id, start))
    record_ids.append(int(rows[0][0]))
    print('  新建场次 id=%s（%s，%s 元）' % (rows[0][0], start, SLOT_PRICES[index]))
check('三个场次就绪', len(set(record_ids)) == 3, record_ids)

# ---------------------------------------------------------------- 4. 充值
print('\n=== 4. 充值（真实流程：提交单据 → 回调入账） ===')
for username in ('lisi', 'wangwu'):
    token = u_tokens[username]
    _, created = call('POST', '/api/v1/recharges', token, {'amount': RECHARGE_AMOUNT})
    recharge_id = (created.get('data') or {}).get('id')
    check('%s 提交充值单据（处理中，余额不变）' % username, bool(recharge_id), created)
    if not recharge_id:
        sys.exit(1)
    check('%s 提交后余额仍为 0.00' % username, balance(token) == 0.00, balance(token))
    _, cb = call('POST', '/api/v1/recharges/%d/callback' % recharge_id, token, {'success': True})
    check('%s 回调成功入账 %s 元' % (username, RECHARGE_AMOUNT),
          cb.get('code') == '200' and balance(token) == RECHARGE_AMOUNT,
          cb if cb.get('code') != '200' else balance(token))

# ---------------------------------------------------------------- 5. 订单
print('\n=== 5. 下单 → 支付 → 取票（真实流程） ===')
order_info = []
for index, (username, _, _) in enumerate(DEMO_USERS):
    token = u_tokens[username]
    record_id = record_ids[index]
    price = SLOT_PRICES[index]
    before = balance(token)

    _, created = call('POST', '/api/v1/orders/create', token,
                      {'recordId': record_id, 'seat': '1排1座'})
    order = created.get('data') or {}
    order_id = order.get('id')
    check('%s 下单成功且进入待支付' % username,
          created.get('code') == '200' and order.get('status') == '待支付', created)
    if not order_id:
        sys.exit(1)
    check('%s 单价快照 = 场次票价 %s' % (username, price), money(order.get('unitPrice')) == price,
          order.get('unitPrice'))
    check('%s 下单后未扣款' % username, balance(token) == before, balance(token))

    _, paid = call('PUT', '/api/v1/orders/%d/pay' % order_id, token)
    check('%s 支付成功（待取票）' % username, paid.get('code') == '200', paid)
    check('%s 支付后余额 %s - %s = %s' % (username, before, price, money(before - price)),
          balance(token) == money(before - price), balance(token))

    _, picked = call('PUT', '/api/v1/orders/%d/pickup' % order_id, cinema_token)
    check('%s 影院端取票成功（已取票）' % username, picked.get('code') == '200', picked)
    order_info.append((username, record_id, film_ids[index], order_id, price))

# ---------------------------------------------------------------- 6. 评价
print('\n=== 6. 评价（真实接口 /api/v1/marks） ===')
for index, (username, _, film_id, _, _) in enumerate(order_info):
    score, comment = REVIEWS[index]
    _, res = call('POST', '/api/v1/marks', u_tokens[username],
                  {'filmId': film_id, 'score': score, 'mark': comment})
    check('%s 发表评价（影片 %d，%s 分）' % (username, film_id, score), res.get('code') == '200', res)

# ---------------------------------------------------------------- 7. 核对
print('\n=== 7. 账实核对 ===')
booked = ','.join(str(i[3]) for i in order_info)
rows = query('SELECT orders, unit_price, status FROM ordered WHERE id IN (%s) ORDER BY id' % booked)
check('三笔订单都写了单价快照（不为 NULL）', all(r[1] not in ('NULL', '') for r in rows), [r[1] for r in rows])
check('订单号格式 = yyyyMMdd + 8 位十六进制（真实生成）',
      all(re.fullmatch(r'\d{8}[0-9A-F]{8}', r[0]) for r in rows), [r[0] for r in rows])
check('三笔订单状态均为已取票', all(r[2] == '已取票' for r in rows), [r[2] for r in rows])

for index, (username, record_id, film_id, _, price) in enumerate(order_info):
    token = u_tokens[username]
    expect_balance = 100.00 - price
    check('%s 期末余额 = 100.00 - %s = %s' % (username, price, money(expect_balance)),
          balance(token) == money(expect_balance), balance(token))
    user_flows = flows(token)
    kinds = sorted(f.get('source') or '' for f in user_flows)
    print('  %s 资金流水 %d 条：%s' % (username, len(user_flows), kinds))

for index, (_, _, film_id, _, _) in enumerate(order_info):
    score = REVIEWS[index][0]
    row = query('SELECT score FROM film WHERE id = %d' % film_id)[0][0]
    check('影片 %d 评分由评价回写 = %s' % (film_id, score), money(row) == score, row)

remaining = query('SELECT COUNT(*) FROM ordered WHERE status = %s' % "'待支付'")[0][0]
print('  全库遗留待支付订单：%s 条（超时会被 OrderCleanupTask 自动取消）' % remaining)

# ---------------------------------------------------------------- 汇总
passed = sum(1 for _, ok, _ in results if ok)
print('\n=== 结果：%d/%d 通过 ===' % (passed, len(results)))
sys.exit(0 if passed == len(results) else 1)
