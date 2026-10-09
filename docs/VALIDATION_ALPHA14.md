# alpha14 验证记录

2026-10-10，versionCode 14。本轮补刷新仓库的存储异常边界，并修复固定 Android 17 模拟器的 Linux 运行依赖。

## 刷新存储边界

三个新 JVM 回归先在原实现明确失败：刷新前设置读取 IOException、凭据读取 IOException、处理 503 时记录失败的设置写入 IOException，均逃逸到调用方。修复后保留原成功快照与凭据，以 STORAGE 状态和可重试结果返回；协程取消继续传播，取消后的回滚写入失败不替换原取消异常。

如果 Retry-After 写入失败，同进程仍需遵守截止时间：增加受 Mutex 保护、绑定账号 generation 的内存 guard，与磁盘截止时间取更严格值。新增回归确认清除短尝试保护后仍不发送额外 HTTP，另一个账号 generation 不受旧 guard 影响。未能写盘的 guard 无法保证跨进程持久化；这不是整个 App 的所有存储故障路径验收。

56 项 JVM 测试零失败／零错误，lint、应用及测试 APK 构建通过。API 37 ARM64／emulator 37.1.11 的页面、手动刷新、Worker 七项定向回归首轮六项通过，一项旧夹具在人工清空磁盘 Retry-After 后被内存 guard 拦住，整轮明确失败（27.754 秒）。夹具改短 Retry-After 并等待实际截止时间，不删除限流期间请求数、缓存保留和恢复断言，最终原七项回归全部通过（33.328 秒，7 个成功状态及正常 instrumentation 完成码）。

## Android 17 CI 依赖

固定 37.1.11 的 CI 38001424560 两个 Android 17 job 在运行时加载前失败：缺少 libpulse.so.0，未执行任何 App 测试。下载／SHA-256 校验已通过；添加仅 Android 17 job 的 libpulse0 安装步骤。此日志不能证明原 SurfaceFlinger 问题已经解决，仍需新云端完整套件及 16KB 镜像结果。

## 本地开发包交付

在干净提交 ac3b6df6455856352fc546203ce844e16375f27a 再次执行 assembleDebug 后，使用现有产物工具打包并独立 verify 成功。本地包 `.tools/delivery-alpha14/quotapal.apk` 的 applicationId=com.tsonglew.quotapal、versionCode=14、versionName=0.1.0-alpha14，aapt2 实际清单一致。APK SHA-256：`eabca36b9c5c6fbfb06e52674cef3039b4ad7c856e36f1f3b21a74fbd8516a31`；metadata.json 与 SHA256SUMS 同目录。

apksigner 实际验证 v2 签名成功；本地 Android Debug 证书 SHA-256 为 `a83328765a27dbb4713f363f5869edbfea78603cf1dc78563d41e6dcafe63290`。这是供自用验收的开发产物，不是固定发布签名或已通过完整云端 gate 的 Release，也不计为 G08 deployment 验收。

## 云端终态与有效图形配置对照

CI 38002272484 全部终态：API 29／31／35／36 成功，37.0／37.2 在补齐 libpulse0 并实际启动 37.1.11 后仍复现相同 SurfaceFlinger／ReadColorBufferDma 断言，安装时系统服务不可用；gate 失败。降级未解决云端问题，已从脚本撤回固定运行环境，保留完整镜像与测试范围。

本地 37.2.12 的 verbose 日志确认 GLDirectMem 开关实际传入了 Gfxstream（GlDirectMem=0），此前“未生效”的解释不成立。继续同时关闭 GLDirectMem／GLDMA／HasSharedSlotsHostMemoryAllocator，三项实际渲染器值均为 0，仍在 App 测试前复现原断言，因此未将该无效配置加入 CI。

依据官方 emulator 发布说明测试 VulkanNativeSwapchain composition，日志确认 VulkanNativeSwapchain=1、GuestVulkanOnly=1，自动启用 GuestAngle；通过框架稳定准备，启动 crash buffer 为空。完整原生普通套件 18 项实际测试全部通过（129.937 秒，另 4 项条件探针跳过），包含真实小组件点击及渲染断言。CI 候选使用相同两个 feature 参数并保留 verbose 实际值日志；本地 ARM64 通过不证明云端 x86_64／16KB，不据此勾选 E06。

- Vulkan composition 补充回归：外部强停 seed／restore 均通过，真实时区／时钟回退／DST 探针通过（9.6 秒），结束 crash buffer 为空。

- CI 38004158377 终态：29／31／36 成功；35 真实刷新测试查找 TextView 上的描述失败，改为等待完整视图树中唯一刷新语义与实际点击入口，本地 API 35 两项刷新测试（9.088 秒）及完整 18 项普通套件（123.699 秒）通过，保留三组件／单请求／刷新中断言。37 两组在渲染器初始化前失败，实际日志 API level: 3、Vulkan=0、VulkanNativeSwapchain=1；本地同路径 API level: 37、Vulkan=1。下一候选显式开启 Vulkan 以消除该配置冲突，仍须云端完整验证，不认定已修复。X11 备用库名加载成功，不归因缺库。
