# QuotaPal

面向 Android 的 Codex 额度监控 App，参考 Nowdex 的额度速览与桌面小组件体验。首版支持单 Codex 账号，先自用验证，再公开发布。

当前状态：首版实现与自动验证已完成，使用实验性 Codex 直连接入。27 项单元测试、lint、4 项 Android 35 设备测试通过。真实额度只读探测已成功，Android 真实登录、续期和连续自用仍需验证。

- [开发计划](docs/DEVELOPMENT_PLAN.md)
- [开发与验收 checklist](docs/DEVELOPMENT_CHECKLIST.md)
- [GitHub milestones](https://github.com/tsonglew/QuotaPal/milestones)
- [实时开发进度](docs/PROGRESS.md)
- [Codex 接入证据与边界](docs/CODEX_INTEGRATION.md)
- [界面规格](docs/UI_SPEC.md)
- [测试与自用验收](docs/TESTING.md)
- [开发 PR](https://github.com/tsonglew/QuotaPal/pull/7)
- [本次验证与 APK 校验和](docs/VALIDATION.md)
- [原生页面和组件截图](docs/SCREENSHOTS.md)

Android 10+，Kotlin 2.1.21、Compose、Glance、Room、DataStore 和 WorkManager。固定构建链为 AGP 8.10.1、Gradle 8.11.1、JDK 17，compileSdk／targetSdk 36。

已有 Android SDK 与 JDK 17 时执行 `./gradlew testDebugUnitTest lintDebug assembleDebug`。APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。有设备或模拟器时执行 `./gradlew connectedDebugAndroidTest`。GitHub Actions 会保存 APK、测试报告及原生截图。

设备登录由用户在 OpenAI 页面完成，QuotaPal 不收集密码。示例模式须主动选择并明确标注，不执行额度请求。凭据通过 Android Keystore 加密保存在本机；退出清理本机连接，远程授权不会因此自动撤销。
