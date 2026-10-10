# 后台更新引导

2026-10-10 用户反馈：小米 17 Ultra／Android 17 清理后台后额度不再自动更新。尚无设备日志，不能确定为厂商清理、强制停止、网络或系统休眠中的哪一种。

设置页新增“后台更新保障”，默认按制造商与品牌识别，支持手动切换。显示各品牌启动、后台耗电和最近任务锁定指引；提供应用信息及系统电池优化列表入口，入口不可用时回退。返回 App 时重新读取 Android 电池优化豁免状态。此状态不代表厂商自启动或锁定已设置，不保存虚假的“已配置成功”。

## 调研依据与适用范围

核对日期：2026-10-10。以下是菜单参考，尚非当前各品牌最新系统的真机验收结果。厂商会按地区、系统版本变更菜单，App 提供搜索关键词和手动选择，不依赖未公开 Activity 名称。

| 品牌 | 引导重点 | 官方依据与限制 |
| --- | --- | --- |
| 小米／Redmi／POCO | 后台自启动、无限制省电、最近任务锁定或安全中心锁定应用 | [小米后台自启动](https://www.mi.com/global/support/faq/details/KA-507608/)、[Redmi 后台锁定](https://www.mi.com/global/support/faq/details/KA-535435/)、[小米后台运行策略](https://privacy.mi.com/xiaomihealth/en_US)。资料覆盖其他机型，17 Ultra／Android 17 菜单待用户真机核对。 |
| vivo／iQOO | 自启动、后台耗电管理、可用时锁定任务 | [vivo 支持说明](https://kefu.vivo.com.cn/robot/imgmsgData/2616a9cd7cd64a5083a264d16e5767da/index_1.html) 提供自启动与后台高耗电路径；资料较旧，OriginOS 新版入口以设备为准，锁定步骤作为可选项。 |
| 华为／荣耀 Android 兼容设备 | 启动改为手动管理，允许自启动和后台活动 | [荣耀后台运行说明](https://www.honor.com/cn/support/content/zh-cn00406916/)、[华为启动管理说明](https://consumer.huawei.com/cn/support/content/zh-cn15831372/)。华为引用为智慧屏资料，只用于启动管理概念；不宣称兼容 HarmonyOS NEXT。 |
| Samsung | 从休眠／深度休眠移除，加入从不休眠，允许后台 | [Samsung 支持](https://www.samsung.com/us/support/answer/ANS10003442/)、[Samsung 开发者应用管理](https://developer.samsung.com/mobile/app-management.html)。保持打开为机型可选功能，不等同自启动。 |
| OPPO／OnePlus／realme | 搜索自启动、允许后台活动、可用时锁定任务 | [OPPO 官方旧版用户手册](https://ipics.oppo.com/oppo_nl/answer/OPPO_Smartphone_ColorOS_3.1_User_Guide_180217.pdf)、[OnePlus 电池优化手册](https://service.oneplus.com/content/dam/support/user-manuals/common/OnePlus_9_User_Manual_EN.pdf)。当前系列菜单尚缺完整官方资料，故采用搜索和“若有此项”的条件引导，不保证同一路径适用于三品牌。 |
| Pixel／原生／其他 | 允许后台电池与网络使用，不要求不存在的自启动开关 | [Pixel 电池帮助](https://support.google.com/pixelphone/answer/6090612?hl=en-GB)。其他厂商提供通用设置搜索。 |

## 恢复与验证

[Android 强制停止规则](https://developer.android.com/about/versions/15/behavior-changes-all#enhanced-stop-states)说明 stopped 状态需要用户交互解除。引导要求重新打开 App；现有 onForeground 会刷新并重新协调唯一周期任务。后台锁定只能减少清理影响，不能保证常驻或绕过强制停止。

配置后返回 App 刷新一次，再回到桌面记录最后成功更新时间、清理方式、系统省电状态和网络。至少观察两个目标周期；系统调度可能延迟，不能只凭未准点更新判断失败。需要分别验证：普通退出、最近任务一键清理、锁屏休眠、重启、强停后重开。

小米实际设置与自动更新恢复、其他 OEM 菜单跳转、后台观察均保留待验收。引导不改变轮询频率，不新增常驻服务、唤醒锁或直接申请忽略省电的权限。
