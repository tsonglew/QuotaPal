# alpha05 验证记录

2026-10-10，`0.1.0-alpha05`／versionCode 5。

新增设置页分品牌后台更新引导、自动识别与手动选择、应用信息和电池优化入口、返回时更新系统豁免状态、强制停止恢复说明。来源与限制见 [后台更新引导](BACKGROUND_GUIDE.md)。

- 本地 JDK 17／Android SDK 36：40 项 JVM 测试（0 失败）、lint、debug APK 与测试 APK 构建通过。
- Android 35 标准 AOSP 模拟器直接 instrumentation：11 项全部通过，耗时 95.204 秒。新增测试覆盖切换小米／Samsung 引导、展开／收起、长内容滚动和设置入口可见性；原有页面、存储、唯一周期任务与组件测试通过。
- 检查小米引导原生截图，品牌选择、文字换行及底部导航显示正常。
- 11 项交付脚本测试通过，`git diff --check` 通过。
- debug APK SHA-256：`7c88be11508fc8daf5c588741afb9b6adeae3ae8f45dcdfb7191c94435620bfa`。

以上不包含厂商设置 Activity 真机跳转、自启动或后台锁状态读取、各 OEM 清理后台后的恢复验证。未宣称修复了小米系统后台限制；H04、E03／E07／E08／E09 保持待验收。
