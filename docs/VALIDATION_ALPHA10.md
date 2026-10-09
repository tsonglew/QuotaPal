# alpha10 后台故障与有界重试

2026-10-10，versionCode 10／0.1.0-alpha10。

UsageSyncWorker 对一次任务周期最多执行 3 次尝试，之后返回 failure，保留仓库的失败状态与原成功快照；新手动操作和后续周期可以恢复。429 等待期内不增加 HTTP 请求，达到本轮上限也不清除 Retry-After。设置页面说明持续失败时的恢复方式。

新增仅测试包的传输故障控制和 Worker 设备回归，覆盖离线／超时异常、503、429、缓存不变、次数上限及新尝试恢复。使用 [TestListenableWorkerBuilder 官方测试 API](https://developer.android.com/develop/background-work/background-tasks/testing/persistent/worker-impl) 注入尝试次数；不改变全局 WorkManager 初始化，也不发送合成凭据到真实服务。

验证结果如下。设备传输故障不等同于真实切断网络或 Doze／系统省电、OEM 后台限制验证；E02／E03 保留完整验收。

第一轮 alpha10 本地完整套件：19 个入口，18 项实际执行通过、1 项外部生命周期入口按条件跳过（169.094 秒）；随后外部强停 seed／restore 独立通过（0.876／8.115 秒）。格式提示最后调整后，AppFlowTest 与 SyncWorkerDeviceTest 五项重跑通过（21.665 秒），47 项 JVM、lint、构建通过。核对当前实际 WorkManager 2.10.1 AAR 的 WorkerWrapper：周期任务收到 failure 会 resetPeriodic 并清零尝试次数，下一周期仍可运行；不把单轮失败解释为永久取消周期同步。

alpha09 CI 37992734085：API 31／35／36 普通设备套件成功，后续独立 instrumentation 启动失败，补执行前重新安装 App／测试 APK。API 29 的 130×50 大字体时间裁切仍存在，进一步统一紧凑布局的最小高度占用，保留全部裁切／省略检查；新布局待 API 29 云端回归。Android 17 两组最终均因内部存储不足在 APK 安装阶段失败，未执行 App 测试。

最终紧凑最小高度排版在本地 API 37 六种尺寸及额度边界回归通过（22.909 秒）；构建、lint、actionlint、ShellCheck 和 16 项交付脚本测试通过。API 29 仍需下一轮云端证明。API 37.0 CI 现已成功列出 AVD 并通过框架就绪，失败推进到安装，明确为 Requested internal only, but not enough space；配置 4 GB data 分区并保存安装前 df 输出，等待云端验证。

当前 debug APK SHA-256：`929ef1f6cde878beff2970f1e5fd14542a05c551156bf9321f4d1b3a796f7d72`。
