# alpha10 后台故障与有界重试

2026-10-10，versionCode 10／0.1.0-alpha10。

UsageSyncWorker 对一次任务周期最多执行 3 次尝试，之后返回 failure，保留仓库的失败状态与原成功快照；新手动操作和后续周期可以恢复。429 等待期内不增加 HTTP 请求，达到本轮上限也不清除 Retry-After。设置页面说明持续失败时的恢复方式。

新增仅测试包的传输故障控制和 Worker 设备回归，覆盖离线／超时异常、503、429、缓存不变、次数上限及新尝试恢复。使用 [TestListenableWorkerBuilder 官方测试 API](https://developer.android.com/develop/background-work/background-tasks/testing/persistent/worker-impl) 注入尝试次数；不改变全局 WorkManager 初始化，也不发送合成凭据到真实服务。

验证结果如下。设备传输故障不等同于真实切断网络或 Doze／系统省电、OEM 后台限制验证；E02／E03 保留完整验收。

第一轮 alpha10 本地完整套件：19 个入口，18 项实际执行通过、1 项外部生命周期入口按条件跳过（169.094 秒）；随后外部强停 seed／restore 独立通过（0.876／8.115 秒）。格式提示最后调整后，AppFlowTest 与 SyncWorkerDeviceTest 五项重跑通过（21.665 秒），47 项 JVM、lint、构建通过。核对当前实际 WorkManager 2.10.1 AAR 的 WorkerWrapper：周期任务收到 failure 会 resetPeriodic 并清零尝试次数，下一周期仍可运行；不把单轮失败解释为永久取消周期同步。

alpha09 CI 37992734085：API 31／35／36 普通设备套件成功，后续独立 instrumentation 启动失败，补执行前重新安装 App／测试 APK。API 29 的 130×50 大字体时间裁切仍存在，进一步统一紧凑布局的最小高度占用，保留全部裁切／省略检查；新布局待 API 29 云端回归。Android 17 两组最终均因内部存储不足在 APK 安装阶段失败，未执行 App 测试。

最终紧凑最小高度排版在本地 API 37 六种尺寸及额度边界回归通过（22.909 秒）；构建、lint、actionlint、ShellCheck 和 16 项交付脚本测试通过。API 29 仍需下一轮云端证明。API 37.0 CI 现已成功列出 AVD 并通过框架就绪，失败推进到安装，明确为 Requested internal only, but not enough space；配置 4 GB data 分区并保存安装前 df 输出，等待云端验证。

当前 debug APK SHA-256：`929ef1f6cde878beff2970f1e5fd14542a05c551156bf9321f4d1b3a796f7d72`。

## 实际 TalkBack 焦点与激活

API 37 Google APIs 镜像使用已安装的真实 TalkBack，UiAutomation 保留其他无障碍服务，确认 TalkBack 已绑定且触摸探索启用。200% 字体下分别绑定标准和 2×1 provider，从无障碍树确认额度与最后成功时间；将实际无障碍焦点移到刷新入口，注入双击后合成额度从剩余 62% 更新为 83%，各只发一次请求。首次通过 15.225 秒；加入服务确认及截图后重跑通过 18.348 秒。

截图已查看：[标准组件焦点](screenshots/talkback-standard-focus.png)、[2×1 更新结果](screenshots/talkback-slim-updated.png)，可见 TalkBack 绿色焦点框。仅证明焦点、触摸激活及可访问树内容，不宣称听取了实际语音。测试完成恢复 enabled_accessibility_services=null、accessibility_enabled=0、font_scale=1.0 并清除合成账号。普通套件跳过该显式探针，避免在不含 TalkBack 的镜像上伪造通过。D09 的 API 29 大字体仍等 CI，不提前勾选。

CI 37994070738 的 API 37.0 在 SDK emulator 下载遇到 Error on ZipFile unknown archive，未创建或启动 AVD，和上一轮设备 data 空间不足不同。SDK 安装增加最多三次有界尝试；故障注入确认前两次失败可在第三次继续、持续失败三次后终止。ShellCheck、actionlint 通过，其他 CI job 仍在运行。
