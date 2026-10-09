# alpha13 验证记录

2026-10-10，versionCode 13。

## 启动存储异常

alpha12 CI 37.0 的原生安装成功后，首项 App 测试未能解析活动，日志同时出现 Room 读取时 `SQLiteCantOpenDatabaseException`（数据库目录缺失）。运行环境仍有 SurfaceFlinger 重启，不能断言目录缺失的根因在 App；但仓库 initialize 只捕获 ApiFailure，其他本地存储异常会逃逸到启动协程，这是独立需要处理的问题。

初始化改为把设置／凭据／缓存读取异常转为 STORAGE 状态，保留已读到的连接身份和原凭据，不清理用户数据、不伪造成功缓存；协程取消继续向上传播。新增三项 JVM 回归：数据库不可读后恢复并成功刷新、设置读取失败不删除凭据、取消不误报存储错误。50 项 JVM 测试、lint、应用与测试 APK 构建通过。API 37 正常 AppFlow 三项回归通过（19.147 秒）。这里的故障注入验证仓库边界，不声称覆盖整个 App 的所有存储故障路径。

## 无障碍测试环境

alpha12 最新代码在 API 37 software 后端的五项组件渲染均通过；同轮 TalkBack 被系统弹窗遮挡，整轮明确失败（177.558 秒）。补充超时截图与节点日志后证实根节点为 System UI 无响应对话框，另有通知权限弹窗。

冷启动 4GB 模拟器，探针仅临时授予 TalkBack 通知权限、finally 恢复原权限；原始焦点／双击／一次请求／62%→83% 断言独立通过（19.861 秒）。外部确认 font_scale=1.0、enabled_accessibility_services=null、TalkBack POST_NOTIFICATIONS=false，均恢复原值。结合 API 29／31／35／36 的渲染 CI，D09 完成；E06 的完整最新版本 CI 仍未通过。

## Android 17 云端图形问题

CI 37998260240 全部终态，29／31／35／36（包括真实改时及真实网络探针）成功；37.0／37.2 software 后端仍出现相同 SurfaceFlinger 断言，CI gate 失败，证明单纯切软件后端未解决问题。

根据 Gfxstream 当前源码试验关闭 GlDirectMem 和 HasSharedSlotsHostMemoryAllocator；本地启动检查一度通过，但实际 instrumentation 启动遇到 DeadObjectException。此实验未证明稳定，已撤回，不提交到 CI。保留原始失败证据，Android 17 完整矩阵仍待解决。

## 系统省电恢复探针（E03 部分证据）

新增 `scripts/power_smoke.sh`，仅供受控模拟器运行，沿用拦截全部 HTTP 的合成账号 fixture。API 29 实际进入深度 Doze（IDLE）、开启系统省电（low_power=1）、设置 RUN_ANY_IN_BACKGROUND=ignore，各保留 5 秒并保存 JobScheduler 状态。退出限制后外部强停再启动，逐项断言原 Keystore 连接身份、Room 快照及 fetchedAt、组件绑定／设置／38% 展示保留，唯一周期任务恢复，零额度重新请求。每个模式均有 seed 与 restore 两阶段，共六项。

脚本对 adb 操作设置 90 秒上限，EXIT 恢复 forced-idle、省电、后台 app-op、模拟电池状态；仅允许初始未强制 idle／未省电／后台允许的模拟器，避免覆盖已有实验。它不会宣称五秒观察等价于长期省电验收，也不证明 Doze 内真实后台执行／延迟或 OEM 行为。E03 仍未完成，另需限制期间任务与请求次数的证据。已有 Worker 失败回归覆盖每轮最多三次尝试，但不能拿直接 Worker 调用替代系统后台调度验证。

最终脚本重跑六阶段均成功：0.367／3.010、0.270／3.438、0.297／2.721 秒（分别 seed／restore）。外部复核 deep=ACTIVE、low_power=0、后台 app-op=default，电池恢复 AC 供电且未处于模拟冻结。后台受限 JobScheduler 证据出现 readyNotRestrictedInBg=false 和 WAITING；Doze 记录仍有 readyNotDozing=true，因此不能推断所有任务已受 Doze 阻挡，仍保留 E03 待办。

进一步检查发现该 AOSP 模拟器 mDeepEnabled=false：此前 IDLE 状态不能证明深度 Doze 已实际作用到 JobScheduler。修正 harness 临时启用 deep、关闭屏幕，额外要求 readyNotDozing=false；后台限制也要求 readyNotRestrictedInBg=false。修正后六阶段通过（0.255／3.209、0.269／3.205、0.267／3.301 秒），原 deep=0 与 screen=true 均恢复。此结果取代此前的 Doze 调度条件证据，不抹去先前验证范围不足的记录。

最终 harness 在整个合成夹具存续期禁用模拟器 Wi-Fi 与移动网络，退出时先停止 App 再恢复原网络传输状态，防止 instrumentation 结束后系统另起的普通进程绕过测试拦截器。它证明系统 Doze／后台限制条件被正确阻挡，但网络也同时被隔离，不能声称单独测出了 Doze 的延迟或限流。CI 为 API 29 增加此探针，其他版本尚待扩展。

网络隔离最终版本六阶段均成功（0.258／3.579、0.309／2.953、0.306／3.327 秒）；外部复核 Wi-Fi=1、mobile_data=1、low_power=0、后台 app-op=default，均恢复原值。ShellCheck、diff 检查、20 项交付脚本测试通过。

单独关闭 GLDirectMem 的本地对照也失败：模拟器 37.2.12 日志确认该 flag 已 disabled，但 API 37.0 ARM64 在任何 App 测试前约 20 秒即发生同一 SurfaceFlinger／RegionSampling 的 ReadColorBufferDma 断言，system_server 不在运行。已保存本地 crash buffer 并关闭该受控模拟器，未将无效参数提交到 CI。此证据表明旧 emulator feature 开关并不能保证当前 Gfxstream 的扩展广播已关闭，不能据当前主仓库源码推断所安装二进制的行为。

## 官方上一稳定版运行环境对照

官方 [发布说明](https://developer.android.com/studio/releases/emulator) 明确 37.1.11 Stable 支持 API 37 所需 Vulkan 扩展。按 [官方归档](https://developer.android.com/studio/emulator_archive) 下载 Apple Silicon 构建 15917651，SHA-256 校验为 `22530de9363f34ea945ecb5cad74523abd4b615f27f3c1a9899efb183ea9e144`，独立解压到忽略的工具目录，没有替换当前 SDK。保留原 API 37.0 ARM64 镜像、4GB RAM、software 后端及全部测试，不添加无效的直接内存开关。

37.1.11 已通过实际框架稳定准备检查，启动 crash buffer 尚无同一断言；完整 18 项普通设备回归已启动，结果待定。这个对照用于确认模拟器版本与故障关系，不把开始运行当作验证通过。

进一步查阅 emulator emu-master-dev 的 opengles.cpp 可见旧 feature 到 Gfxstream feature 的显式映射，因此“两套入口直接导致失效”的解释并未获得证明。已安装二进制与所阅分支之间的差异仍未知，以实际对照为准。
