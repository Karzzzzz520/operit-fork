# release/version.json

供应用内「检查更新」读取的版本清单。应用优先通过 jsDelivr 读取它：

```
https://cdn.jsdelivr.net/gh/Karzzzzz520/operit-fork@main/release/version.json
```

GitHub API（`api.github.com`）只在清单不可用时兜底，因为它在部分网络下不可达或限流。

## 字段

| 字段 | 说明 |
| --- | --- |
| `version` | 版本号，与 `versionName` 一致，如 `1.12.2-f1` |
| `updateUrl` | Release 页面地址 |
| `downloadUrl` | APK 直链（GitHub Releases redirect 地址） |
| `notes` | 可选说明，留空即可 |

## 发布新版本时

1. 构建并发布对应 tag 的 Release（`fork-<版本>-f<N>`）；
2. 把本文件的 `version` / `updateUrl` / `downloadUrl` 更新为新版本并提交。

jsDelivr 对 `@main` 有缓存，更新可能延迟数小时生效。
