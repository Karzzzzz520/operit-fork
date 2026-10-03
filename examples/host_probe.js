/*
METADATA
{
  "name": "host_probe",
  "display_name": {
    "zh": "主机探测（真·可执行版）",
    "en": "Host Probe (real exec v2)"
  },
  "description": "基于已验证通道 Tools.System.shell（大写 S）执行 shell、读取系统属性、操作文件，产出真正可工作的设备管理工具。",
  "author": "Sakuya Izayoi",
  "enabledByDefault": true,
  "category": "Utility",
  "tools": [
    {
      "name": "shell",
      "description": "在设备上执行一条 shell 命令，走 Tools.System.shell 已验证通道，返回完整输出。",
      "parameters": [
        { "name": "command", "description": "要执行的 shell 命令", "type": "string", "required": true },
        { "name": "timeout", "description": "超时毫秒，默认 15000", "type": "number", "required": false }
      ]
    },
    {
      "name": "sys_probe",
      "description": "读取本机系统信息（Android 版本、机型、品牌、SDK、内核、内存），走 Tools.System.shell。",
      "parameters": []
    },
    {
      "name": "file_probe",
      "description": "验证 Tools.Files 文件读写通道：列目录、读文件、写临时文件再读回。",
      "parameters": []
    },
    {
      "name": "device_actions",
      "description": "设备基础操作（只读探测）：屏幕尺寸、WiFi 状态、电池状态、当前时间。",
      "parameters": []
    }
  ]
}
*/

const hostProbe = (function () {
  function g() { return (typeof globalThis !== "undefined") ? globalThis : {}; }
  function tools() { return g().Tools || {}; }

  // 统一执行 shell
  async function runShell(command, timeout) {
    const T = tools();
    const shellFn = T.System && T.System.shell;
    if (typeof shellFn !== "function") {
      throw new Error("Tools.System.shell 不可用");
    }
    const r = await shellFn(command, timeout || 15000);
    // r 可能是 {output} 也可能是字符串
    if (r == null) return "";
    if (typeof r === "string") return r;
    if (r.output != null) return String(r.output);
    if (r.stdout != null) return String(r.stdout);
    return String(r);
  }

  async function shell(params) {
    const command = String((params && params.command) || "").trim();
    if (!command) return complete({ success: false, message: "command 不能为空" });
    try {
      const timeout = Number((params && params.timeout) || 15000);
      const output = await runShell(command, timeout);
      return complete({ success: true, data: { command, output: String(output).slice(0, 4000) } });
    } catch (e) {
      return complete({ success: false, message: String(e && e.message ? e.message : e) });
    }
  }

  async function sys_probe(params) {
    try {
      const out = {};
      const props = {
        release: "getprop ro.build.version.release",
        sdk: "getprop ro.build.version.sdk",
        model: "getprop ro.product.model",
        brand: "getprop ro.product.brand",
        manufacturer: "getprop ro.product.manufacturer",
        device: "getprop ro.product.device",
        kernel: "uname -r",
        abi: "getprop ro.product.cpu.abilist",
      };
      out.properties = {};
      for (const key of Object.keys(props)) {
        out.properties[key] = String(await runShell(props[key])).trim().slice(0, 200);
      }
      // 内存
      out.meminfo = String(await runShell("cat /proc/meminfo | head -5")).trim().slice(0, 600);
      out.jsDate = new Date().toISOString();
      return complete({ success: true, data: out });
    } catch (e) {
      return complete({ success: false, message: String(e && e.message ? e.message : e) });
    }
  }

  async function file_probe(params) {
    try {
      const F = tools().Files;
      if (!F) return complete({ success: false, message: "Tools.Files 不可用" });
      const out = {};

      // 列目录
      out.listHome = await Promise.resolve().then(async () => {
        try { return await F.list("/sdcard"); } catch (e) { return "<err:" + String(e && e.message ? e.message : e) + ">"; }
      });

      // 写临时文件再读回
      const tmp = "/sdcard/Download/host_probe_tmp.txt";
      try {
        const w = await F.write(tmp, "红魔馆女仆长自检:" + new Date().toISOString());
        out.writeResult = w;
        const r = await F.read(tmp);
        out.readBack = r;
        out.filesMethods = Object.keys(F);
      } catch (e) {
        out.fileErr = String(e && e.message ? e.message : e);
      }
      return complete({ success: true, data: out });
    } catch (e) {
      return complete({ success: false, message: String(e && e.message ? e.message : e) });
    }
  }

  async function device_actions(params) {
    try {
      const out = {};
      out.screen = String(await runShell("wm size; wm density")).trim().slice(0, 300);
      out.wifi = String(await runShell("dumpsys wifi | grep -i 'Wi-Fi is' | head -3")).trim().slice(0, 200);
      out.battery = String(await runShell("dumpsys battery | grep -iE 'level|status|temperature' | head -5")).trim().slice(0, 300);
      out.uptime = String(await runShell("cat /proc/uptime")).trim().slice(0, 100);
      out.deviceTime = String(await runShell("date")).trim().slice(0, 150);
      out.jsDate = new Date().toISOString();
      return complete({ success: true, data: out });
    } catch (e) {
      return complete({ success: false, message: String(e && e.message ? e.message : e) });
    }
  }

  return {
    shell: shell,
    sys_probe: sys_probe,
    file_probe: file_probe,
    device_actions: device_actions,
  };
})();

exports.shell = hostProbe.shell;
exports.sys_probe = hostProbe.sys_probe;
exports.file_probe = hostProbe.file_probe;
exports.device_actions = hostProbe.device_actions;