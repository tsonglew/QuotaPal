# QuotaPal 开发进度

更新日期：2026-10-10（Asia/Shanghai）。用户已授权持续开发、按里程碑提交 PR 并合并；当前开发分支为 `codex/stability-matrix`。

| 阶段 | 当前状态 | 已有证据 | 下一步 |
| --- | --- | --- | --- |
| M0 接入验证 | 进行中 | 官方协议已核对；真实额度探测 HTTP 200；认证、解析、并发、续期与清理单元测试通过；用户反馈 Android 真机连接成功 | 实际权限、续期、撤销及账号覆盖仍待验收 |
| M1 规格冻结 | 已完成 | 页面与状态规格冻结；浅深色原生截图已检查；issue 和 milestone 已关闭 | 真机验收中仅修复问题，新增范围另行排期 |
| M2 账号与额度 | 实现及自动验证完成，已有真实连接反馈 | 36 项既有单元测试、lint、页面流程、Keystore 和独立配置测试通过；C01–C08 完成；用户反馈真机连接成功 | 补齐刷新、退出、重连及 M0 续期与账号范围的真机证据 |
| M3 小组件与后台 | 实现及基础设备验证完成 | 标准布局及独立 2×1 入口、无周期及未知额度状态、文字可见性、共享更新、混合实例配置与退出清理通过 | 系统选择器、点击刷新、进程恢复、重启与 OEM 验收 |
| M4 稳定性验证 | 自动验证通过，待真机观察 | Android 35 模拟器 9 项测试通过；标准与 2×1 组件均有原生截图 | 平台与 Launcher 矩阵、48 小时后台及 7 天自用 |
| M5 公开发布准备 | 待开始 | 无 | 稳定性通过后执行 |
| M6 CI/CD 与部署 | 实现中（用户新增） | 已核对 magpie 固定提交的工作流；APK 产物脚本 4 项测试通过；新增预览与签名发布配置 | 静态检查、本地发布演练及 GitHub 实际运行 |

进度以提交、构建、测试和实际设备记录为证据，不将实现完成等同于真机验收完成。GitHub 任务会与本页及 checklist 同步更新。

## 当前环境

- 已创建原生 Android 工程、自动测试及 GitHub CI。
- 本机使用 JDK 17；2026-10-10 在忽略目录 `.tools/android-sdk` 安装 Android 36 SDK、Build Tools 35.0.0 和 platform-tools，尚无模拟器。
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
