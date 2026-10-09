# QuotaPal 验证记录与自用步骤

自动化结果以 [Android CI](https://github.com/tsonglew/QuotaPal/actions/workflows/android.yml) 为准；阶段进度见 [PROGRESS.md](PROGRESS.md)，完整验收见 [DEVELOPMENT_CHECKLIST.md](DEVELOPMENT_CHECKLIST.md)。

## 自动化验证

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
bash scripts/lifecycle_smoke.sh
```

- JVM：解析单／多窗口、缺失／非法百分比、重置时间、账号隔离、HTTP 错误、退避、并发请求、续期与退出时的旧响应。
- Android 页面：进入示例、切换标签、深色主题、退出清理，导出浅深色截图。
- Android 存储：Keystore 加密往返、密文篡改拒绝、凭据删除和备份关闭；组件配置互不覆盖。
- Android 组件：使用 debug 专用原生 AppWidgetHost，渲染真实 Glance RemoteViews，检查 140×150、280×150、140×230 dp，退出后切换到未连接状态。
- 可选额度回归：仅周额度、无窗口且可用、未提供周期额度、用量未知、服务端受限；检查解析和缓存替换、Compose 说明及三种真实组件尺寸，确保不虚构百分比且提示与更新时间完整可见。
- 2×1 原生组件：验证独立入口的 2×1 网格元数据，130×70、140×70、280×70 与横屏 260×50 dp；覆盖浅深色、已用／剩余、无周期、未知用量、受限和退出，逐项检查所有文字可见且不省略。与标准组件同时绑定时检查共享更新和独立设置。

组件测试临时通过系统 shell 授予绑定权限，结束时撤销。测试宿主仅包含在 debug 变体中，未导出，不进入 release。实现依据 [Android widget host 文档](https://developer.android.com/develop/ui/views/appwidgets/host) 和 [AOSP appwidget shell 命令](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/cmds/appwidget/src/com/android/commands/appwidget/AppWidget.java)。宿主测试不能替代真实 Launcher、系统选择器和 OEM 验证。

CI 产物 `android-build` 含 debug APK、单元测试与 lint 报告；设备矩阵覆盖 API 29、31、35、36，`android-device-tests-api-<API>` 各含对应设备测试报告和截图，保留 14 天。不上传凭据或真实账号数据。模拟器矩阵不替代 OEM Launcher、真实授权与连续自用。

## 首次自用验收

先用示例检查页面和组件，再连接自己的 Codex。当前接入处于实验阶段，兼容官方开源客户端协议；真实 Android 授权和公开发布适用范围仍需确认。不要复制电脑上的 `auth.json` 到手机。

设备码登录需要先在 ChatGPT 的安全设置中启用；工作区账号可能需要管理员在工作区权限中开启。此要求来自 [官方认证说明](https://learn.chatgpt.com/docs/auth#preferred-device-code-authentication-beta)。

| 步骤 | 应看到的结果 | 记录 |
| --- | --- | --- |
| 安装 debug APK，首次打开 | 未连接欢迎页，无虚构账号数据 | 待真机 |
| 点击“先看看示例” | 明确标记示例数据；剩余 62%／36% | 待真机 |
| 切换已用／剩余与浅深色 | 数字和分段条语义一致 | 待真机 |
| 点击连接，取消一次 | 关闭登录流程，无新账号残留 | 待真机 |
| 再次连接，打开 OpenAI 页 | 在系统浏览器授权，不在 App 输入密码 | 待真机 |
| 完成设备码授权 | 自动返回可刷新额度，账号／工作区正确 | 待真机 |
| 对照同账号官方客户端 | 窗口、百分比和重置时间相符，记录对照时刻 | 待真机 |
| 使用无短周期窗口的账号 | 保留周等长期额度，明确提示未提供短周期额度；主界面与组件一致 | 待真机 |
| 账号未返回任何周期窗口 | 有说明文字，不显示空卡片或虚构百分比；服务端确认可用时显示当前可用 | 待真机 |
| 经系统选择器添加组件 | 配置可取消、保存成功后出现在桌面 | 待真机 |
| 放置 3 个组件并缩放 | 各自主题可不同，共享同一更新时间与额度快照 | 待真机 |
| 断网后手动刷新 | 保留上次数据与成功时间，显示失败状态 | 待真机 |
| 恢复网络再刷新 | 获取新快照，App 与组件同步更新 | 待真机 |
| 退出并清除数据 | App 和全部组件进入未连接，后台同步取消 | 待真机 |
| 重新登录、进程回收、重启 | 仅恢复当前账号缓存，联网后正常同步 | 待真机 |
| 令牌实际到期后继续使用 | 续期成功，且无重复续期／账号混用 | 待真机 |

设备记录只包含机型、Android 版本、Launcher、测试时刻、结果和脱敏现象；不记录登录码、token、账号邮箱或完整服务响应。

## 长时间观察

2026-10-10 自用设备反馈：小米 17 Ultra，Android 17（用户更正后的报告），当前 APK 已成功连接真实 Codex 账号。此反馈仅证明用户完成首次连接，不作为额度对照、续期、退出／重连、OEM 组件操作或连续使用时长的通过证据。

48 小时后台观察和连续 7 天自用必须实际等待完成，不能用模拟器通过替代。记录实际刷新间隔、请求次数、失败与恢复、耗电和崩溃；Doze 与系统省电下允许刷新延迟，更新时间必须真实。

强制停止后，以重新打开 App 能恢复为验收条件。Pixel/AOSP、Samsung、小米 Launcher 以及最低 Android 版本仍需分别执行。公开发布前继续完成签名、升级迁移、渠道与隐私政策验收。

## 外部生命周期回归

`lifecycle_smoke.sh` 使用测试包的合成账号拦截器，seed 写入真实 Keystore／Room 并绑定平台组件，外部停止进程后 restore 检查缓存时间、账号、同一组件 ID／独立设置、组件重新显示和唯一周期任务。普通测试套件跳过该两阶段测试，CI 在普通套件通过后单独执行，保存 lifecycle-seed.txt／lifecycle-restore.txt；am instrument 的退出码不能证明通过，脚本还要求 OK (1 test)。

仅在测试模拟器上使用 `LIFECYCLE_RESTART=reboot` 执行实际设备重启，或先装旧版 debug APK、再用 `LIFECYCLE_RESTART=upgrade LIFECYCLE_APK=/absolute/path/new.apk` 验证覆盖升级。升级断言要求版本号实际增加，不接受同版本重装作为版本迁移证明。`LIFECYCLE_OUTPUT_DIR` 可改证据目录。测试结束清除合成账号及测试组件；失败后如需恢复可重跑完整 harness。真实账号和 OEM 验证仍单独执行。

alpha10 增加 Worker 设备回归：合成离线／超时异常、503 与 429，检查原成功快照不变、每轮最多 3 次、Retry-After 阻止额外请求和新尝试恢复。此回归验证传输故障处理与 Worker 返回值，不代替设备实际断网、Doze 或 OEM 后台限制。

alpha11 的 Android 17 CI 使用 `scripts/native_device_tests.py` 安装两 APK 并执行完整 runner，规避已观察到的 ddmlib 安装失败。原始报告保存在 `screenshots/native-instrumentation.txt`；至少 18 项实际通过、开始／结束状态配对及成功完成码是必要条件，Shell 退出 0 不代表测试通过。其他 API 继续使用 Gradle connected 流程。大字体失败时保存实际 View 几何及截图，方便定位旧系统占位布局问题。

alpha12 新增 `scripts/system_time_smoke.sh`：仅在自建测试模拟器执行真实改时探针，检查时区／夏令时／跨天及回拨刷新，finally 恢复时间与自动设置。两个系统时间广播属于 [Android 隐式广播例外](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions)，接收器只重绘缓存；不使用每分钟广播唤醒后台。

`scripts/network_smoke.sh` 显式关闭自建模拟器的 Wi-Fi 与移动数据，要求 OS 从有效互联网变为无网络，再恢复原连接。检查真实 WorkManager 约束与调度：离线不执行请求、成功缓存保留，重连后只请求一次。用量响应由测试 runner 拦截，不把合成凭据发到外部。该探针普通套件默认跳过，CI 在系统时间探针后单独运行。仅“有 activeNetwork”不算恢复，必须满足 NET_CAPABILITY_VALIDATED；本地受代理限制的宿主须先配置其测试网络并在结束后恢复。

TalkBack 专项仅在装有 Google TalkBack 的测试模拟器显式执行：`adb -e shell am instrument -w -e class com.tsonglew.quotapal.TalkBackDeviceTest -e talkbackProbe true com.tsonglew.quotapal.test/com.tsonglew.quotapal.QuotaTestRunner`。会临时启用真实 TalkBack 和 200% 字体，检查标准／紧凑组件的可访问树、焦点、双击和共享请求，并保存 talkback-*-focus／updated 截图；结束恢复原设置。普通套件按条件跳过，不以无 TalkBack 服务的模拟结果代替实际验证。
