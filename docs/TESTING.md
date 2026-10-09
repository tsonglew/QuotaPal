# QuotaPal 验证记录与自用步骤

自动化结果以 [Android CI](https://github.com/tsonglew/QuotaPal/actions/workflows/android.yml) 为准；阶段进度见 [PROGRESS.md](PROGRESS.md)，完整验收见 [DEVELOPMENT_CHECKLIST.md](DEVELOPMENT_CHECKLIST.md)。

## 自动化验证

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
```

- JVM：解析单／多窗口、缺失／非法百分比、重置时间、账号隔离、HTTP 错误、退避、并发请求、续期与退出时的旧响应。
- Android 页面：进入示例、切换标签、深色主题、退出清理，导出浅深色截图。
- Android 存储：Keystore 加密往返、密文篡改拒绝、凭据删除和备份关闭；组件配置互不覆盖。
- Android 组件：使用 debug 专用原生 AppWidgetHost，渲染真实 Glance RemoteViews，检查 140×150、280×150、140×230 dp，退出后切换到未连接状态。

组件测试临时通过系统 shell 授予绑定权限，结束时撤销。测试宿主仅包含在 debug 变体中，未导出，不进入 release。实现依据 [Android widget host 文档](https://developer.android.com/develop/ui/views/appwidgets/host) 和 [AOSP appwidget shell 命令](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/cmds/appwidget/src/com/android/commands/appwidget/AppWidget.java)。宿主测试不能替代真实 Launcher、系统选择器和 OEM 验证。

CI 产物 `android-build` 含 debug APK、单元测试与 lint 报告；`android-device-tests` 含设备测试报告和截图，保留 14 天。不上传凭据或真实账号数据。

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
| 经系统选择器添加组件 | 配置可取消、保存成功后出现在桌面 | 待真机 |
| 放置 3 个组件并缩放 | 各自主题可不同，共享同一更新时间与额度快照 | 待真机 |
| 断网后手动刷新 | 保留上次数据与成功时间，显示失败状态 | 待真机 |
| 恢复网络再刷新 | 获取新快照，App 与组件同步更新 | 待真机 |
| 退出并清除数据 | App 和全部组件进入未连接，后台同步取消 | 待真机 |
| 重新登录、进程回收、重启 | 仅恢复当前账号缓存，联网后正常同步 | 待真机 |
| 令牌实际到期后继续使用 | 续期成功，且无重复续期／账号混用 | 待真机 |

设备记录只包含机型、Android 版本、Launcher、测试时刻、结果和脱敏现象；不记录登录码、token、账号邮箱或完整服务响应。

## 长时间观察

48 小时后台观察和连续 7 天自用必须实际等待完成，不能用模拟器通过替代。记录实际刷新间隔、请求次数、失败与恢复、耗电和崩溃；Doze 与系统省电下允许刷新延迟，更新时间必须真实。

强制停止后，以重新打开 App 能恢复为验收条件。Pixel/AOSP、Samsung、小米 Launcher 以及最低 Android 版本仍需分别执行。公开发布前继续完成签名、升级迁移、渠道与隐私政策验收。
