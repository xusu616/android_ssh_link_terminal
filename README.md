# SSH Link Terminal

一款面向 Android 设备的 **Cyber-Glass 简洁假终端 & Shizuku 持久守护 SSH 客户端**。

参考 [shizuku-ftp](file:///D:/Users/xvsu/AndroidStudioProjects/shizuku-ftp) 的现代工程架构与毛玻璃视觉体系构建，专为解决移动终端使用 Termux `ssh` 时的四大痛点而设计：

1. **连接保活与断线自愈**：内置 SSH Transport KeepAlive（`ServerAliveInterval`）+ 实时 RTT 延迟采集 + `ConnectivityManager.NetworkCallback` 切网感知自动重连，可配 `tmux new-session -A -s main` 无感续接远端现场。
2. **密码与私钥安全保存**：基于 `AndroidKeyStore` AES-256-GCM 加密保存主机密码与私钥口令；内置 **Ed25519 密钥对生成器**与一键写入远端 `~/.ssh/authorized_keys`。
3. **Tailscale / Antigravity OAuth 自动捕捉与一键跳转**：完整处理 SSH Banner 与 `keyboard-interactive` 验证流；自动识别终端输出或认证挑战中的 `login.tailscale.com/a/...`、`accounts.google.com/...` 等 OAuth 验证 URL，弹出灵动授权胶囊（支持自动唤起 Chrome Custom Tabs），终端内所有链接亦可直接点击跳转浏览器。
4. **Shizuku 守护与防杀后台加固**：Android 14+ 合规 `specialUse` 前台服务 + `WakeLock`/`WifiLock` + Shizuku `daemon(true)` 独立守护进程，一键写入 Doze 白名单（`cmd deviceidle whitelist`）、`Standby Active`（`am set-standby-bucket`）与 `AppOps` 后台放行，并在主进程异常被回收时尝试自动拉起守护服务。

## 工程技术栈

- **Gradle 9.3.0 / AGP 9.0.1 / Kotlin 2.3.20 / Java 21 Toolchain**
- **Version Catalog (`gradle/libs.versions.toml`) + Release 自动递增版本号 (`app/version.properties`)**
- **Jetpack Compose + Material 3 + `Haze` 毛玻璃 + `Palette` 动态取色 + `Vico` 延迟监控图表**
- **SSH 引擎**：`com.github.mwiede:jsch`（支持现代 Ed25519 / RSA-SHA2 / PTY / `keyboard-interactive`）
- **特权守护**：`dev.rikka.shizuku:api` & `provider` + AIDL `IShizukuDaemonService`

## 构建与测试

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```
