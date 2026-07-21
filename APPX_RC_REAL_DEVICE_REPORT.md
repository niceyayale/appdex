# AppX Genesis RC 真机验收报告

**测试设备**: Vivo Pad (PA2170, DPD2106)  
**Android 版本**: Android 13+  
**屏幕**: 1600×2560, 400dpi  
**测试时间**: 2026-07-17  
**测试方法**: ADB 真机操作 + uiautomator + logcat  
**应用版本**: 2.0.0 (Debug)

---

## 整体评分: 62 / 100

---

## 优点 TOP20

1. **AI First 设计理念真正落地** — 首屏即 AI 对话，不是工具列表
2. **12 个 AI 提供商支持** — OpenAI / Anthropic / Gemini / DeepSeek / OpenRouter / Ollama / LM Studio / AnythingLLM / LocalAI / Custom API / OpenAI Compatible / Anthropic Compatible
3. **配置持久化完善** — API Key / Base URL / Model 在强杀重启后完整保存
4. **Command Palette 搜索体验优秀** — 搜索 "manifest" 即时返回 Manifest 编辑器，分类清晰（目标/工具/导航）
5. **错误处理有动作按钮** — AI 错误消息附带复制/重新生成/分享三个操作
6. **权限申请时机合理** — 首次启动申请存储权限，解释清楚
7. **无启动崩溃/白屏/黑屏** — 冷启动流畅
8. **工具显示模式三档可选** — 普通/高级/专家模式，适配不同用户
9. **语言切换支持中英文** — 含重启提示
10. **编辑器/终端独立字体配置** — 字体大小、Tab 宽度、回滚行数
11. **Quick Actions 设计直觉** — "分析这个 APK"、"检查是否安全"等卡片描述通俗
12. **Workspace 空状态清晰** — "开始新任务" + "选择 APK 文件开始分析"
13. **设计系统统一** — AppXTheme 贯穿全局，amberGold 品牌色一致
14. **AI 推荐区域** — 在空状态下提供引导性建议
15. **底部三 Tab 架构** — AI / 工作区 / 设置，简洁
16. **导入 APK 入口明显** — 金色按钮 "导入 APK 开始分析"
17. **搜索栏全局可用** — AI 页面顶部 Command Palette 入口
18. **无 logcat 噪音** — 启动过程无异常日志
19. **Compose UI 渲染流畅** — 无明显掉帧（除一次 Skipped 38 frames 警告）
20. **密码字段安全** — API Key 以掩码显示

---

## 缺点 TOP20

1. **P0: AI 发送按钮被键盘遮挡** — 键盘弹出时发送按钮完全不可见，用户无法发送消息（已修复 imePadding）
2. **P0: 发送按钮在 OutlinedTextField trailingIcon 内被触摸拦截** — 点击发送按钮无效（已修复：移到外部）
3. **P0: 缺少 keyboardActions** — IME Send 键无响应（已修复：添加 KeyboardActions）
4. **P1: 设置页 EditText 有焦点时无法滚动** — 输入 API Key 后无法向下滚动查看 Model/Status
5. **P1: AI 错误消息过于技术化** — "failed to connect to api.openai.com/108.160.162.109 (port 443) from /192.168.1.83 (port 44760) after 30000ms" 应转为友好提示
6. **P1: 连接超时 30 秒太长** — 用户等待 30 秒才看到错误
7. **P1: Quick Actions 在无 APK 时仍可点击** — 点击后直接弹文件选择器，缺乏引导说明
8. **P1: 无障碍支持不足** — 语言/模式选项的选中状态未通过 selected/checked 属性暴露
9. **P2: 无 APK 导入后的完整分析流程验证** — 因测试 APK 限制未完整验证
10. **P2: 深色模式未测试** — 设置中外貌选项点击未成功导航
11. **P2: 横屏适配未验证** — 未测试旋转后的布局
12. **P2: 性能压力测试未完成** — 未进行连续 20 次 AI / 10 个 APK 分析
13. **P2: Report 页面未验证** — 需要先导入 APK
14. **P2: 逆向工具未逐一验证** — Manifest/DEX/HEX/SQLite/ELF/Terminal/Diff/Signing/Repack 均需 APK 上下文
15. **P2: AI Copilot 编辑器联动未验证** — 需要先打开文件
16. **P3: 文件编码问题** — AiScreen.kt 存在双 BOM 导致编译失败
17. **P3: Gradle clean 失败** — build/outputs 目录被锁定
18. **P3: 安装超时** — Vivo Pad 安装确认对话框导致 adb install 超时
19. **P3: uiautomator dump 编码问题** — 中文显示为乱码（GBK/UTF-8 不匹配）
20. **P3: 无 LeakCanary** — 未集成内存泄漏检测

