"""阶段一（账户-充值-订单闭环）端到端验证脚本。

针对备用端口 + 临时库运行，不触碰开发库 xm-film 与本机 9090/5173：

    # 1. 建临时库并导入结构与种子数据
    mysql -uroot -p --default-character-set=utf8mb4 -e "DROP DATABASE IF EXISTS xm_film_verify; \
CREATE DATABASE xm_film_verify DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    mysql -uroot -p --default-character-set=utf8mb4 xm_film_verify < xm_film/sql/schema.sql
    mysql -uroot -p --default-character-set=utf8mb4 xm_film_verify < xm_film/sql/data.sql

    # 注：data.sql 已不再预置放映场次/订单/评价（一律由真实接口产生），
    # 本脚本启动时会自己准备一个独占场次（单价 39.50），并把余额复位到种子值。

    # 2. 用临时库、备用端口起后端（不要动 9090）
    cd xm_film/springboot
    DB_NAME=xm_film_verify java -jar target/springboot-0.0.1-SNAPSHOT.jar --server.port=9191

    # 3. 跑本脚本（BASE 可用环境变量 E2E_BASE 覆盖）
    python scripts/verify/p4-account-wallet-e2e.py

覆盖评审专项 1/3/4/5/6/7/8：
  余额不足 → 订单保持待支付且座位继续锁定
  提交充值单据不改余额 → 回调成功才入账 → 继续支付成功扣款
  资金流水三条（充值/购票/退票）含变动前后余额与关联单据ID
  重复回调被拒 / 回调失败不改余额
  已支付订单不可物理删除 / 终态废单可删除
  余额不随通用用户查询泄漏、跨用户回调被拒、影院角色无账户
  选座接口只返回座位与归属，不泄露他人订单号/金额/用户ID
"""
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta

sys.stdout.reconfigure(encoding='utf-8')

BASE = os.environ.get('E2E_BASE', 'http://localhost:9191')
DB = os.environ.get('E2E_DB', 'xm_film_verify')
DB_PASSWORD = os.environ.get('DB_PASSWORD', '123456')
USER_ID = 6        # 种子用户 zhangsan
USERNAME = 'zhangsan'
PASSWORD = 'user123'
RECORD_ID = None   # 由 ensure_record() 在启动时准备；种子已不再预置排片

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


def ensure_record():
    """准备本脚本独占的场次。种子已不再预置排片，所以由脚本自己建；已存在可复用的就复用。

    断言依赖「单价 39.50」与「≥5 排 × ≥3 列」的座位规模（用到了 1~5 排、1~3 座）。
    """
    _, cinema = call('POST', '/api/v1/auth/login',
                     body={'username': 'asks', 'password': 'cinema123', 'role': 'CINEMA'})
    cinema_token = (cinema.get('data') or {}).get('token')
    if not cinema_token:
        print('  FAIL  影院账号登录失败，无法准备场次: %s' % cinema)
        sys.exit(2)
    room = query('SELECT r.id FROM room r WHERE r.seat_rows >= 5 AND r.seat_cols >= 3 '
                 "AND r.cinema_id = (SELECT id FROM cinema WHERE username = 'asks') ORDER BY r.id LIMIT 1")
    film = query("SELECT id FROM film WHERE status = '已上映' ORDER BY id LIMIT 1")
    if not room or not film:
        print('  FAIL  找不到可用影厅或已上映影片，无法准备场次')
        sys.exit(2)
    room_id, film_id = int(room[0][0]), int(film[0][0])
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


def money(value):
    return round(float(value), 2)


def balance(token):
    _, res = call('GET', '/api/v1/account/summary', token)
    return money(res.get('data', {}).get('balance'))


def flows(token):
    _, res = call('GET', '/api/v1/fund-flows/page?pageNum=1&pageSize=50', token)
    return res.get('data', {}).get('list', [])


def seats_in_use(token, record_id):
    _, res = call('GET', '/api/v1/orders/seats?recordId=%d' % record_id, token)
    used = set()
    for order in (res.get('data') or []):
        for seat in (order.get('seat') or '').split(','):
            if seat.strip():
                used.add(seat.strip())
    return used


print('=== 0. 登录与场次准备 ===')
_, login = call('POST', '/api/v1/auth/login',
                body={'username': USERNAME, 'password': PASSWORD, 'role': 'USER'})
token = login.get('data', {}).get('token')
check('登录取得 token', bool(token), login)
if not token:
    sys.exit(1)

# 种子已不再预置排片：场次由脚本自己准备（已存在则复用，可反复运行）
RECORD_ID = ensure_record()
check('演示场次就绪（单价 39.50）', RECORD_ID > 0, RECORD_ID)

