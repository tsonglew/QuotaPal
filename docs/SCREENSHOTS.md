# Android 原生截图

来自 [已通过的 Android 35 设备测试](https://github.com/tsonglew/QuotaPal/actions/runs/37936883382)，并经视觉检查。所有额度均为示例数据。App 由 Compose 测试捕获，组件由真实 AppWidgetHost 渲染后使用 PixelCopy 捕获。

## 额度页

| 浅色 | 深色 |
| --- | --- |
| <img src="screenshots/quota-light.png" width="300" alt="浅色额度页" /> | <img src="screenshots/quota-dark.png" width="300" alt="深色额度页" /> |

## 桌面小组件

140×150 dp 紧凑版：

<img src="screenshots/widget-140x150-light.png" width="220" alt="浅色紧凑组件" />

280×150 dp 宽版：

<img src="screenshots/widget-280x150-dark.png" width="440" alt="深色宽版组件" />

140×230 dp 纵向版：

<img src="screenshots/widget-140x230-light.png" width="220" alt="浅色纵向组件" />

## 引导与退出

| 小组件引导 | 退出后的未连接页 |
| --- | --- |
| <img src="screenshots/widgets-light.png" width="300" alt="小组件引导" /> | <img src="screenshots/welcome-dark.png" width="300" alt="退出后的欢迎页" /> |

## alpha02 可选额度状态

来自 [已通过的 alpha02 Android 35 测试](https://github.com/tsonglew/QuotaPal/actions/runs/37946436238)。均为合成示例数据；页面图片为对应 Compose 信息区域，组件图片来自实际原生宿主。

| 仅周额度 | 无周期窗口且可用 | 未知用量 |
| --- | --- | --- |
| <img src="screenshots/quota-weekly-only.png" width="300" alt="说明未提供短周期额度并保留周额度" /> | <img src="screenshots/quota-no-windows.png" width="300" alt="当前可用说明" /> | <img src="screenshots/quota-unknown-usage.png" width="300" alt="未知用量显示说明而非空进度条" /> |

| 周额度紧凑组件 | 无周期窗口组件 | 受限状态组件 |
| --- | --- | --- |
| <img src="screenshots/widget-weekly-only-140x150.png" width="220" alt="周额度提示和完整更新时间" /> | <img src="screenshots/widget-no-windows-140x150.png" width="220" alt="明确显示当前可用" /> | <img src="screenshots/widget-restricted-140x150.png" width="220" alt="明确显示当前使用受限" /> |