---

## P0（必须修）

| # | 问题 | 状态 | 修复方案 |
|---|------|------|----------|
| 1 | AI 发送按钮被 OutlinedTextField trailingIcon 触摸拦截 | ✅ 已修复 | 将发送/停止按钮移到 TextField 外部作为 Row sibling |
| 2 | 键盘弹出时遮挡发送按钮 | ✅ 已修复 | 添加 `Modifier.imePadding()` 到 AiInputBar |
| 3 | IME Send 键无响应 | ✅ 已修复 | 添加 `keyboardActions = KeyboardActions(onSend = { onSend() })` |
| 4 | 文件双 BOM 导致编译失败 | ✅ 已修复 | 移除重复 BOM 字节 |

---

## P1（建议修）

| # | 问题 | 建议方案 |
|---|------|----------|
| 1 | 设置页 EditText 焦点时无法滚动 | 使用 `Modifier.scrollable` 或 `focusProperties` 处理 |
| 2 | AI 错误消息过于技术化 | 捕获 `SocketTimeoutException` / `UnknownHostException` 显示 "无法连接到 AI 服务器，请检查网络" |
| 3 | 连接超时 30 秒 | 设置 10 秒连接超时 + 流式读取超时 |
| 4 | Quick Actions 无 APK 时直接弹文件选择器 | 显示 Toast "请先导入 APK" 或禁用卡片 |
| 5 | 无障碍选中状态未暴露 | 为 RadioGroup 使用 `Modifier.selectable` + `semantics` |
| 6 | 无深度链接/文件关联 | 支持 `application/vnd.android.package-archive` Intent Filter |
| 7 | 无 Crashlytics / 错误上报 | 集成 Firebase Crashlytics 或 Bugly |
| 8 | 未集成 LeakCanary | Debug 版本添加 LeakCanary 依赖 |

---

## P2（未来版本）

| # | 功能 | 说明 |
|---|------|------|
| 1 | APK 签名验证 | 导入 APK 时自动验证签名 |
| 2 | 多 APK 批量分析 | 支持同时导入多个 APK |
| 3 | 历史记录搜索 | 搜索已分析过的 APK |
| 4 | 导出报告为 PDF | Report 页面支持导出 |
| 5 | 插件系统完善 | Plugin Manager 可用性提升 |
| 6 | FTP/WebDAV 远程分析 | 远程文件直接分析 |
| 7 | DEX 反编译为 Java | 集成 jadx-core |
| 8 | 多窗口/分屏支持 | 平板分屏优化 |

---

## Bug List

| # | 严重性 | 描述 | 复现步骤 | 状态 |
|---|--------|------|----------|------|
| 1 | P0 | AI 发送按钮点击无响应 | 输入文字 → 点击发送按钮 → 无反应 | ✅ 已修复 |
| 2 | P0 | 键盘遮挡发送按钮 | 输入文字 → 键盘弹出 → 发送按钮不可见 | ✅ 已修复 |
| 3 | P0 | IME Send 键无响应 | 输入文字 → 按键盘 Send 键 → 无反应 | ✅ 已修复 |
| 4 | P1 | 设置页无法滚动（EditText 焦点时） | 输入 API Key → 尝试下滑 → 无法滚动 | 待修复 |
| 5 | P1 | adb input text 添加额外字符 | 输入 "Hello" → 显示 "Hello👀看" | 测试环境问题 |
| 6 | P3 | AiScreen.kt 双 BOM | 编辑后文件开头出现双 BOM | ✅ 已修复 |

---

