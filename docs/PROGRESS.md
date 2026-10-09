# QuotaPal 开发进度

更新日期：2026-10-10（Asia/Shanghai）。用户已授权持续开发、按里程碑提交 PR 并合并；当前开发分支为 `codex/stability-matrix`。

| 阶段 | 当前状态 | 已有证据 | 下一步 |
| --- | --- | --- | --- |
| M0 接入验证 | 进行中 | 官方协议已核对；真实额度探测 HTTP 200；认证、解析、并发、续期与清理单元测试通过；用户反馈 Android 真机连接成功 | 实际权限、续期、撤销及账号覆盖仍待验收 |
| M1 规格冻结 | 已完成 | 页面与状态规格冻结；浅深色原生截图已检查；issue 和 milestone 已关闭 | 真机验收中仅修复问题，新增范围另行排期 |
| M2 账号与额度 | 实现及自动验证完成，已有真实连接反馈 | 40 项单元测试、lint、页面流程、Keystore 和独立配置测试通过；C01–C08 完成；用户反馈真机连接成功 | 补齐刷新、退出、重连及 M0 续期与账号范围的真机证据 |
| M3 小组件与后台 | 实现及基础设备验证完成 | 标准布局及独立 2×1 入口、无周期及未知额度状态、文字可见性、共享更新、混合实例配置与退出清理通过 | 系统选择器、点击刷新、进程恢复、重启与 OEM 验收 |
| M4 稳定性验证 | 自动验证通过，待真机观察 | API 29／31／35／36 设备矩阵通过；Android 35 本地 10 项测试通过；标准与 2×1 组件均有原生截图 | 平台与 Launcher 矩阵、48 小时后台及 7 天自用 |
| M5 公开发布准备 | 资料准备中，公开发布待稳定性验收 | 离线隐私说明与数据流核对完成；设置和连接入口设备验证通过 | 固定签名、升级、渠道与发布资料验收 |
| M6 CI/CD 与部署 | 实现中（用户新增） | 已核对 magpie 固定提交的工作流；11 项交付脚本测试通过；四版本 CI 全部通过；release 构建及临时密钥签名验证通过 | 合并配置，验证预览 deployment；配置固定签名后验证 Release |

进度以提交、构建、测试和实际设备记录为证据，不将实现完成等同于真机验收完成。GitHub 任务会与本页及 checklist 同步更新。

## 当前环境

- 已创建原生 Android 工程、自动测试及 GitHub CI。
- 本机使用 JDK 17；2026-10-10 在忽略目录 `.tools/android-sdk` 安装 Android 36 SDK、Build Tools 35.0.0 和 platform-tools，已运行 Android 35 标准 AOSP 模拟器。
- 本地构建已启动；实机和长时间观察项目保留待验收。
- 自用设备：小米 17 Ultra，Android 17（版本以用户更正后的报告为准）。2026-10-10 用户确认当前 APK 已成功连接真实 Codex 账号；未收集账号、凭据或原始额度数据。
- 已下载自用测试 APK；[验证记录](VALIDATION.md) 保存构建版本、校验和和证据，[原生截图](SCREENSHOTS.md) 保存实际渲染结果。

## 进展记录

- 2026-10-10：收到小米 17 Ultra／Android 17 真机连接成功反馈，作为首次连接的用户报告保存；未将该反馈扩展为续期、账号／工作区映射或连续自用验收。模拟器安装发现本机仅约 1.5 GB 可用空间，停止安装，优先完成 JVM、lint 与 APK 构建。现有自动化矩阵最高 API 36，Android 17 另作用户真机验证平台。
- 2026-10-10：本地构建使用当前网络代理重试依赖下载后通过：40 项单元测试（22 解析／时间、10 仓库、8 网络认证）、lint、App 与设备测试 APK；lint 无错误，17 项非阻断警告。新增跨天／时区、夏令时测试与真实 WorkManager 数据库的唯一周期任务设备测试。版本推进 `0.1.0-alpha04`，准备执行设备回归；设备测试尚未据此勾选。

