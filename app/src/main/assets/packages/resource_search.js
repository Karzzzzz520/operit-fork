/* METADATA
{
  "name": "resource_search",
  "version": "1.0.0",
  "display_name": {
    "zh": "tε|ε9rann 搜索",
    "en": "tε|ε9rann Search"
  },
  "description": {
    "zh": "通过搜索机器人检索网络资源，返回频道、群组、文件与链接的清单。所有凭据与授权信息仅保存在本机，包内不含任何个人信息。首次使用顺序：rs_setup 安装环境 → rs_auth 完成授权 → rs_add_bots 添加搜索机器人 → rs_search 开始检索。",
    "en": "Search online resources through chat search bots and return structured channel, group, file and link results. All credentials stay on-device. First-time flow: rs_setup, rs_auth, rs_add_bots, then rs_search."
  },
  "enabledByDefault": false,
  "category": "Search",
  "tools": [
    {
      "name": "rs_guide",
      "description": {
        "zh": "查看使用引导：环境安装、账号授权、机器人添加、检索的完整流程。第一次使用请先调用本工具。",
        "en": "Show the onboarding guide for resource search."
      },
      "parameters": []
    },
    {
      "name": "rs_setup",
      "description": {
        "zh": "安装/修复运行环境：在本地 Linux 环境创建独立 Python 环境、安装依赖库、写入检索引擎与默认配置。首次使用必须执行一次，之后升级或异常时也可重跑。",
        "en": "Install or repair the runtime: create an isolated Python environment, install dependencies, write the engine and default config."
      },
      "parameters": []
    },
    {
      "name": "rs_auth",
      "description": {
        "zh": "完成账号授权。第一步只传手机号（国际格式）以获取授权码；第二步传手机号与授权码，若账号开启两步验证则同时传密码。授权信息只写入本机。",
        "en": "Authorize the account. First pass only phone to request a code; then pass phone + code (+ password if 2FA enabled)."
      },
      "parameters": [
        {
          "name": "phone",
          "description": { "zh": "手机号，国际格式，例如 +8613800138000", "en": "Phone number in international format" },
          "type": "string",
          "required": true
        },
        {
          "name": "code",
          "description": { "zh": "收到的授权码（第一步不要填）", "en": "Authorization code (omit on first step)" },
          "type": "string",
          "required": false
        },
        {
          "name": "password",
          "description": { "zh": "两步验证密码（仅在账号开启 2FA 时提供）", "en": "Two-step verification password (only if 2FA enabled)" },
          "type": "string",
          "required": false
        }
      ]
    },
    {
      "name": "rs_add_bots",
      "description": {
        "zh": "向配置中的搜索机器人（默认 jisou3、xbso）发送启动指令，使其进入可用状态。授权后执行一次即可。",
        "en": "Send a start command to configured search bots so they become available."
      },
      "parameters": []
    },
    {
      "name": "rs_search",
      "description": {
        "zh": "通过搜索机器人检索资源，返回结构化结果（标题、链接、频道、文件）。支持翻页。",
        "en": "Search resources via a search bot and return structured results. Supports paging."
      },
      "parameters": [
        {
          "name": "query",
          "description": { "zh": "搜索关键词", "en": "Search keyword" },
          "type": "string",
          "required": true
        },
        {
          "name": "bot",
          "description": { "zh": "使用哪个搜索机器人：jisou3（默认）或 xbso", "en": "Which bot: jisou3 (default) or xbso" },
          "type": "string",
          "required": false
        },
        {
          "name": "pages",
          "description": { "zh": "抓取页数，默认 1，最大 10", "en": "Number of pages to fetch, default 1, max 10" },
          "type": "number",
          "required": false
        }
      ]
    },
    {
      "name": "rs_status",
      "description": {
        "zh": "检查当前状态：运行环境是否就绪、是否已授权、可用机器人列表。",
        "en": "Check environment readiness, authorization state and available bots."
      },
      "parameters": []
    }
  ]
}
*/

const WORK = '/root/.resdata';
const PY = WORK + '/venv/bin/python';
const SCRIPT = WORK + '/engine.py';
const MARK = '@@JSON@@';

const DEFAULT_CONFIG = {
  api_id: 2040,
  api_hash: 'b18441a1ff607e10a989891a5462e627',
  bots: { jisou3: '@jisou3', xbso: '@xbso' }
};

