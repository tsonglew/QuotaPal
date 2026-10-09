# QuotaPal 开发进度

更新日期：2026-10-09（Asia/Shanghai）。用户已授权开始开发；开发分支为 `codex/android-mvp`。

| 阶段 | 当前状态 | 已有证据 | 下一步 |
| --- | --- | --- | --- |
| M0 接入验证 | 进行中 | 官方协议已核对；真实额度探测 HTTP 200；认证、解析、并发、续期与清理单元测试通过 | Android 真实授权与续期仍待验收 |
| M1 规格冻结 | 已完成 | 页面与状态规格冻结；浅深色原生截图已检查；issue 和 milestone 已关闭 | 真机验收中仅修复问题，新增范围另行排期 |
| M2 账号与额度 | 实现及自动验证完成，待真实连接 | 27 项单元测试、lint、页面流程、Keystore 和独立配置测试通过；C01–C08 完成 | 等待 M0 Android 真实授权、续期及账号覆盖 |
| M3 小组件与后台 | 实现及基础设备验证完成 | 三种实际布局、更新时间可见性、共享快照和退出清理通过 | 系统选择器、点击刷新、多实例、重启与 OEM 验收 |
| M4 稳定性验证 | 自动验证通过，待真机观察 | Android 35 模拟器 4 项测试通过；7 张截图完成视觉检查 | 平台与 Launcher 矩阵、48 小时后台及 7 天自用 |
| M5 公开发布准备 | 待开始 | 无 | 稳定性通过后执行 |

进度以提交、构建、测试和实际设备记录为证据，不将实现完成等同于真机验收完成。GitHub 任务会与本页及 checklist 同步更新。

## 当前环境

- 已创建原生 Android 工程、自动测试及 GitHub CI。
- 本机有 JDK 17／23，尚无 Android SDK 或模拟器。
- 先配置可复现构建与 GitHub CI，实机和长时间观察项目保留待验收。
- 已下载自用测试 APK；[验证记录](VALIDATION.md) 保存构建版本、校验和和证据，[原生截图](SCREENSHOTS.md) 保存实际渲染结果。

## 进展记录

- 2026-10-09：启动 M0、M1；核对官方设备授权流程与额度读取路径；检查 Nowdex 的蓝色分段进度条、大号百分比、浅深色圆角卡片和更新时间布局。

- 2026-10-09：真实额度探测返回 HTTP 200，未保存身份、凭据或实际额度值。
- 2026-10-09：建立 [M0](https://github.com/tsonglew/QuotaPal/issues/1)、[M1](https://github.com/tsonglew/QuotaPal/issues/2)、[M2](https://github.com/tsonglew/QuotaPal/issues/3)、[M3](https://github.com/tsonglew/QuotaPal/issues/4)、[M4](https://github.com/tsonglew/QuotaPal/issues/5)、[M5](https://github.com/tsonglew/QuotaPal/issues/6) 跟踪任务；完成协议适配、原生页面、组件、后台同步第一轮实现，进入构建验证。
- 2026-10-09：创建 [开发 PR #7](https://github.com/tsonglew/QuotaPal/pull/7)。[首轮代码构建](https://github.com/tsonglew/QuotaPal/actions/runs/37931567792) 编译、单元测试和 APK 生成通过；lint 拦截 Glance 内部 API 使用，已改公开接口。同时改为订阅共享状态，新增组件原生渲染和 Android Keystore 验证。
- 2026-10-09：[构建验证](https://github.com/tsonglew/QuotaPal/actions/runs/37932751546) 通过编译、单元测试、lint 和 App／测试 APK 生成，进入 Android 35 模拟器阶段。C01–C05 基于实现检查及自动测试勾选；C08 为明确不支持远程撤销、界面区分本地清理。真实登录和设备验收仍保留未完成。
- 2026-10-09：[设备验证](https://github.com/tsonglew/QuotaPal/actions/runs/37933925112) 中 27 项单元测试、lint 和 3 项 Android 页面／存储测试通过；组件测试发现 Glance 单容器最多 10 个子元素，已用嵌套容器修复分段条与窗口截断。C06 基于原生加密往返、密文篡改拒绝、删除和备份关闭测试勾选。
- 2026-10-09：[后续组件回归](https://github.com/tsonglew/QuotaPal/actions/runs/37934786626) 确认容器截断日志消失；发现测试宿主未预留系统组件内边距，已校正。截图改用 MediaStore 保存，避开 AGP 测试后自动卸载清理；随后运行 [回归 37935801206](https://github.com/tsonglew/QuotaPal/actions/runs/37935801206)。另补齐并发刷新合并结果和设备码开关引导。
- 2026-10-09：[回归 37935801206](https://github.com/tsonglew/QuotaPal/actions/runs/37935801206) 全部通过；视觉检查发现组件底部更新时间被裁切，随后调整间距和最小尺寸，增加完整可见性断言，改用 PixelCopy 捕获真实圆角效果。
- 2026-10-09：[最终实现验证](https://github.com/tsonglew/QuotaPal/actions/runs/37936883382) 全部通过：27 项单元测试、lint、APK 与测试包构建、4 项 Android 35 原生测试。复查 7 张截图，确认三种组件布局的数字、分段条和更新时间完整。M1 任务及 milestone 已完成；其余阶段保留真实授权、平台／Launcher 与长期观察的待验收项。