# 状态重置：清掉该用户的流水与充值单、该场次的订单，并把余额复位成种子值，
# 保证脚本可以在同一个临时库上反复运行（断言用的是绝对条数与绝对余额）。
sql('DELETE FROM fund_flow WHERE user_id = %d;' % USER_ID)
sql('DELETE FROM recharge_order WHERE user_id = %d;' % USER_ID)
sql('DELETE FROM ordered WHERE record_id = %d;' % RECORD_ID)
sql('UPDATE user SET balance = 100.00 WHERE id = %d;' % USER_ID)

print('=== 1. 余额只对本人可见，且不随通用用户查询泄漏 ===')
start_balance = balance(token)
check('zhangsan 初始余额为种子值 100.00', start_balance == 100.00, start_balance)

_, users = call('GET', '/api/v1/users/6', token)
leaked = 'balance' in json.dumps(users.get('data') or {})
check('GET /api/v1/users/{id} 不含 balance 字段（避免越权读他人余额）', not leaked, users.get('data'))

print('=== 2. 下单只锁座不扣款 ===')
_, created = call('POST', '/api/v1/orders/create', token,
                  {'recordId': RECORD_ID, 'seat': '1排1座,1排2座,1排3座'})
order = created.get('data') or {}
order_id = order.get('id')
check('下单成功且进入待支付', created.get('code') == '200' and order.get('status') == '待支付', created)
check('订单总金额 = 单价 × 座位数（39.50 × 3）', money(order.get('total')) == 118.50, order.get('total'))
check('订单留存单价快照 39.50', order.get('unitPrice') is not None and money(order.get('unitPrice')) == 39.50,
      order.get('unitPrice'))
check('下单后余额未变（仍 100.00）', balance(token) == 100.00, balance(token))

print('=== 3. 余额不足：订单保持待支付 + 座位继续锁定 ===')
_, pay_fail = call('PUT', '/api/v1/orders/%d/pay' % order_id, token)
check('支付被拒（余额不足）', pay_fail.get('code') != '200', pay_fail)
check('拒绝原因含「余额不足」', '余额不足' in (pay_fail.get('msg') or ''), pay_fail.get('msg'))

_, after_fail = call('GET', '/api/v1/orders/%d' % order_id, token)
check('订单状态仍为待支付', (after_fail.get('data') or {}).get('status') == '待支付',
      (after_fail.get('data') or {}).get('status'))
check('订单未写入支付凭证（payAmount 为空）', (after_fail.get('data') or {}).get('payAmount') is None,
      (after_fail.get('data') or {}).get('payAmount'))
check('余额未被扣减（仍 100.00）', balance(token) == 100.00, balance(token))
locked = seats_in_use(token, RECORD_ID)
check('三个座位仍被占用锁定', {'1排1座', '1排2座', '1排3座'} <= locked, locked)
check('未产生资金流水（失败支付不记账）', len(flows(token)) == 0, len(flows(token)))

print('=== 4. 充值申请：提交单据不改余额 ===')
_, recharge = call('POST', '/api/v1/recharges', token, {'amount': 200})
recharge_id = (recharge.get('data') or {}).get('id')
check('充值单据创建成功', recharge.get('code') == '200' and recharge_id is not None, recharge)
check('单据初始状态为「处理中」', (recharge.get('data') or {}).get('status') == '处理中',
      (recharge.get('data') or {}).get('status'))
check('提交单据后余额未增加（仍 100.00）', balance(token) == 100.00, balance(token))

_, bad_amount = call('POST', '/api/v1/recharges', token, {'amount': 50000.01})
check('超过单笔上限被拒', bad_amount.get('code') != '200', bad_amount.get('msg'))
_, negative = call('POST', '/api/v1/recharges', token, {'amount': -10})
check('负数金额被拒', negative.get('code') != '200', negative.get('msg'))

print('=== 5. 模拟支付回调成功：单据完成 + 余额入账 ===')
_, callback = call('POST', '/api/v1/recharges/%d/callback' % recharge_id, token,
                   {'success': True, 'remark': ''})
check('回调成功返回 200', callback.get('code') == '200', callback)
check('余额入账 100 + 200 = 300.00', balance(token) == 300.00, balance(token))

_, recharge_after = call('GET', '/api/v1/recharges/page?pageNum=1&pageSize=10', token)
rows = recharge_after.get('data', {}).get('list', [])
done = [r for r in rows if r.get('id') == recharge_id]
check('单据已置为「已完成」', done and done[0].get('status') == '已完成',
      done[0].get('status') if done else 'not found')
check('单据记录了完成时间', bool(done and done[0].get('finishTime')),
      done[0].get('finishTime') if done else None)

print('=== 6. 重复回调被拒绝（幂等） ===')
_, again = call('POST', '/api/v1/recharges/%d/callback' % recharge_id, token, {'success': True})
check('重复回调被拒', again.get('code') != '200', again)
check('重复回调后余额不变（仍 300.00）', balance(token) == 300.00, balance(token))