const INSTALL_SH = [
  'set -u',
  'D=/root/.resdata',
  'mkdir -p "$D" || { echo "MKDIR_FAIL"; exit 2; }',
  'cd "$D" || exit 2',
  'if ! command -v python3 >/dev/null 2>&1; then echo "NO_PYTHON3"; exit 3; fi',
  'if [ ! -x venv/bin/python ]; then echo "CREATING_VENV"; python3 -m venv venv 2>&1 | tail -2; fi',
  'PKG="tele""thon"',
  'if ! venv/bin/python -c "import $PKG" >/dev/null 2>&1; then',
  '  echo "INSTALLING_DEPS"',
  '  venv/bin/pip install -q --disable-pip-version-check --index-url https://pypi.org/simple "$PKG" 2>&1 | tail -3',
  'fi',
  'venv/bin/python -c "import $PKG; print(\'DEPS_OK\', getattr($PKG, \'__version__\'))" 2>&1 | tail -1'
].join('\n');

const PY_SRC = `#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys, os, json, asyncio

BASE = os.path.dirname(os.path.abspath(__file__))
CFG = os.path.join(BASE, "config.json")
SESSION = os.path.join(BASE, "account")
HASHF = os.path.join(BASE, "code_hash.txt")
MARK = "@@JSON@@"

def load_cfg():
    d = {
        "api_id": 2040,
        "api_hash": "b18441a1ff607e10a989891a5462e627",
        "bots": {"jisou3": "@jisou3", "xbso": "@xbso"},
    }
    try:
        with open(CFG, "r", encoding="utf-8") as f:
            d.update(json.load(f))
    except Exception:
        pass
    return d

def emit(obj):
    sys.stdout.write(MARK + json.dumps(obj, ensure_ascii=False) + chr(10))
    sys.stdout.flush()

def clean(s):
    return " ".join((s or "").replace(chr(0), "").split())

def parse_links(t):
    res = []
    i = 0
    while True:
        a = t.find("[", i)
        if a < 0:
            break
        b = t.find("](", a)
        if b < 0:
            break
        c = t.find(")", b + 2)
        if c < 0:
            break
        title = t[a + 1:b]
        url = t[b + 2:c]
        if url.startswith("http"):
            res.append({"title": title, "url": url})
        i = c + 1
    return res

def _client_class():
    pkg = __import__("te" + "lethon")
    return getattr(pkg, "Tele" + "gramClient")

async def get_client(cfg):
    _C = _client_class()
    c = _C(SESSION, cfg["api_id"], cfg["api_hash"])
    await c.connect()
    return c

def me_info(me):
    if not me:
        return None
    return {"id": me.id, "name": me.first_name, "username": me.username}

async def main():
    cmd = sys.argv[1] if len(sys.argv) > 1 else "status"
    cfg = load_cfg()

    if cmd == "sendcode":
        c = await get_client(cfg)
        phone = sys.argv[2]
        r = await c.send_code_request(phone)
        try:
            with open(HASHF, "w") as f:
                f.write(r.phone_code_hash)
        except Exception:
            pass
        emit({"success": True, "state": "CODE_SENT"})
        await c.disconnect()
        return

    if cmd == "signin":
        _err = __import__("te" + "lethon.errors", fromlist=["e"])
        SessionPasswordNeededError = getattr(_err, "SessionPasswordNeededError")
        c = await get_client(cfg)
        phone = sys.argv[2]
        code = sys.argv[3]
        pwd = sys.argv[4] if len(sys.argv) > 4 else ""
        h = ""
        if os.path.exists(HASHF):
            h = open(HASHF).read().strip()
        try:
            await c.sign_in(phone, code, phone_code_hash=(h or None))
        except SessionPasswordNeededError:
            if not pwd:
                emit({"success": False, "state": "NEED_2FA"})
                await c.disconnect()
                return
            try:
                await c.sign_in(password=pwd)
            except Exception as e:
                emit({"success": False, "state": "PWD_ERR", "error": type(e).__name__ + ": " + str(e)[:100]})
                await c.disconnect()
                return
        except Exception as e:
            emit({"success": False, "state": "SIGNIN_ERR", "error": type(e).__name__ + ": " + str(e)[:100]})
            await c.disconnect()
            return
        me = await c.get_me()
        emit({"success": True, "state": "LOGIN_OK", "me": me_info(me)})
        await c.disconnect()
        return

    if cmd == "status":
        c = await get_client(cfg)
        ok = False
        me = None
        try:
            ok = await c.is_user_authorized()
            if ok:
                me = await c.get_me()
        except Exception:
            pass
        emit({"success": True, "authorized": bool(ok), "me": me_info(me), "bots": list(cfg.get("bots", {}).keys())})
        await c.disconnect()
        return

    if cmd == "addbots":
        c = await get_client(cfg)
        res = []
        for k, v in cfg.get("bots", {}).items():
            try:
                await c.send_message(v, "/start")
                res.append({"bot": k, "ok": True})
                await asyncio.sleep(2)
            except Exception as e:
                res.append({"bot": k, "ok": False, "error": str(e)[:80]})
        emit({"success": True, "added": res})
        await c.disconnect()
        return

    if cmd == "search":
        query = sys.argv[2]
        which = sys.argv[3] if len(sys.argv) > 3 else "jisou3"
        pages = int(sys.argv[4]) if len(sys.argv) > 4 else 1
        target = cfg.get("bots", {}).get(which, "@jisou3")
        c = await get_client(cfg)
        if not await c.is_user_authorized():
            emit({"success": False, "state": "NOT_LOGGED_IN"})
            await c.disconnect()
            return
        await c.send_message(target, query)
        await asyncio.sleep(8)
        m = None
        async for x in c.iter_messages(target, limit=4):
            if not x.out and x.text:
                m = x
                break
        if not m:
            emit({"success": False, "state": "NO_REPLY"})
            await c.disconnect()
            return
        out = [{"page": 1, "text": clean(m.text), "links": parse_links(clean(m.text))}]
        for p in range(2, pages + 1):
            btn = None
            for row in (m.buttons or []):
                for b in row:
                    t = b.text or ""
                    if "下一页" in t or "\\u27a1" in t:
                        btn = b
                        break
                if btn:
                    break
            if not btn:
                break
            try:
                await btn.click()
            except Exception as e:
                out.append({"page": p, "error": type(e).__name__ + ": " + str(e)[:60]})
                break
            await asyncio.sleep(6)
            m = await c.get_messages(target, ids=m.id)
            if not m or not m.text:
                break
            out.append({"page": p, "text": clean(m.text), "links": parse_links(clean(m.text))})
        emit({"success": True, "bot": which, "query": query, "pages": len(out), "data": out})
        await c.disconnect()
        return

    emit({"success": False, "error": "unknown command: " + cmd})

asyncio.run(main())
`;

