# Operit Fork API Test

目标宿主：`com.ai.assistance.operit`（debug 与 release 同包名，见 README）

测试包：`com.operit.fork.api_test`

自包含探针包：根目录 `manifest.json` + `main.js`（保守 JS，不使用可选链 / 箭头函数 / 模板字符串）。

## 它检查什么

1. ToolPkg 运行时、生命周期注册 API、沙盒文件 API；
2. `OperitFork` 全局是否存在；
3. `OperitFork.isAvailable()` / `listCapabilities()` / `getHostInfo()`；
4. `secureTokenStore.put` / `get` 往返；
5. `hostInfo.injected === true`。

启用后注册 `application_on_create` 生命周期 Hook，运行探针并把报告写入
配置目录的 `fork_api_test_report.json`，同时打印 `passed N/M`。

## 打包

```bash
python3 tools/package_fork_api_test.py   # 生成 com.operit.fork.api_test.toolpkg（ZIP_STORED）
```

也可直接调用 `runForkApiTest()` / `writeReport()` 导出函数做单次探测。
