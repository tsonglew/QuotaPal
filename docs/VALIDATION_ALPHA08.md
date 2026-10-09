# alpha08 刷新状态与平台验证

2026-10-10，versionCode 8／0.1.0-alpha08。

- 已初始化的共享快照读取不再等待网络请求占用的互斥锁；手动刷新请求仍在执行时先唤醒 Glance，结束后再次更新。保留 8 秒超时续办与服务端限流约束。
- 本地 47 项 JVM 测试、lint、App／测试 APK 构建通过。新增慢请求期间快照读取与进度更新顺序回归。
- Android 35 首轮 13 项设备测试中 12 项通过；一项组件布局测试捕获到 RemoteViews 替换期间的空 layout。改为在同一 UI 回调确认布局并检查、截图后，3 项 WidgetRenderTest 全部通过（73.853 秒）。未声称完整 13 项单轮全部通过。
- 标准 AOSP 35 实际 Launcher：标准组件确认取消及再次添加、2×1 添加确认、两种桌面组件显示、点击标准组件打开 App 均通过；系统选择器显示两个入口，拖入 2×1 进入独立配置页并验证取消。该 Launcher 标准组件显示为 2×3；不据此认定其他 Launcher 或小米验收通过。
- actionlint、ShellCheck 与 11 项交付脚本测试通过。新增 API 37.0/google_apis 和 37.2/google_apis_ps16k、4 GB RAM；签名及演练明确进行 16 KB zipalign 并检查。新增矩阵尚待 CI，原 APK 未发现对齐失败。
- debug APK SHA-256：`108cc019b6bbabf98ef65840852fdf1235db68347cf4e3b798fdc1bd63491b43`。

真实小米 Android 17 手动刷新、清理后台后的更新、长期观察、正式签名发布及实际 GitHub deployment 仍待验证。GitHub PR 写入权限问题尚未解决。

Android 17 本地镜像首次启动完成，系统报告 Android 17／API 37／4 KB 页面，App 与测试 APK 安装成功。首轮旧 Espresso 在页面测试抛出 `InputManager.getInstance` 不存在；存储两项、唯一周期任务与刷新回调通过。测试依赖更新到 AndroidX Test runner 1.7.0／JUnit 扩展 1.3.0／Espresso 3.7.0，升级后页面测试已越过系统方法异常；中断前一轮留下的示例状态又暴露页面测试依赖执行顺序，已增加每项开始前清理示例／连接状态。升级后的完整 13 项运行有 11 项通过、2 项因残留示例状态失败；加入独立清理后针对性重跑 AppFlowTest，3 项全部通过（19.396 秒）。Android 17 共 13 个独立测试已有通过覆盖，未声称单轮完整 13 项全部成功。该修复依据 [官方发布说明](https://developer.android.com/jetpack/androidx/releases/test)。

## 后续生命周期回归

Android 17 上新增真实 AppWidgetHost 回归通过（9.883 秒）：280×150 → 140×230 → 140×150，检查实际 RemoteViews 从两窗口到单窗口布局；关闭宿主删除实例后，等待平台删除广播清除已用／深色配置；重新分配实例得到不同 ID 和默认剩余／系统主题。debug 与测试 APK 重建、lint 通过。该回归为第 14 个设备测试，独立执行通过，未据此声称完整 14 项单轮通过。

本地加入 debug 宿主缩放方法后的 APK SHA-256 为 `fd5e9c30529a4523bfeca6cf458370cc9ff97df779bd51de04ab52c80f2835cf`；上文哈希对应最初 3f0d798 构建。生产源码未变更。
