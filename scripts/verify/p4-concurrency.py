"""余额扣减的并发正确性验证（评审专项 8）。

两个场景：
  A. 余额只够一单时，两笔订单并发支付 → 只允许一单成功，余额不得为负
  B. 同一订单并发重复支付   → 只有一次生效，余额只扣一次

前置：按 p4-account-wallet-e2e.py 顶部说明建好临时库并在备用端口起后端，
然后 `python scripts/verify/p4-concurrency.py`。
本脚本会直接改临时库的余额与流水表，**不要指向开发库**。

环境变量：E2E_BASE / E2E_DB / DB_PASSWORD / E2E_RECORD_ID
"""
import json
import os
import subprocess
import sys
import threading
import urllib.request
from datetime import datetime, timedelta

sys.stdout.reconfigure(encoding='utf-8')

BASE = os.environ.get('E2E_BASE', 'http://localhost:9191')
DB = os.environ.get('E2E_DB', 'xm_film_verify')
DB_PASSWORD = os.environ.get('DB_PASSWORD', '123456')
# 未指定则由 ensure_record() 自己准备（单价 39.50）；种子已不再预置排片
_record_env = os.environ.get('E2E_RECORD_ID')
RECORD_ID = int(_record_env) if _record_env else None

opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
results = []


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

    断言依赖「单价 39.50」与「≥4 排 × ≥2 列」的座位规模（用到了 3~4 排、1~2 座）。
    """
    _, cinema = call('POST', '/api/v1/auth/login',
                     body={'username': 'asks', 'password': 'cinema123', 'role': 'CINEMA'})
    cinema_token = (cinema.get('data') or {}).get('token')
    if not cinema_token:
        print('  FAIL  影院账号登录失败，无法准备场次: %s' % cinema)
        sys.exit(2)
    room = query('SELECT r.id FROM room r WHERE r.seat_rows >= 4 AND r.seat_cols >= 2 '
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
        return exc.code, json.loads(exc.read().decode('utf-8'))


def check(name, ok, detail=''):
    results.append((name, ok, detail))
    print(('  PASS  ' if ok else '  FAIL  ') + name + (('  -> ' + str(detail)) if detail and not ok else ''))


def balance(token):
    _, res = call('GET', '/api/v1/account/summary', token)
    return round(float(res.get('data', {}).get('balance')), 2)


def purchase_flow_count():
    out = subprocess.run(['mysql', '--default-character-set=utf8mb4', '-uroot', '-p' + DB_PASSWORD, DB, '-N', '-B',
                          '-e', "SELECT COUNT(*) FROM fund_flow WHERE source='购票'"],
                         capture_output=True, text=True, encoding='utf-8', check=True)
    return int(out.stdout.strip())


_, login = call('POST', '/api/v1/auth/login',
                body={'username': 'zhangsan', 'password': 'user123', 'role': 'USER'})
token = login['data']['token']

if RECORD_ID is None:
    RECORD_ID = ensure_record()
    check('演示场次就绪（单价 39.50）', RECORD_ID > 0, RECORD_ID)

# 本脚本独占该场次：清掉该场次既有订单，保证反复运行不会撞上"座位已售"
sql('DELETE FROM ordered WHERE record_id = %d;' % RECORD_ID)
sql('DELETE FROM fund_flow;')

print('=== 场景 A：余额只够一单，两笔订单并发支付 ===')
sql('UPDATE user SET balance = 40.00 WHERE id = 6;')

order_ids = []
for index, seat in enumerate(['3排1座', '3排2座']):
    _, created = call('POST', '/api/v1/orders/create', token, {'recordId': RECORD_ID, 'seat': seat})
    order_ids.append(created['data']['id'])
check('两笔待支付订单已创建（每笔 39.50）', len(order_ids) == 2, order_ids)
check('建单后余额仍为 40.00', balance(token) == 40.00, balance(token))

outcomes = []
barrier = threading.Barrier(2)


def pay_concurrently(order_id):
    barrier.wait()
    _, body = call('PUT', '/api/v1/orders/%d/pay' % order_id, token)
    # 线程完成顺序不确定，必须把订单号与结果成对记录，不能靠 append 顺序对齐
    outcomes.append((order_id, body.get('code'), body.get('msg')))


threads = [threading.Thread(target=pay_concurrently, args=(oid,)) for oid in order_ids]
for thread in threads:
    thread.start()
for thread in threads:
    thread.join()

successes = [(oid, code, msg) for oid, code, msg in outcomes if code == '200']
failures = [(oid, code, msg) for oid, code, msg in outcomes if code != '200']
check('恰好一单支付成功', len(successes) == 1, outcomes)
check('另一单被余额不足拒绝', len(failures) == 1 and '余额不足' in (failures[0][2] or ''), outcomes)
check('余额为 40.00 - 39.50 = 0.50，未出现负数', balance(token) == 0.50, balance(token))
check('只记录一条购票流水', purchase_flow_count() == 1, purchase_flow_count())

_, paid_row = call('GET', '/api/v1/orders/%d' % successes[0][0], token)
check('成功那单已出票（待取票）', (paid_row.get('data') or {}).get('status') == '待取票',
      (paid_row.get('data') or {}).get('status'))
_, other_row = call('GET', '/api/v1/orders/%d' % failures[0][0], token)
check('失败那单仍为待支付', (other_row.get('data') or {}).get('status') == '待支付',
      (other_row.get('data') or {}).get('status'))

print('=== 场景 B：同一订单并发重复支付 ===')
sql('UPDATE user SET balance = 100.00 WHERE id = 6;')
sql('DELETE FROM fund_flow;')

_, created = call('POST', '/api/v1/orders/create', token, {'recordId': RECORD_ID, 'seat': '4排1座'})
dup_order_id = created['data']['id']

outcomes = []
barrier = threading.Barrier(2)


def pay_same_order():
    barrier.wait()
    _, body = call('PUT', '/api/v1/orders/%d/pay' % dup_order_id, token)
    outcomes.append(body.get('code'))


threads = [threading.Thread(target=pay_same_order) for _ in range(2)]
for thread in threads:
    thread.start()
for thread in threads:
    thread.join()

check('同一订单并发支付只成功一次', sum(1 for code in outcomes if code == '200') == 1, outcomes)
check('余额只扣一次（100.00 - 39.50 = 60.50）', balance(token) == 60.50, balance(token))
check('只记录一条购票流水', purchase_flow_count() == 1, purchase_flow_count())

print()
passed = sum(1 for _, ok, _ in results if ok)
total = len(results)
print('===== 并发结果: %d/%d 通过 =====' % (passed, total))
for name, ok, detail in results:
    if not ok:
        print('  FAILED: %s -> %s' % (name, detail))
sys.exit(0 if passed == total else 1)
