# 隐私说明与数据流核对

更新：2026-10-10。完整政策采用随 App 打包的[隐私说明原文](../app/src/main/res/raw/privacy_notice.txt)，设置和连接对话框读取同一份文件，离线可查看。此页为实现核对，不是另一份政策。

| 数据／行为 | 实现证据 |
| --- | --- |
| 授权、续期、额度请求域名及携带信息 | `CodexApi.kt`：authBase、usageUrl、设备码／令牌请求、Authorization／ChatGPT-Account-Id headers；没有聊天生成调用 |
| 凭据加密与存放 | `Storage.kt`：CredentialVault、noBackupFilesDir、AndroidKeyStore、AES/GCM/NoPadding、原子文件 |
| 快照及偏好 | `Storage.kt`：Room 单槽 usage_snapshot，DataStore 显示偏好、组件配置、尝试与错误类别；快照并非加密数据库 |
| 桌面展示 | `QuotaWidget.kt`：额度、状态、成功时间传给 Launcher；不展示原始令牌 |
| 退出 | `UsageRepository.logout()`、`MainViewModel.logout()`：删除登录文件及快照、清理同步状态、取消任务、更新组件；保留显示偏好，不调用远程撤销 |
| 备份 | 主 manifest 与备份 XML：allowBackup=false，关闭云备份／迁移；登录文件另放 noBackup 目录 |
| 数据上报 | 当前依赖及调用未包含广告、分析、自动崩溃上报或自建服务；CI 的测试产物不包含用户连接数据 |

账号连接的实际权限、续期、服务方适用范围仍由 M0 真机接入验证完成，本政策不替代其验收。公开 APK／Google Play 渠道及对应平台要求尚待 F01，不能将本地隐私入口等同于已满足商店发布要求。