print('=== 7. 余额充足后继续支付同一订单 ===')
_, pay_ok = call('PUT', '/api/v1/orders/%d/pay' % order_id, token)
check('支付成功', pay_ok.get('code') == '200', pay_ok)
_, paid = call('GET', '/api/v1/orders/%d' % order_id, token)
paid_row = paid.get('data') or {}
check('订单流转为待取票', paid_row.get('status') == '待取票', paid_row.get('status'))
check('实付金额 = 订单总额 118.50', money(paid_row.get('payAmount')) == 118.50, paid_row.get('payAmount'))
check('余额扣减 300 - 118.50 = 181.50', balance(token) == 181.50, balance(token))

print('=== 8. 资金流水：充值 + 购票 两条，含变动前后余额 ===')
flow_list = flows(token)
check('流水共 2 条', len(flow_list) == 2, len(flow_list))
by_source = {f.get('source'): f for f in flow_list}
purchase = by_source.get('购票')
recharge_flow = by_source.get('充值')
check('购票流水金额为 -118.50', purchase and money(purchase.get('changeAmount')) == -118.50,
      purchase.get('changeAmount') if purchase else None)
check('购票流水前后余额 300.00 → 181.50',
      purchase and money(purchase.get('balanceBefore')) == 300.00 and money(purchase.get('balanceAfter')) == 181.50,
      (purchase.get('balanceBefore'), purchase.get('balanceAfter')) if purchase else None)
check('购票流水关联订单ID', purchase and purchase.get('relatedId') == order_id,
      purchase.get('relatedId') if purchase else None)
check('充值流水前后余额 100.00 → 300.00',
      recharge_flow and money(recharge_flow.get('balanceBefore')) == 100.00
      and money(recharge_flow.get('balanceAfter')) == 300.00,
      (recharge_flow.get('balanceBefore'), recharge_flow.get('balanceAfter')) if recharge_flow else None)
check('充值流水关联充值单ID', recharge_flow and recharge_flow.get('relatedId') == recharge_id,
      recharge_flow.get('relatedId') if recharge_flow else None)

print('=== 9. 已支付订单不可物理删除（封堵删单当退票的旁路） ===')
_, del_paid = call('DELETE', '/api/v1/orders/%d' % order_id, token)
check('删除待取票订单被拒', del_paid.get('code') != '200', del_paid.get('msg'))
_, still_there = call('GET', '/api/v1/orders/%d' % order_id, token)
check('订单仍然存在', (still_there.get('data') or {}).get('id') == order_id, still_there.get('data'))

print('=== 10. 退票：余额退回 + 座位释放 + 退票流水 ===')
_, refund = call('PUT', '/api/v1/orders/%d/refund' % order_id, token)
check('退票成功', refund.get('code') == '200', refund)
_, refunded = call('GET', '/api/v1/orders/%d' % order_id, token)
refunded_row = refunded.get('data') or {}
check('订单流转为已退票', refunded_row.get('status') == '已退票', refunded_row.get('status'))
check('退款金额 = 118.50', money(refunded_row.get('refundAmount')) == 118.50, refunded_row.get('refundAmount'))
check('余额退回 181.50 + 118.50 = 300.00', balance(token) == 300.00, balance(token))
released = seats_in_use(token, RECORD_ID)
check('三个座位已释放', not ({'1排1座', '1排2座', '1排3座'} & released), released)
flow_list = flows(token)
check('新增一条退票流水（共 3 条）', len(flow_list) == 3, len(flow_list))
refund_flow = {f.get('source'): f for f in flow_list}.get('退票')
check('退票流水金额 +118.50 且关联订单ID',
      refund_flow and money(refund_flow.get('changeAmount')) == 118.50 and refund_flow.get('relatedId') == order_id,
      refund_flow)

print('=== 11. 终态废单可删除 ===')
_, del_refunded = call('DELETE', '/api/v1/orders/%d' % order_id, token)
check('已退票订单可删除', del_refunded.get('code') == '200', del_refunded)

print('=== 12. 回调失败：单据置为已失败，余额不变 ===')
_, recharge2 = call('POST', '/api/v1/recharges', token, {'amount': 50})
recharge2_id = (recharge2.get('data') or {}).get('id')
_, failed = call('POST', '/api/v1/recharges/%d/callback' % recharge2_id, token,
                 {'success': False, 'remark': '模拟支付失败'})
check('失败回调返回 200', failed.get('code') == '200', failed)
_, rows2 = call('GET', '/api/v1/recharges/page?pageNum=1&pageSize=10', token)
failed_row = [r for r in rows2.get('data', {}).get('list', []) if r.get('id') == recharge2_id]
check('单据置为「已失败」', failed_row and failed_row[0].get('status') == '已失败',
      failed_row[0].get('status') if failed_row else None)