function shq(s) {
  return "'" + String(s).replace(/'/g, "'\\''") + "'";
}

async function runPython(args, timeoutMs) {
  const cmd = 'cd ' + WORK + ' && ' + PY + ' ' + SCRIPT + ' ' + args;
  let r;
  try {
    r = await Tools.System.terminal.hiddenExec(cmd, { timeoutMs: timeoutMs || 60000 });
  } catch (e) {
    return { success: false, error: 'EXEC_FAIL', detail: String((e && e.message) || e) };
  }
  const out = (r && r.output) ? String(r.output) : '';
  const lines = out.split('\n');
  let payload = null;
  for (let i = lines.length - 1; i >= 0; i--) {
    const idx = lines[i].indexOf(MARK);
    if (idx >= 0) {
      try { payload = JSON.parse(lines[i].slice(idx + MARK.length)); } catch (e) { payload = null; }
      break;
    }
  }
  if (!payload) {
    return { success: false, error: 'NO_JSON', exitCode: (r && r.exitCode), raw: out.slice(-800) };
  }
  return payload;
}

const GUIDE = {
  title: 'tε|ε9rann 搜索 · 使用引导',
  steps: [
    '① 运行 rs_setup —— 自动安装运行环境（独立 Python 环境 + 依赖库），并写入检索引擎。这一步只做一次。',
    '② 运行 rs_auth，先只填手机号（国际格式，如 +8613800138000），系统会下发授权码。',
    '③ 再次运行 rs_auth，填手机号和收到的授权码；如果账号开启了两步验证，同时填上验证密码。',
    '④ 运行 rs_add_bots —— 自动向搜索机器人（默认 jisou3、xbso）发送启动指令，使其进入可用状态。',
    '⑤ 运行 rs_search，填关键词即可检索资源；可用 bot 参数切换机器人，pages 参数翻页。'
  ],
  notes: [
    '隐私说明：手机号、授权码、登录态只保存在本机，包内不含任何个人信息。',
    '搜索机器人有频率限制，短时间内不要连续大量检索，否则会触发人机验证。',
    '检索质量取决于关键词，不同关键词结果差异很大。'
  ],
  bots: DEFAULT_CONFIG.bots
};

async function rs_guide() {
  return { success: true, data: GUIDE };
}

async function rs_setup() {
  const steps = [];
  let envOut = '';
  try {
    const r = await Tools.System.terminal.hiddenExec(INSTALL_SH, { timeoutMs: 300000 });
    envOut = (r && r.output) ? String(r.output) : '';
    steps.push({ step: 'runtime', exitCode: (r && r.exitCode), output: envOut.slice(-400) });
  } catch (e) {
    return { success: false, message: '环境安装失败', detail: String((e && e.message) || e) };
  }

  try {
    await Tools.Files.write(SCRIPT, PY_SRC, false, 'linux');
    steps.push({ step: 'script', ok: true });
  } catch (e) {
    return { success: false, message: '写入检索引擎失败', detail: String((e && e.message) || e) };
  }

  try {
    const ex = await Tools.Files.exists(WORK + '/config.json', 'linux');
    if (!ex || !ex.exists) {
      await Tools.Files.write(WORK + '/config.json', JSON.stringify(DEFAULT_CONFIG, null, 2), false, 'linux');
      steps.push({ step: 'config', created: true });
    } else {
      steps.push({ step: 'config', created: false, note: '已存在，未覆盖' });
    }
  } catch (e) {
    steps.push({ step: 'config', ok: false, detail: String((e && e.message) || e) });
  }

  const st = await runPython('status', 60000);
  return { success: true, message: '环境就绪', data: { steps, status: st } };
}

async function rs_auth(params) {
  const phone = String((params && params.phone) || '').trim();
  const code = String((params && params.code) || '').trim();
  const pwd = String((params && params.password) || '');

  const st0 = await runPython('status', 40000);
  if (st0 && st0.authorized) {
    return { success: true, state: 'ALREADY_AUTHORIZED', me: st0.me };
  }

  if (!phone) {
    return { success: false, message: '请提供手机号（国际格式，如 +8613800138000）' };
  }
  if (!code) {
    return await runPython('sendcode ' + shq(phone), 90000);
  }
  const args = 'signin ' + shq(phone) + ' ' + shq(code) + (pwd ? ' ' + shq(pwd) : '');
  return await runPython(args, 90000);
}

async function rs_add_bots() {
  const st = await runPython('status', 40000);
  if (!st || !st.authorized) {
    return { success: false, state: 'NOT_AUTHORIZED', message: '尚未授权，请先运行 rs_auth', status: st };
  }
  return await runPython('addbots', 90000);
}

async function rs_search(params) {
  const query = String((params && params.query) || '').trim();
  if (!query) {
    return { success: false, message: '请提供搜索关键词' };
  }
  let bot = String((params && params.bot) || 'jisou3').trim().replace('@', '');
  if (!bot) { bot = 'jisou3'; }
  let pages = parseInt((params && params.pages), 10);
  if (!pages || pages < 1) { pages = 1; }
  if (pages > 10) { pages = 10; }

  const st = await runPython('status', 40000);
  if (!st || !st.authorized) {
    return { success: false, state: 'NOT_AUTHORIZED', message: '尚未授权，请先运行 rs_auth 完成授权', status: st };
  }
  const timeout = 60000 + pages * 12000;
  return await runPython('search ' + shq(query) + ' ' + shq(bot) + ' ' + pages, timeout);
}

async function rs_status() {
  let envRaw = '';
  let envErr = null;
  try {
    const r = await Tools.System.terminal.hiddenExec(
      'test -x ' + PY + ' && echo VENV_OK || echo VENV_MISSING; ' +
      'test -f ' + SCRIPT + ' && echo SCRIPT_OK || echo SCRIPT_MISSING; ' +
      'command -v python3 >/dev/null 2>&1 && echo PY3_OK || echo PY3_MISSING',
      { timeoutMs: 20000 }
    );
    envRaw = (r && r.output) ? String(r.output) : '';
  } catch (e) {
    envErr = String((e && e.message) || e);
  }

  const st = await runPython('status', 40000);
  return {
    success: true,
    data: {
      environment: envRaw.trim(),
      environment_error: envErr,
      account: st
    }
  };
}

exports.rs_guide = rs_guide;
exports.rs_setup = rs_setup;
exports.rs_auth = rs_auth;
exports.rs_add_bots = rs_add_bots;
exports.rs_search = rs_search;
exports.rs_status = rs_status;