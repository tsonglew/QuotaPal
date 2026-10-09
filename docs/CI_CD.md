# CI/CD 与 GitHub Deployments

2026-10-10 用户新增需求，对应 M6、checklist G01–G08。

参考 [magpie 工作流](https://github.com/yetone/magpie/tree/599d784f597342e6601bdc1d49b495fdf84e546f/.github/workflows)：预览使用独立环境，构建与发布分离，发布按版本 tag 触发。QuotaPal 将这些边界用于 Android APK，不引入 macOS 公证或模型 API Key。

## 流程

| 入口 | 检查／产物 | 部署 |
| --- | --- | --- |
| PR、master／codex 分支 push、手动 CI | 交付脚本测试、47 项 JVM 测试、lint、debug APK；API 29／31／35／36／37.0／37.2 设备矩阵（新增 Android 17 两项待首次 CI） | PR 或 master 全部通过后登记预览 deployment；codex 分支 push 仅验证 |
| `v<versionName>` tag，或在该 tag 上手动运行 Release | 验证 tag 格式、版本及主分支祖先关系；复用完整 CI；额外构建和 lint minified release，并在四版本模拟器检查真实 release 页面 | 固定密钥签名、校验证书、发布 GitHub Release；记录 `android-release` 环境 |

统一状态为 `Android CI gate`，只有交付工具检查、build 与全部设备矩阵成功才会通过。交付工具检查固定 actionlint 1.7.12、运行 ShellCheck 与 Python 测试。主分支 CI 不因后续 push 取消正在运行的验证；PR 新提交可取消旧验证。发布同 tag 串行执行。

## 预览 deployment

`deploy-preview.yml` 在 Android CI 的 `workflow_run` 完成后运行。仅处理成功的本仓库运行，不处理失败、取消、fork 来源、已关闭 PR 或已被新提交替代的运行。PR 关闭后，独立清理工作流将该预览标为 inactive；清理 job 不 checkout PR 代码。它直接使用部署 API 指定被测试的源 SHA，避免 `workflow_run` 自动使用默认分支 SHA 而误记来源。

环境为 `android-preview/pr-<number>` 或 `android-preview/master`。记录 `in_progress`，下载并校验 APK 后记录 `success`；下载或校验失败记为 `failure`。每条记录的 environment URL 指向对应运行的 `android-apk` 下载入口，log URL 指向部署运行。预览是可下载 APK，无需额外开通 Pages。

`android-apk` 包含 `quotapal.apk`、`SHA256SUMS`、`metadata.json`；元数据包括版本号、源 SHA、实际构建 SHA 与 APK 哈希。PR 源 SHA 是分支 head；实际构建 SHA 可以是 GitHub 合并测试提交。预览保留 14 天，过期后重新运行 CI 获取新产物；旧部署记录不意味着下载永久可用。

预览是 debug 签名，供评审测试。GitHub 下载 artifact 可能需要登录。不同 runner 的 debug 签名不保证一致，正式升级使用下述固定发布签名。

有写权限的部署工作流不 checkout 或执行 PR 代码；只读取已完成运行的产物，使用工作流内固定的校验程序。构建任务使用只读 token，不接收发布凭据。

## 发布环境

仓库 Settings → Environments 创建 `android-release`，按组织策略设置允许的 `v*` tags 和发布保护规则。在该环境中配置四个 secrets：

| 名称 | 内容 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | 现有固定 release keystore 的 Base64 内容 |
| `ANDROID_STORE_PASSWORD` | keystore 密码 |
| `ANDROID_KEY_ALIAS` | 签名 key alias |
| `ANDROID_KEY_PASSWORD` | key 密码 |

设置环境变量 `ANDROID_SIGNING_CERT_SHA256` 为预期证书的 SHA-256 指纹（可带冒号）。指纹不匹配或任一凭据缺失时发布失败。密钥由维护者离线保管和备份，不提交到 Git、不上传到 artifact，不在日志输出密码。不要为每个版本生成新密钥。

发布步骤只在所有测试通过后获取环境 secrets。编译任务不使用签名凭据，签名任务下载 unsigned release APK，以 `zipalign`、`apksigner` 完成对齐、签名和验证。临时 keystore 退出时删除。

先更新 `versionCode`（递增）和 `versionName`，经 PR 合并到 master，再创建 `v<versionName>` tag。示例：`versionName = "0.1.0-alpha04"` 对应 `v0.1.0-alpha04`。Release 的手动运行也必须选择已有版本 tag，不能在分支上运行。

最终资产为 `QuotaPal-<version>.apk`、`SHA256SUMS`、`provenance.json`。带连字符的版本标为 prerelease，稳定版本为普通 Release。发布 URL 在 GitHub Deployments 的 `android-release` 中可见。稳定 tag 应在 M4–M5 的真机、长期使用及公开发布验收后创建。

CI debug 签名与 release 签名不同，首次切换需要卸载 debug 包再安装并重新连接；后续 release 版本沿用同一密钥覆盖升级。此配置尚不代替实机升级测试。

## 失败与重跑

- CI 任一步失败或取消：不登记成功预览，不进入签名发布。
- 预览下载／哈希失败：deployment 标为失败，保留对应运行日志。修复后重跑完整 CI。
- 签名缺失／指纹错误：修复环境配置后重跑失败 job，不换 key 绕过指纹校验。
- 发布先建立 draft，全部资产上传后才公开。已有 draft 可重跑上传；已公开版本拒绝覆盖，修复需增加版本并创建新 tag。
- 需要回退时安装已验证的旧 Release；Android 降级限制与数据兼容需单独确认。不要移动已公开版本的 tag。

## 当前验证状态

APK 产物工具 6 项测试通过，覆盖来源区分、篡改 APK、错误提交、符号链接、错误应用及校验清单拒绝。[GitHub CI 运行 37978463336](https://github.com/tsonglew/QuotaPal/actions/runs/37978463336) 的构建、lint、单元测试、四版本设备矩阵及 gate 全部通过。环境配置、正式发布签名、升级及 deployment 下载仍待验证；不能仅凭 YAML 完成就勾选 G08。

发布控制另有 5 项命令替身测试：上传完成后才公开 draft、证书不匹配、缺失凭据、错误版本均不调用 GitHub，已公开版本不覆盖。替身测试不证明实际证书签名或 GitHub API 发布成功。

GitHub 环境与部署行为参见 [官方环境说明](https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/manage-environments) 和 [部署状态 API](https://docs.github.com/en/rest/deployments/statuses)。

版本发布时，设备矩阵在 debug 回归通过后，下载同次构建的 unsigned release，以一次性测试密钥签名，在隔离模拟器检查启动、示例、组件页、设置、隐私说明和退出。脚本拒绝在真机上执行；通过后才进入正式固定签名 job。日志随设备测试产物保存。此测试不替代 OEM、真实账号或正式签名覆盖升级验收。

手动运行 Android CI 时可勾选 `build-release`，在所选分支验证 minified release 和四版本页面流程，不读取正式签名 secrets，也不创建 GitHub Release。用于在合并／打 tag 前排查发布构建问题。

Android 17 使用官方 SDK 的 `37.0/google_apis` 与 `37.2/google_apis_ps16k` 镜像及 4 GB RAM；后者覆盖 16 KB 页面配置。签名脚本显式使用 `zipalign -P 16` 并验证对齐。此处是配置说明，实际结果见对应运行记录。

### Android 17 启动就绪

运行 37986219241 的 API 37.0／37.2 在应用测试开始前分别因 input 服务缺失／Broken pipe 失败；四个既有版本设备测试通过。Android 17 改由 `scripts/android17_device_ci.sh` 启动相同官方镜像，`wait_for_android.py` 要求启动标记、核心服务、包查询、settings 与无操作按键命令连续三轮成功，再运行原有完整测试及可选 minified release 检查。启动／logcat 日志随设备证据上传；失败继续使统一 gate 失败，不跳过检查。新启动流程尚待云端验证。

所有普通工作流 job 检出事件的不可变 `github.sha`，避免设备 job 启动较晚时读到分支新提交；可复用工作流仍支持显式 ref。

### Android 17 模拟器运行环境

API 37.0／37.2 云端可重复出现 SurfaceFlinger 的 ReadColorBufferDma 断言，完整 gate 保持失败。曾固定官方 37.1.11 Stable 并校验下载 SHA-256；本地 ARM64 全套通过，但补 libpulse0 后的实际云端运行仍复现相同断言，因此撤回无效降级，继续使用 SDK 稳定模拟器。系统镜像与完整测试范围保持原样。图形路径的新配置须实际验证后才能认定有效，版本依据与失败对照见 [alpha13 验证](VALIDATION_ALPHA13.md) 和 [alpha14 验证](VALIDATION_ALPHA14.md)。

Android 17 job 显式安装 Linux 运行依赖 libpulse0；初次固定版本对照在缺少 libpulse.so.0 时提前失败，没有执行设备测试，不计作通过。

Android 17 新候选启用官方 Vulkan composition（`-feature VulkanNativeSwapchain -feature GuestUsesAngle`），使用与本地完整 18 项测试通过时相同的参数；verbose 日志保留实际特性值。仅本地 ARM64 已通过，云端 x86_64／16KB 仍待独立验证，不视为 gate 修复完成。
