# QuotaPal

面向 Android 的 Codex 额度监控 App，参考 Nowdex 的额度速览与桌面小组件体验。QuotaPal 是独立第三方工具，与 OpenAI 无隶属关系。首版支持单 Codex 账号，先自用验证，再公开发布。

![QuotaPal 浅色与深色界面，以及 2×1、标准和宽版桌面小组件预览](docs/assets/quotapal-preview.png)

基于原生界面截图合成的展示效果图，额度均为示例数据。[查看原生截图](docs/SCREENSHOTS.md)。

当前开发分支版本：`0.1.0-alpha15`（开发中，尚未合并）。alpha15 新增本机诊断预览／分享／清除、7 天按小时请求与刷新统计，以及后台任务状态；本地 71 项 JVM、debug lint 与应用／测试 APK 构建通过，六版本设备与固定签名执行待验收，见 [alpha15 记录](docs/VALIDATION_ALPHA15.md)。公开渠道确定为 GitHub Releases APK。已实现分品牌后台引导、小组件即时手动刷新、离线隐私说明、后台有限重试和系统时间变化后的缓存重绘。alpha14 补充刷新存储异常处理与写盘失败时的进程内限流保护，56 项单元测试、lint、构建通过。[六版本 CI](https://github.com/tsonglew/QuotaPal/actions/runs/38007906590) 的 API 29／31／35／36／37.0／37.2 及统一 gate 全部通过，Android 17 普通／16KB 镜像各 18 项普通设备测试及四个独立探针通过，实际页面大小 4096／16384。已有小米 Android 17 真机连接成功反馈，真实续期、清理后更新及长期自用仍待验收。验证范围见 [alpha14 记录](docs/VALIDATION_ALPHA14.md)。

- [开发计划](docs/DEVELOPMENT_PLAN.md)
- [开发与验收 checklist](docs/DEVELOPMENT_CHECKLIST.md)
- [GitHub milestones](https://github.com/tsonglew/QuotaPal/milestones)
- [实时开发进度](docs/PROGRESS.md)
- [CI/CD、预览 Deployments 与签名发布](docs/CI_CD.md)
- [隐私说明与数据流](docs/PRIVACY.md)
- [后台更新引导](docs/BACKGROUND_GUIDE.md)
- [小组件刷新修复验证](docs/VALIDATION_ALPHA06.md)
- [Codex 接入证据与边界](docs/CODEX_INTEGRATION.md)
- [接口兼容与故障维护策略](docs/COMPATIBILITY_POLICY.md)
- [界面规格](docs/UI_SPEC.md)
- [测试与自用验收](docs/TESTING.md)
- [历史修复 PR](https://github.com/tsonglew/QuotaPal/pull/8)
- [alpha03 验证记录与 2×1 安装包](docs/VALIDATION_ALPHA03.md)
- [alpha02 验证记录](docs/VALIDATION_ALPHA02.md)
- [首版验证与 APK 校验和](docs/VALIDATION.md)
- [原生页面和组件截图](docs/SCREENSHOTS.md)
- [产品简介、发布素材与使用帮助](docs/RELEASE_MATERIALS.md)

Android 10+，Kotlin 2.1.21、Compose、Glance、Room、DataStore 和 WorkManager。固定构建链为 AGP 8.10.1、Gradle 8.11.1、JDK 17，compileSdk／targetSdk 36。

已有 Android SDK 与 JDK 17 时执行 `./gradlew testDebugUnitTest lintDebug assembleDebug`。APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。有设备或模拟器时执行 `./gradlew connectedDebugAndroidTest`。GitHub Actions 会保存 APK、测试报告及原生截图。

设备登录由用户在 OpenAI 页面完成，QuotaPal 不收集密码。示例模式须主动选择并明确标注，不执行额度请求。凭据通过 Android Keystore 加密保存在本机；退出清理本机连接，远程授权不会因此自动撤销。

当前账号覆盖、刷新限制和未完成验收见 [支持范围与已知限制](docs/SUPPORT.md)。
