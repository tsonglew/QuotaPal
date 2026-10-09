# alpha14 验证记录

2026-10-10，versionCode 14。本轮补刷新仓库的存储异常边界，并修复固定 Android 17 模拟器的 Linux 运行依赖。

## 刷新存储边界

三个新 JVM 回归先在原实现明确失败：刷新前设置读取 IOException、凭据读取 IOException、处理 503 时记录失败的设置写入 IOException，均逃逸到调用方。修复后保留原成功快照与凭据，以 STORAGE 状态和可重试结果返回；协程取消继续传播，取消后的回滚写入失败不替换原取消异常。

如果 Retry-After 写入失败，同进程仍需遵守截止时间：增加受 Mutex 保护、绑定账号 generation 的内存 guard，与磁盘截止时间取更严格值。新增回归确认清除短尝试保护后仍不发送额外 HTTP，另一个账号 generation 不受旧 guard 影响。未能写盘的 guard 无法保证跨进程持久化；这不是整个 App 的所有存储故障路径验收。

56 项 JVM 测试零失败／零错误，lint、应用及测试 APK 构建通过。API 37 ARM64／emulator 37.1.11 的页面、手动刷新、Worker 七项定向回归首轮六项通过，一项旧夹具在人工清空磁盘 Retry-After 后被内存 guard 拦住，整轮明确失败（27.754 秒）。夹具改短 Retry-After 并等待实际截止时间，不删除限流期间请求数、缓存保留和恢复断言，最终原七项回归全部通过（33.328 秒，7 个成功状态及正常 instrumentation 完成码）。

## Android 17 CI 依赖

固定 37.1.11 的 CI 38001424560 两个 Android 17 job 在运行时加载前失败：缺少 libpulse.so.0，未执行任何 App 测试。下载／SHA-256 校验已通过；添加仅 Android 17 job 的 libpulse0 安装步骤。此日志不能证明原 SurfaceFlinger 问题已经解决，仍需新云端完整套件及 16KB 镜像结果。