## Crash List

| # | 描述 | 频率 | 状态 |
|---|------|------|------|
| - | 测试过程中未观察到 Crash | - | - |

---

## UX List

| # | 区域 | 问题 | 建议 |
|---|------|------|------|
| 1 | AI 空状态 | Quick Actions 在无 APK 时仍可点击 | 禁用或显示"请先导入 APK" |
| 2 | AI 输入栏 | 发送按钮在键盘弹出时不可见 | 已修复 (imePadding) |
| 3 | 设置页 | EditText 焦点时无法滚动 | 需修复 |
| 4 | 设置页 | 语言/模式选中状态不清晰 | 增加 visual indicator |
| 5 | Workspace | 空状态缺少工具入口 | 添加"或直接使用工具"链接 |
| 6 | AI 错误 | 技术性错误消息不友好 | 需要友好化处理 |
| 7 | 首次使用 | 无引导/教程 | 考虑添加 onboarding |
| 8 | 权限申请 | 申请后无返回确认 | 权限授予后应显示确认 |

---

## 性能报告

| 指标 | 结果 | 评价 |
|------|------|------|
| 冷启动时间 | ~3-4 秒 | 正常 |
| UI 渲染 | 1次 Skipped 38 frames | 可接受 |
| 内存 | 未观察到 OOM | 需进一步压测 |
| ANR | 未观察到 | - |
| Crash | 未观察到 | - |
| AI 响应延迟 | 30秒超时 | 过长（网络问题） |
| 配置保存 | 即时 | 正常 |
| Command Palette 搜索 | <1秒 | 优秀 |

---

## AI 联动报告

| 功能 | 状态 | 说明 |
|------|------|------|
| AI 配置 | ✅ | 12 个提供商，配置完整 |
| 配置持久化 | ✅ | 强杀重启后完整保存 |
| 消息发送 | ✅ | 已修复 P0，通过建议问题验证 |
| 流式响应 | ⚠️ | 代码已实现，但因网络无法验证 |
| 错误处理 | ✅ | 显示错误消息 + 操作按钮 |
| 错误消息友好度 | ❌ | 过于技术化 |
| AI Action Card | ⚠️ | 代码已实现，需 APK 上下文验证 |
| AI Copilot | ⚠️ | 代码已实现，需编辑器上下文验证 |
| 取消/停止 | ✅ | 代码已实现 |
| 重新生成 | ✅ | 代码已实现 |

---

## 工具联动报告

| 工具 | 页面存在 | 功能验证 | 说明 |
|------|----------|----------|------|
| Manifest 编辑器 | ✅ | ⚠️ | Command Palette 可搜索到，需 APK 上下文 |
| DEX Browser | ✅ | ⚠️ | 代码已实现，需 APK 上下文 |
| HEX Editor | ✅ | ⚠️ | 代码已实现 |
| Resource Viewer | ✅ | ⚠️ | 代码已实现 |
| Signature | ✅ | ⚠️ | 代码已实现 |
| Security Scanner | ✅ | ⚠️ | 代码已实现 |
| Report | ✅ | ⚠️ | 需 APK 上下文 |
| SQLite Viewer | ✅ | ⚠️ | 代码已实现 |
| ELF Viewer | ✅ | ⚠️ | 代码已实现 |
| Terminal | ✅ | ⚠️ | 代码已实现 |
| Diff | ✅ | ⚠️ | 代码已实现 |
| Repack | ✅ | ⚠️ | 代码已实现 |

---

## 配置持久化报告

| 配置项 | 持久化 | 说明 |
|--------|--------|------|
| API Key | ✅ | 强杀重启后保存 |
| Base URL | ✅ | 预填默认值，保存自定义 |
| Model | ✅ | 预填默认值 |
| Provider 选择 | ✅ | OpenAI 选中状态保持 |
| 工具显示模式 | ✅ | DataStore 持久化 |
| 语言 | ✅ | DataStore 持久化 |
| 编辑器字体 | ✅ | DataStore 持久化 |
| 终端配置 | ✅ | DataStore 持久化 |

---

## 真机兼容性报告

