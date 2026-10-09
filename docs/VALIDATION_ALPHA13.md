# alpha13 验证记录

2026-10-10，versionCode 13。

## 启动存储异常

alpha12 CI 37.0 的原生安装成功后，首项 App 测试未能解析活动，日志同时出现 Room 读取时 `SQLiteCantOpenDatabaseException`（数据库目录缺失）。运行环境仍有 SurfaceFlinger 重启，不能断言目录缺失的根因在 App；但仓库 initialize 只捕获 ApiFailure，其他本地存储异常会逃逸到启动协程，这是独立需要处理的问题。

初始化改为把设置／凭据／缓存读取异常转为 STORAGE 状态，保留已读到的连接身份和原凭据，不清理用户数据、不伪造成功缓存；协程取消继续向上传播。新增三项 JVM 回归：数据库不可读后恢复并成功刷新、设置读取失败不删除凭据、取消不误报存储错误。50 项 JVM 测试、lint、应用与测试 APK 构建通过。API 37 正常 AppFlow 三项回归通过（19.147 秒）。这里的故障注入验证仓库边界，不声称覆盖整个 App 的所有存储故障路径。

## 无障碍测试环境

alpha12 最新代码在 API 37 software 后端的五项组件渲染均通过；同轮 TalkBack 被系统弹窗遮挡，整轮明确失败（177.558 秒）。补充超时截图与节点日志后证实根节点为 System UI 无响应对话框，另有通知权限弹窗。

冷启动 4GB 模拟器，探针仅临时授予 TalkBack 通知权限、finally 恢复原权限；原始焦点／双击／一次请求／62%→83% 断言独立通过（19.861 秒）。外部确认 font_scale=1.0、enabled_accessibility_services=null、TalkBack POST_NOTIFICATIONS=false，均恢复原值。结合 API 29／31／35／36 的渲染 CI，D09 完成；E06 的完整最新版本 CI 仍未通过。

## Android 17 云端图形问题

CI 37998260240 全部终态，29／31／35／36（包括真实改时及真实网络探针）成功；37.0／37.2 software 后端仍出现相同 SurfaceFlinger 断言，CI gate 失败，证明单纯切软件后端未解决问题。

根据 Gfxstream 当前源码试验关闭 GlDirectMem 和 HasSharedSlotsHostMemoryAllocator；本地启动检查一度通过，但实际 instrumentation 启动遇到 DeadObjectException。此实验未证明稳定，已撤回，不提交到 CI。保留原始失败证据，Android 17 完整矩阵仍待解决。
