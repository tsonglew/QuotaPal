# Codex 接入验证

更新日期：2026-10-09。结论：已验证真实额度读取，首版采用实验性直连适配器。Android 设备登录、续期及账号类型覆盖仍需实际设备验证，M0 尚未全部验收。

## 已取得的证据

使用本机已经登录 Codex 的现有 access token，对 OpenAI 的额度接口执行一次只读请求，返回 HTTP 200。未刷新现有凭据，未保存原始响应、账号身份、实际额度值或 token。

响应包括 `rate_limit`、`additional_rate_limits`、`plan_type`、`rate_limit_reset_credits` 等字段。此次账号仅返回一个主窗口，窗口包含 `used_percent`、`limit_window_seconds`、`reset_at`、`reset_after_seconds`。因此实现按实际窗口渲染，不推测缺失的会话窗口。

复现工具为 `scripts/probe_codex.py`。工具只输出 HTTP 状态、字段名称与窗口数量；输入为用户本机 Codex auth cache，不会将凭据复制到仓库或 Android App。

## 协议边界

依据官方开源 Codex 实现核对认证与额度路径。参考版本为 `openai/codex` commit `a06545b311fe01e51ce855c7aa5d8da21e9e7aaf`。

| 用途 | 请求 |
| --- | --- |
| 设备登录码 | `POST https://auth.openai.com/api/accounts/deviceauth/usercode` |
| 等待用户授权 | `POST https://auth.openai.com/api/accounts/deviceauth/token` |
| 授权码交换 | `POST https://auth.openai.com/oauth/token`，form encoded |
| 令牌续期 | `POST https://auth.openai.com/oauth/token`，JSON refresh grant |
| 额度读取 | `GET https://chatgpt.com/backend-api/wham/usage` |

额度请求使用 Bearer access token 和 `ChatGPT-Account-Id` header。凭据不出现在 URL 中。设备登录码在用户自己的设备中展示，用户在 OpenAI 页面完成授权；取消或超时停止轮询。

目前使用官方开源 Codex 的公开客户端标识，标识不是 secret。该兼容路径不等同于获得面向第三方 Android App 的稳定公共 API 或官方合作资格。用户授权页面可能显示 Codex。公开发布前需要重新确认独立客户端身份、接入支持范围和维护策略；此项保持未完成。

应用仅执行认证、续期和额度读取，不发起模型请求、不消耗重置次数。登录凭据的权限可能广于额度读取，不能描述为只读凭据。

token 中的账号和过期字段只用作路由与刷新提示，不以未经验证的 JWT 内容授予应用权限。真正的访问权限由额度服务验证；如果响应返回不同账号 ID，则拒绝展示。应用不创建自有服务或中转账号凭据。

## 存储与恢复

- Android Keystore 管理 AES-GCM 加密密钥，凭据密文通过 AtomicFile 写入 noBackupFilesDir。
- 账号额度快照存入 Room，偏好及每个小组件配置存入 DataStore；禁用云备份与设备迁移。
- 同账号刷新串行化，旋转后的 refresh token 与 access token 一并替换。已有桌面 Codex 的 refresh token 不会由验证脚本使用。
- 退出立即使进行中的请求失效，清除本机凭据、缓存与后台任务，更新小组件。
- 退出仅清除本机连接，不宣称远程授权已被撤销。
- 服务错误不暴露原始响应内容；保留最后成功快照与其获取时间。

## 尚需验证

- Android 真实用户首次授权、取消、超时、重新连接、续期及撤销后的恢复。
- 不同套餐与工作区，以及账号切换和权限限制。
- 真机凭据恢复、密钥不可用与备份迁移场景。
- 公开发布的独立客户端接入条件及后端协议变化策略。

## 参考来源

- [官方认证说明](https://learn.chatgpt.com/docs/auth)
- [官方 App Server 文档](https://learn.chatgpt.com/docs/app-server)
- [设备授权源码](https://github.com/openai/codex/blob/a06545b311fe01e51ce855c7aa5d8da21e9e7aaf/codex-rs/login/src/device_code_auth.rs)
- [额度请求源码](https://github.com/openai/codex/blob/a06545b311fe01e51ce855c7aa5d8da21e9e7aaf/codex-rs/backend-client/src/client/rate_limit_resets.rs)
- [认证与续期源码](https://github.com/openai/codex/blob/a06545b311fe01e51ce855c7aa5d8da21e9e7aaf/codex-rs/login/src/auth/manager.rs)
