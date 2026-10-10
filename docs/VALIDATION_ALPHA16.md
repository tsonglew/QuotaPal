# alpha16 正式签名覆盖升级验证

日期：2026-10-10。候选版本 0.1.0-alpha16／16，基于合并后的 master f5edf96。版本递增用于实际验证 alpha15→alpha16，不能用同版本重装代替升级。

## 基线与边界

固定签名基线来自 master a983dfd 的运行 38031990526，产物 android-signed-apk／11662492337。已实际下载并独立验证 APK 哈希、签名、版本及 16 KB 对齐，完整证据见 [alpha15](VALIDATION_ALPHA15.md)。候选必须使用同一 android-release 密钥，证书指纹来自公开环境变量；不读取本机 keystore 或密码。

目标是在任务专用 Android 模拟器中安装真实 minified 正式签名 alpha15，通过应用自身认证与额度入口连接合成账号，写入真实 Keystore／Room／DataStore。之后覆盖安装正式签名 alpha16，核对版本增加、UID、缓存、偏好及凭据恢复，再通过受控 401 续期和新快照验证加密凭据可继续使用。不能以示例模式、debug 包、同版本重装或仅比较证书代替这个流程。

尚未完成上述实际执行，F02 保持未勾选。真实账号权限／续期（A）、OEM、48 小时与七天观察仍分别验收；合成服务不证明真实服务或账号支持。

## 合成 HTTPS 服务

`scripts/upgrade_lab_server.py` 只监听本机 127.0.0.1，只对 auth.openai.com 和 chatgpt.com 的固定夹具路径响应，不提供任何外部转发。创建一次性测试 CA／服务器证书，与 APK 签名 keystore 无关；不记录请求正文、凭据或任意 URL。

专用模拟器需要显式信任测试 CA，并将真实服务域名的直接解析阻断到设备环回地址，再配置本机代理。这样应用仍使用生产 HTTPS 请求和正常证书校验；代理失效时直连也应失败。测试 CA、系统 hosts、代理只改变该任务创建的模拟器，不修改生产 APK 的网络信任策略或用户手机。必须先核对 AVD 名称及 ro.kernel.qemu=1，再改变这些状态。结束后清除合成应用数据并停止该模拟器。

脚本回归 25 项通过，包含 HTTPS 证书校验拒绝未信任 CA、未知 CONNECT 主机被拒绝，以及实际 HTTP／TLS 上的首次交换、额度读取、401、refresh token 轮换与新额度读取。第一次沙箱执行因禁止监听本机端口失败，使用允许本机网络的执行环境后完整通过；不把环境权限错误算作逻辑通过。TLS socket 资源清理修正后以 ResourceWarning=error 再次通过。

## 运行证据

初始候选 31b7ebc 的 [CI 38035733101](https://github.com/tsonglew/QuotaPal/actions/runs/38035733101) 完整六组及 gate 成功。并行运行的 master f5edf96 在 API 37.0 被 Pixel Launcher 无响应弹窗遮挡，真实失败截图及系统日志见 [测试指南](TESTING.md#模拟器桌面弹窗隔离)。候选随后增加该已确认桌面变体的严格处理，必须重新运行 CI，不能沿用初始提交的成功结果。

本机更新后的 debug／androidTest 编译成功，专用 API 31 模拟器上 WidgetRefreshActionDeviceTest 两项实际通过（4.838 秒），保留三组件进度、唯一请求及最终额度断言；本轮没有出现桌面弹窗，不能据此声称已实际执行弹窗关闭分支。本机独立 release 构建达到 600 秒预算后退出 124，不算通过；候选 minified 构建仍需 master 云端结果。

追加受控桌面故障对照：只在已核对名称的专用 API 31 AVD 暂停系统 Pixel Launcher，再注入输入造成实际 ANR，随后 finally 恢复该进程。输入命令达到 20 秒限时，并非普通成功返回；后续 UI XML 和系统日志实际确认出现完整匹配的弹窗。运行原三组件真实点击测试，处理分支日志明确记录关闭 com.google.android.apps.nexuslauncher，保留[关闭前截图](screenshots/emulator-pixel-launcher-recovery-api31.png)，原断言通过（5.908 秒）。这证明已执行恢复分支，不证明桌面自身 ANR 被修复。

433a66c 的 [CI 38036477159](https://github.com/tsonglew/QuotaPal/actions/runs/38036477159) 最终五组成功、API 35 失败、gate 正确失败。失败来自诊断报告预览的 Dialog 创建：[完整栈](diagnostics/alpha16-compose-test-thread.txt) 包含 DefaultDispatcher、TestMonotonicFrameClock、ApplyingContinuationInterceptor，尚未在生产运行器复现。[AndroidX 测试源码](https://android.googlesource.com/platform/frameworks/support/+/202b4bda8adfeb303ac30e359e8436a3752fe2b0/compose/ui/ui-test/src/commonMain/kotlin/androidx/compose/ui/test/ApplyingContinuationInterceptor.kt) 说明此拦截器在恢复 continuation 后发送快照通知；据堆栈推断，测试的不受线程约束的恢复路径触发了后台重组。候选将预览／清除协程显式约束到 Android 主调度器，文件读取仍在 IO，增加五轮实际预览／清除及额度请求数不变断言，需重新验证。

正式签名候选、模拟器升级及恢复的结果另行填写。当前完成夹具、环境准备及 debug 定向回归，不声明正式升级已通过。

主线程约束修订后，debug／androidTest 重新构建成功，专用 API 31 AppFlowTest 四项全部通过（18.166 秒），包含五轮预览／清除、共十次报告弹窗，以及额度请求数不变断言。实际签名升级前会卸载该 debug 测试数据，重新安装正式 alpha15 并经其自身连接入口建立基线；不会将 debug 数据或测试签名当作升级基线。