check('失败后余额不变（仍 300.00）', balance(token) == 300.00, balance(token))
check('失败回调不产生资金流水（仍 3 条）', len(flows(token)) == 3, len(flows(token)))
_, failed_again = call('POST', '/api/v1/recharges/%d/callback' % recharge2_id, token, {'success': True})
check('已失败单据不可再回调', failed_again.get('code') != '200', failed_again.get('msg'))

print('=== 13. 取消未支付订单不产生资金回滚 ===')
_, order2 = call('POST', '/api/v1/orders/create', token, {'recordId': RECORD_ID, 'seat': '2排1座'})
order2_id = (order2.get('data') or {}).get('id')
_, cancelled = call('PUT', '/api/v1/orders/%d/cancel' % order2_id, token)
check('取消待支付订单成功', cancelled.get('code') == '200', cancelled)
check('取消后余额不变（仍 300.00）', balance(token) == 300.00, balance(token))
check('取消未扣款故无新流水（仍 3 条）', len(flows(token)) == 3, len(flows(token)))
_, del_cancelled = call('DELETE', '/api/v1/orders/%d' % order2_id, token)
check('已取消订单可删除', del_cancelled.get('code') == '200', del_cancelled)

print('=== 14. 越权与边界 ===')
_, other_user = call('POST', '/api/v1/auth/login',
                     body={'username': 'wangwu', 'password': 'user123', 'role': 'USER'})
other_token = other_user.get('data', {}).get('token')
_, other_balance = call('GET', '/api/v1/account/summary', other_token)
check('wangwu 余额为 0.00', money((other_balance.get('data') or {}).get('balance')) == 0.00,
      other_balance.get('data'))
_, other_recharge = call('POST', '/api/v1/recharges', other_token, {'amount': 10})
other_rid = (other_recharge.get('data') or {}).get('id')
_, cross = call('POST', '/api/v1/recharges/%d/callback' % other_rid, token, {'success': True})
check('不能回调他人充值单据', cross.get('code') != '200', cross.get('msg'))
_, cinema_role = call('POST', '/api/v1/auth/login',
                      body={'username': 'asks', 'password': 'cinema123', 'role': 'CINEMA'})
cinema_token = cinema_role.get('data', {}).get('token')
_, cinema_recharge = call('POST', '/api/v1/recharges', cinema_token, {'amount': 10})
check('影院角色不能发起充值', cinema_recharge.get('code') != '200', cinema_recharge.get('msg'))
_, cinema_summary = call('GET', '/api/v1/account/summary', cinema_token)
check('影院角色无账户余额', cinema_summary.get('code') != '200', cinema_summary.get('msg'))
_, no_token = call('GET', '/api/v1/account/summary')
check('未登录访问账户摘要被拒', no_token.get('code') != '200', no_token.get('msg'))

print('=== 15. 选座接口只暴露座位与归属，不泄露他人订单明细 ===')
_, wangwu_order = call('POST', '/api/v1/orders/create', other_token,
                       {'recordId': RECORD_ID, 'seat': '5排1座'})
check('wangwu 下单成功（用于构造他人订单）', wangwu_order.get('code') == '200', wangwu_order)
_, mine_order = call('POST', '/api/v1/orders/create', token,
                     {'recordId': RECORD_ID, 'seat': '5排2座'})
mine_id = (mine_order.get('data') or {}).get('id')
check('zhangsan 下单成功（用于构造本人订单）', mine_id is not None, mine_order)

_, seats_view = call('GET', '/api/v1/orders/seats?recordId=%d' % RECORD_ID, token)
entries = seats_view.get('data') or []
foreign = [e for e in entries if e.get('seat') == '5排1座']
own = [e for e in entries if e.get('seat') == '5排2座']
check('他人座位仍出现在选座图上（mine=false）', foreign and foreign[0].get('mine') is False, foreign)
check('他人订单不返回订单ID', foreign and foreign[0].get('orderId') is None, foreign)
check('他人订单不返回订单编号', foreign and foreign[0].get('orders') is None, foreign)
check('他人订单不返回金额', foreign and foreign[0].get('total') is None, foreign)
check('他人订单不返回用户ID', foreign and all('userId' not in e for e in entries), foreign)
check('本人座位标记 mine=true', own and own[0].get('mine') is True, own)
check('本人座位带 orderId（继续支付可用）', own and own[0].get('orderId') == mine_id, own)
check('本人座位带金额与倒计时', own and own[0].get('total') is not None
      and own[0].get('pendingTimeoutAt') is not None, own)

print()
passed = sum(1 for _, ok, _ in results if ok)
total = len(results)
print('===== 结果: %d/%d 通过 =====' % (passed, total))
for name, ok, detail in results:
    if not ok:
        print('  FAILED: %s -> %s' % (name, detail))
sys.exit(0 if passed == total else 1)
