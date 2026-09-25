# PT管理宝（PTManager）

手机端管理 PT 下载服务器（Android 原生 + Jetpack Compose）。当前支持 **qBittorrent**。

## 功能

- **添加服务器**：填写名称 / qBittorrent WebUI 地址 / 用户名 / 密码，支持「测试连接」验证后再保存；凭据用 Android Keystore 加密存储。
- **种子列表**：查看所有种子，支持按名称/状态筛选与排序，下拉刷新。
- **种子详情**：名称、下载进度、上下行速度、分享率、剩余时间、状态、大小、上传量/下载量、tracker 列表、保存路径、文件构成。
- **种子操作**：开始 / 暂停 / 删除（可勾选删除文件）/ 重新检查 / 重新播种。
- **添加种子**：支持「URL 直链」与「上传 .torrent 文件」两种方式。
- 内建演示服务器（LocalQB）：模拟器中零配置即可直接看到完整数据流，便于开发调试。

### 技术要点（与本项目一起交付，供维护参考）

- 网络层：OkHttp + Retrofit + kotlinx.serialization，Cookie 持久化在应用内（`InMemoryCookieJar`）。
- 鉴权：`AuthInterceptor` 在收到 403/Forbidden 时自动带上凭据走一次 `/api/v2/auth/login` 拿到 SID 后再重放原请求。
- 凭据保密：不落明文，读取/写入经 Keystore 加解密（`androidx.security.crypto`）后存 SharedPreferences。

> 排查经验：若「测试连接」返回 **Forbidden**，先查服务器 qBittorrent WebUI 的
> **高级 → 启用 Host 头校验**（Host header validation）。它会对来自模拟器/代理转发的
> 连接校验 Host 头而返回 403，与 App 本身无关。宿主机侧用与 App 完全相同的请求
> （无 Referer、正确凭据）可稳定得到 200 + SID。

## 构建

环境要求：JDK 17、Android SDK（`local.properties` 中配置 `sdk.dir`）。

```bash
cd pt
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```
app/src/main/java/com/ptmanager/
├── data/
│   ├── api/       # Retrofit 接口、QbClientFactory、Auth/日志拦截器
│   ├── model/     # 数据模型（种子、服务器配置、tracker 等）
│   └── repo/      # QbRepository / ServerRepository
├── ui/
│   ├── nav/       # 导航
│   ├── servers/   # 服务器管理（列表 + 添加表单）
│   ├── torrents/  # 种子列表
│   ├── detail/    # 种子详情
│   └── add/       # 添加种子
└── util/          # 格式化工具
```

## 许可

仅本人使用与学习研究。禁止用于商业用途。