- 2026-10-10：继续 M3–M4 验证。修复系统时间回拨后未来的最近尝试／成功快照时间会持续阻止刷新的问题；增加回拨恢复与服务端 Retry-After 不被绕过两项回归。CI 扩展 API 29、31、35、36，分别保存设备证据。构建与矩阵尚待验证，不提前勾选 E05／E06；真实授权、OEM 和长期观察仍待验收。
- 2026-10-10：修复与 checklist 已以 `a9541eb` 推送到 `codex/stability-matrix`。GitHub 集成创建 PR 返回 403（Resource not accessible by integration）；CLI 未登录，浏览器 GitHub 访问被权限策略拒绝，故尚无新 PR 或合并。进度同步脚本改为读取文档日期并引用主分支，dry-run 成功；未执行 GitHub 写入。首次本地构建因缺 SDK 失败，补齐 SDK 后重新运行，结果待记录。

- 2026-10-09：实现独立 2×1 桌面组件入口与 App 添加按钮，保留主要额度／状态、刷新入口和完整更新时间，适配单行及较矮横屏布局。两种组件共用更新与后台调度，实例配置独立。版本升级为 `0.1.0-alpha03`。[验证 37953081016](https://github.com/tsonglew/QuotaPal/actions/runs/37953081016) 全部通过：36 项单元测试、lint、构建、9 项 Android 35 测试。新增组件检查 4 种尺寸、已用／剩余、5 种额度状态、退出和混合实例；修复测试宿主同时启动两个 Activity 时的生命周期问题。详情见 [alpha03 验证记录](VALIDATION_ALPHA03.md)。

- 2026-10-09：根据自用反馈修复无短周期／无周期额度的空白状态，加入“未提供短周期额度”“当前可用”“当前使用受限”和缺失用量说明；主界面、预览及原生组件共用状态判断。版本升级为 `0.1.0-alpha02`。[验证 37946436238](https://github.com/tsonglew/QuotaPal/actions/runs/37946436238) 全部通过：36 项单元测试、lint、App／测试 APK 构建、8 项 Android 35 测试。设备回归修复了紧凑组件新增提示后底部时间裁切的问题，三种尺寸的 5 种状态均验证通过。变更见 [PR #8](https://github.com/tsonglew/QuotaPal/pull/8)，完整证据见 [alpha02 验证记录](VALIDATION_ALPHA02.md)。

- 2026-10-09：启动 M0、M1；核对官方设备授权流程与额度读取路径；检查 Nowdex 的蓝色分段进度条、大号百分比、浅深色圆角卡片和更新时间布局。

- 2026-10-09：真实额度探测返回 HTTP 200，未保存身份、凭据或实际额度值。
- 2026-10-09：建立 [M0](https://github.com/tsonglew/QuotaPal/issues/1)、[M1](https://github.com/tsonglew/QuotaPal/issues/2)、[M2](https://github.com/tsonglew/QuotaPal/issues/3)、[M3](https://github.com/tsonglew/QuotaPal/issues/4)、[M4](https://github.com/tsonglew/QuotaPal/issues/5)、[M5](https://github.com/tsonglew/QuotaPal/issues/6) 跟踪任务；完成协议适配、原生页面、组件、后台同步第一轮实现，进入构建验证。
- 2026-10-09：创建 [开发 PR #7](https://github.com/tsonglew/QuotaPal/pull/7)。[首轮代码构建](https://github.com/tsonglew/QuotaPal/actions/runs/37931567792) 编译、单元测试和 APK 生成通过；lint 拦截 Glance 内部 API 使用，已改公开接口。同时改为订阅共享状态，新增组件原生渲染和 Android Keystore 验证。
- 2026-10-09：[构建验证](https://github.com/tsonglew/QuotaPal/actions/runs/37932751546) 通过编译、单元测试、lint 和 App／测试 APK 生成，进入 Android 35 模拟器阶段。C01–C05 基于实现检查及自动测试勾选；C08 为明确不支持远程撤销、界面区分本地清理。真实登录和设备验收仍保留未完成。
- 2026-10-09：[设备验证](https://github.com/tsonglew/QuotaPal/actions/runs/37933925112) 中 27 项单元测试、lint 和 3 项 Android 页面／存储测试通过；组件测试发现 Glance 单容器最多 10 个子元素，已用嵌套容器修复分段条与窗口截断。C06 基于原生加密往返、密文篡改拒绝、删除和备份关闭测试勾选。
- 2026-10-09：[后续组件回归](https://github.com/tsonglew/QuotaPal/actions/runs/37934786626) 确认容器截断日志消失；发现测试宿主未预留系统组件内边距，已校正。截图改用 MediaStore 保存，避开 AGP 测试后自动卸载清理；随后运行 [回归 37935801206](https://github.com/tsonglew/QuotaPal/actions/runs/37935801206)。另补齐并发刷新合并结果和设备码开关引导。
- 2026-10-09：[回归 37935801206](https://github.com/tsonglew/QuotaPal/actions/runs/37935801206) 全部通过；视觉检查发现组件底部更新时间被裁切，随后调整间距和最小尺寸，增加完整可见性断言，改用 PixelCopy 捕获真实圆角效果。
- 2026-10-09：[最终实现验证](https://github.com/tsonglew/QuotaPal/actions/runs/37936883382) 全部通过：27 项单元测试、lint、APK 与测试包构建、4 项 Android 35 原生测试。复查 7 张截图，确认三种组件布局的数字、分段条和更新时间完整。M1 任务及 milestone 已完成；其余阶段保留真实授权、平台／Launcher 与长期观察的待验收项。

- 2026-10-10：新增 M6 CI/CD 与 GitHub Deployments 配置，参考 magpie 固定提交。运行 [37978463336](https://github.com/tsonglew/QuotaPal/actions/runs/37978463336) 的交付检查、40 项 JVM 测试、lint、APK、API 29／31／35／36 设备矩阵及统一 CI gate 全部通过。本地标准 AOSP 35 额外通过 10 项设备测试；release lint／R8 构建通过，实际 zipalign／apksigner 临时密钥演练通过。尚未用正式密钥发布或登记实际 deployment；GitHub 写权限仍阻止创建 PR。详见 [alpha04 验证记录](VALIDATION_ALPHA04.md)。

- 2026-10-10：用户反馈清理后台后不再自动更新。新增分品牌后台引导，版本推进 `0.1.0-alpha05`：自动识别与手动切换、应用信息／电池优化入口、返回后重读系统状态、强制停止后重开提示。未将其认定为已修复后台限制；H04 保留小米 Android 17 及其他 OEM 实测。调研见 [后台更新引导](BACKGROUND_GUIDE.md)。40 项 JVM 测试、lint、App／测试 APK 构建及新增页面回归通过；11 项交付工具测试通过。Android 35 全部 11 项设备测试通过，小米引导原生截图已检查，H01–H03 完成，H04 待真机验收。详见 [alpha05 验证记录](VALIDATION_ALPHA05.md)。

- 2026-10-10：用户报告小组件手动刷新不生效。定位到按钮只排入普通 WorkManager 任务，无立即获取；改为有时限的直接强制刷新，超时／临时失败再续办，Android 12+ 续办使用 expedited，新的续办不被旧任务退避挡住。修复中断请求的最近尝试时间恢复，避免续办误用旧缓存当成功；加入回归测试，版本推进 alpha06。45 项 JVM 测试、lint、App／测试 APK 构建通过；设备共 12 项覆盖通过（首轮 9 项通过，截图布局等待修正后 3 项组件回归通过；最后针对缺失额度布局与刷新回调的 2 项再次通过）。I01–I02 完成，I03 保留真机验证；详见 [alpha06 验证记录](VALIDATION_ALPHA06.md)。

- 2026-10-10：[alpha06 CI 37981492470](https://github.com/tsonglew/QuotaPal/actions/runs/37981492470) 完成：45 项 JVM、lint、构建、API 29／31／35／36 设备矩阵、交付脚本检查及统一 gate 全部通过。alpha07 新增离线隐私说明，两处入口共用一份打包原文；设置和连接前的查看／关闭已通过 Android 35 测试，截图已检查，F04 完成。公开发布仍等待 M4–M5 的其余验收。

- 2026-10-10：alpha07 的 release lint、R8 构建通过，实际 manifest 无 debuggable 与 debug 测试宿主，禁止备份和明文网络；生产代码无凭据／HTTP 日志。使用一次性密钥在 Android 35 标准模拟器安装真实 minified release，启动、示例、组件页、深色设置、隐私说明与退出全部通过。F03 完成；加入发布 CI 四版本 minified 页面检查和不发布的手动验证选项。正式签名、实际发布与升级仍待验收。详见 [alpha07 验证记录](VALIDATION_ALPHA07.md)。

- 2026-10-10：[alpha07 CI 37982843666](https://github.com/tsonglew/QuotaPal/actions/runs/37982843666) 的交付检查、构建、API 29／31／35／36 设备测试与 gate 全部通过。alpha08 修复网络请求期间共享快照初始化阻塞组件渲染，并在请求未结束时唤醒 Glance 显示刷新状态；47 项 JVM、lint 与 debug 构建通过。标准 AOSP 35 验证两种 App 添加确认、标准入口取消、桌面显示和点击打开 App；系统选择器拖入已进入配置页并验证取消。新增 API 37.0 与 37.2 CI 配置、16 KB 对齐检查，实际运行结果待记录。

- 2026-10-10：Android 17／API 37 本地首次运行确认系统版本及 4 KB 页面，应用安装成功。旧 Espresso 的 InputManager 反射方法已被平台移除，升级 AndroidX Test 依赖；补充 AppFlow 每项开始前清理示例状态，避免中断后残留影响结果。未将首次失败计为应用兼容性通过。

- 2026-10-10：Android 17／API 37 设备验证覆盖 13 个测试：升级框架后首轮 11 项通过，两项因测试残留示例状态失败；增加独立清理后，3 项 AppFlowTest 全部通过（19.396 秒），包括隐私、分品牌后台引导和页面流程。3 项原生组件渲染、存储、唯一任务和手动刷新回调通过。API 37.2／16 KB 及小米实际手动刷新仍待验证。

- 2026-10-10：新增真实 AppWidgetHost 生命周期回归，在 Android 17 验证横向／纵向／紧凑缩放后的实际窗口布局、删除广播清理独立配置、重新添加的新 ID 与默认配置，1 项通过（9.883 秒），构建及 lint 通过。D03 保留完整 Launcher 流程验收；当前分支 CI 37986219241 构建检查已通过、作业仍在收尾，新增生命周期回归尚未推送，避免取消当前矩阵。

- 2026-10-10：Android 17 单轮完整 15 项设备测试全部通过（123.716 秒），新增合成已连接账号的真实 PendingIntent 刷新、三个混合组件共享一次请求和唯一周期任务，D04／D06 完成。47 项 JVM、lint、debug 构建及 14 项交付脚本测试通过。alpha08 云端四个既有版本通过，Android 17 两组在测试前 input 命令失败；补充服务与命令连续就绪检查、启动日志，普通 job 固定事件 SHA。新启动脚本与 16 KB 云端验证待运行。

- 2026-10-10：Pixel Launcher／Android 17 原生系统选择器完成标准与紧凑添加配置、取消、紧凑从两列扩大为四列、删除与重新添加，独立浅深色并存截图已检查；真实 Launcher 绑定两种 provider。结合 AOSP 35 App 添加入口与宿主自动回归，D01／D03 完成，OEM／真实账号后台仍保留待验收。新 CI 37987702971 的交付与构建通过，六组设备作业运行中。

- 2026-10-10：Pixel Launcher 200% 字体实测发现标准组件底部时间被裁掉、紧凑组件时间和刷新符号省略，记录为 D09 待修复布局问题；已恢复模拟器字体。SIGKILL 后示例恢复仅证明示例模式，不等同于真实缓存或同步恢复，D08 保留。

- 2026-10-10：alpha09 大字体专用布局与定向回归通过，六种尺寸保留完整时间和刷新入口；0／100%、缺失用量、无周期、受限、未连接覆盖通过。启动完整 16 项 Android 17 回归，D09 无障碍及小米实测继续待验收。

- 2026-10-10：alpha09 完整 16 项 Android 17 回归通过（147.606 秒），47 项 JVM／lint／构建通过。Pixel Launcher 原生深浅两组件切换 200% 字体后完整显示额度、时间和刷新按钮，截图检查通过并恢复字体；TalkBack 和小米实际清理后台后的更新继续待验收。

- 2026-10-10：CI 37987702971 的四个既有平台通过，Android 17 两组框架就绪超时后清理卡住，最终取消；补充有界 adb 清理、模拟器提前退出检查及启动日志输出。16 项交付脚本测试通过，云端 Android 17／16 KB 继续待验证。