| 项目 | 结果 | 说明 |
|------|------|------|
| Vivo Pad 启动 | ✅ | 无崩溃 |
| 权限申请 | ✅ | 存储权限正常授予 |
| 文件选择器 | ✅ | 系统 DocumentsUI 正常弹出 |
| 键盘输入 | ✅ | OutlinedTextField 正常接收输入 |
| 底部导航 | ✅ | 三 Tab 切换正常 |
| 滚动 | ⚠️ | 设置页 EditText 焦点时异常 |
| 安装 | ⚠️ | Vivo 需手动确认安装 |
| 屏幕适配 | ✅ | 1600×2560 正常显示 |
| Density | ✅ | 400dpi 正常渲染 |

---

## 体验评分（每项 10 分）

| 项目 | 评分 | 说明 |
|------|------|------|
| AI | 7 | 功能完整，发送按钮已修复，错误消息需优化 |
| 工具 | 6 | 页面存在，需 APK 上下文完整验证 |
| Workspace | 6 | 空状态清晰，功能需验证 |
| Workflow | 5 | 未完整验证分析流程 |
| UX | 5 | 多个交互问题（滚动/错误消息/引导） |
| 动画 | 7 | 渐变分割线等细节到位 |
| 颜色 | 8 | 品牌色一致，暗色主题待验证 |
| 字体 | 7 | 可配置，但默认偏小 |
| 图标 | 7 | Material Icons 统一 |
| 一致性 | 7 | Design System 贯穿 |
| 学习成本 | 6 | AI 引导降低门槛，但工具仍需学习 |
| 专业能力 | 7 | 12 个 AI 提供商 + 完整工具链 |
| 逆向体验 | 6 | 需完整 APK 分析验证 |
| 新手体验 | 5 | 缺少引导，Quick Actions 无 APK 时困惑 |
| 稳定性 | 7 | 无崩溃，但有几个 P0/P1 Bug |
| 完成度 | 6 | 核心功能在，但多处断裂需修复 |

**总分: 62 / 100**

---

## 竞品对比

| 维度 | AppX | APKTool M | MT Manager | NP Manager | JADX | MobSF |
|------|------|-----------|------------|------------|------|-------|
| AI 集成 | ⭐⭐⭐⭐ | ❌ | ❌ | ❌ | ❌ | ❌ |
| APK 分析 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| DEX 浏览 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| Manifest 编辑 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐ |
| 签名/重打包 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ❌ | ⭐ |
| 安全扫描 | ⭐⭐⭐ | ⭐ | ⭐ | ⭐ | ⭐ | ⭐⭐⭐⭐⭐ |
| UI/UX | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| 平板适配 | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐ |
| 开源 | ⭐⭐⭐⭐⭐ | ❌ | ❌ | ❌ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

**AppX 领先**: AI 集成（唯一）、平板适配、开源、UI/UX 设计  
**AppX 落后**: DEX 反编译深度、签名/重打包成熟度、工具实际可用性验证  
**AppX 已超过**: 在 AI 引导 + 工具组合的产品形态上无直接竞品

---

## 最终是否建议发布: **NO**

### 还差什么:

1. **P0 已修复但需最终验证**: 发送按钮修复需在真机上用真实 API Key 完整验证流式响应
2. **P1 必须修复**: 
   - 设置页滚动问题（影响所有用户配置体验）
   - AI 错误消息友好化（影响用户信任）
   - 连接超时缩短至 10 秒
3. **关键流程未验证**: 
   - APK 导入 → 分析 → Report 完整流程
   - AI Action Card → 工具跳转联动
   - AI Copilot → 编辑器建议
4. **缺少 Crashlytics**: 发布前必须集成崩溃上报
5. **缺少 Release 签名**: 当前仅 Debug APK

### 距离 Google Play 推荐还差:
- 完整的 APK 分析流程无 Bug 验证
- AI 与工具的联动体验闭环
- 错误处理的全面友好化
- 深色模式 + 横屏适配验证
- 性能压测通过（连续 20 次 AI / 10 个 APK）
- Release 签名 + ProGuard 混淆
- Google Play Privacy Policy + Data Safety 表

**预计还需 1-2 个迭代周期可达到发布标准。**
