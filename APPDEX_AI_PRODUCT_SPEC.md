# AppDex 2.0 AI 产品设计规格书

> **这是 AppDex 2.0 最重要的一份文档。**
>
> 角色：产品经理 × UX Designer × AI 产品负责人 × 普通用户
>
> 原则：不考虑现有代码，不考虑现有架构，不考虑 Sprint。从零开始定义 AppDex AI。
>
> 核心理念：**AI 只是入口。真正工作的仍然是 AppDex 自己。**

---

## 目录

- [第一部分：AI 产品定位](#第一部分ai-产品定位)
- [第二部分：AI 工作流程](#第二部分ai-工作流程)
- [第三部分：AI 对话能力](#第三部分ai-对话能力)
- [第四部分：AI 可调用的 AppDex 能力清单](#第四部分ai-可调用的-appdex-能力清单)
- [第五部分：AI 永远不能做什么](#第五部分ai-永远不能做什么)
- [第六部分：三级模式设计](#第六部分三级模式设计)
- [第七部分：AI 驱动 UI 的交互设计](#第七部分ai-驱动-ui-的交互设计)
- [第八部分：Action Card 设计](#第八部分action-card-设计)
- [第九部分：Prompt Library](#第九部分prompt-library)
- [第十部分：未来 Agent 能力规划](#第十部分未来-agent-能力规划)
- [第十一部分：AI Provider 设计](#第十一部分ai-provider-设计)
- [第十二部分：首页重新设计](#第十二部分首页重新设计)

---

---

## 第一部分：AI 产品定位

### 1.1 AppDex 的新定位

**AppDex 不是：** APK 逆向工具

**AppDex 是：** AI 驱动的 APK 分析助手

AppDex 2.0 的核心转变不是技术升级，而是**关系重构**。

在 1.0 时代，用户面对的是一堆专业工具：DEX Browser、Manifest 查看器、HEX 编辑器、签名工具、重打包工具……用户需要自己知道"我要做什么"，然后找到对应工具，然后解读工具输出的原始数据。

在 2.0 时代，用户面对的是一个**懂 APK 的 AI 助手**。用户只需要说出自己的疑问或目标，AI 就会：
1. 自动调用 AppDex 底层的分析能力
2. 读取分析结果
3. 把原始数据翻译成用户能理解的语言
4. 给出结论和建议
5. 在需要操作时，直接提供操作入口

**关键区别：** AI 不是替代 AppDex 的工具，而是**站在所有工具之上的智能层**。AI 理解用户意图，调度底层工具，解释工具输出。真正的解析、扫描、编辑、签名、重打包——所有脏活累活仍然由 AppDex 自己的引擎完成。

### 1.2 AI 是什么

| 维度 | 定义 |
|---|---|
| **角色** | AppDex 的"大脑"和"翻译官" |
| **职责** | 理解用户意图 → 调用 AppDex 能力 → 解读分析结果 → 输出人话 → 提供操作入口 |
| **定位** | 用户与 AppDex 专业能力之间的桥梁 |
| **类比** | AI 是前台接待 + 翻译 + 导航员。它不负责修理，但负责把你领到对的修理师傅面前，并用你能听懂的话解释问题 |
| **存在形式** | 对话界面（主入口）+ Action Card（操作入口）+ 自动分析（被动触发） |
| **智能层级** | 理解自然语言 → 识别意图类型 → 匹配 AppDex 能力 → 组装上下文 → 调用 LLM → 解读结果 → 生成操作建议 |

### 1.3 AI 不是什么

| AI 不是 | 原因 |
|---|---|
| **AI 不是破解器** | AI 不会自动绕过授权、不会自动修改收费逻辑、不会生成破解版 APK |
| **AI 不是搜索引擎** | AI 不会联网搜索通用知识，它只回答与当前 APK 相关的问题 |
| **AI 不是代码执行器** | AI 不会直接修改 APK 文件、不会直接执行 Smali、不会直接重打包。它提供"建议"和"入口"，由用户决定是否执行 |
| **AI 不是万能的** | AI 依赖 LLM 的能力，受限于模型本身的推理能力。复杂逻辑分析可能不准确 |
| **AI 不是替代品** | AI 不替代 DEX Browser、不替代 Security Scanner、不替代任何现有工具。所有工具保留，AI 只是更友好的入口 |
| **AI 不是云端服务** | AppDex 没有服务器。AI 调用由用户自己配置的 LLM API。数据直接从用户设备发送到用户选择的 LLM 服务商 |
| **AI 不是代码生成器** | AI 可以建议修改方案（Smali/Java/Kotlin/资源），但不会自动编译和注入代码。用户需要手动执行 |

### 1.4 AI 解决什么问题

| 问题 | 当前痛点 | AI 如何解决 |
|---|---|---|
| **"这个 APP 安全吗？"** | 普通用户看不懂权限列表、签名信息、安全扫描结果 | AI 自动读取所有安全分析数据，综合判断，给出"安全/基本安全/有风险/高危"的结论和理由 |
| **"为什么打不开？"** | 用户不知道从哪里排查闪退原因 | AI 分析 Manifest（exported 组件）、权限缺失、签名问题、minSdk 不兼容等，给出可能原因列表 |
| **"广告在哪？"** | 用户不知道怎么定位广告 SDK | AI 扫描 APK 内文件名和 DEX 字符串，识别广告 SDK（AdMob、Facebook Ads、AppLovin 等），定位其位置 |
| **"怎么修改名字？"** | 用户不知道 AppName 存在 resources.arsc 中 | AI 告诉用户"应用名在 resources.arsc 的 string/app_name 资源中"，并直接提供"打开资源编辑器"按钮 |
| **"为什么会员验证失败？"** | 用户不知道怎么找到会员验证逻辑 | AI 在 DEX 中搜索关键词（VIP、premium、subscribe、verify 等），定位相关类和方法，展示 Smali 代码 |
| **"本地会员在哪里？"** | 同上 | AI 分析 DEX 字符串和类结构，推测会员验证逻辑的代码位置 |
| **"为什么检测 Root？"** | 用户不知道 Root 检测的代码模式 | AI 搜索 DEX 中的 Root 检测关键词（su、magisk、busybox、RootChecker 等），定位检测代码 |
| **"为什么闪退？"** | 闪退原因复杂 | AI 综合分析：SDK 兼容性、签名验证逻辑、权限冲突、组件配置错误等，给出排查清单 |
| **"为什么联网？"** | 用户不知道 APP 在和谁通信 | AI 分析 Manifest 中的 INTERNET 权限、DEX 中的 URL 字符串、追踪 SDK，列出网络通信目标 |
| **"这个权限是干什么的？"** | 权限名是英文技术术语 | AI 用中文解释每条权限的用途、风险等级、是否必要 |
| **"我想去掉广告"** | 用户不知道怎么操作 | AI 定位广告 SDK 位置，建议修改方案（禁用广告 Activity、修改广告初始化代码等），提供操作入口 |
| **"签名是什么意思？"** | 签名术语难以理解 | AI 用通俗语言解释 V1/V2/V3 签名的含义，评估当前签名是否安全 |
| **"这个 APP 会不会偷隐私？"** | 用户无法判断 | AI 综合权限列表、追踪 SDK、网络权限、危险权限数量，给出隐私风险评估 |

### 1.5 AI 不能解决什么问题

| AI 不能解决 | 原因 | 替代方案 |
|---|---|---|
| **自动生成破解版 APK** | 违反产品定位和法律底线 | AI 可以提供修改建议，但用户需要手动操作 |
| **自动绕过付费验证** | 同上 | AI 可以定位付费验证逻辑位置，但不提供绕过方案 |
| **保证 100% 准确的代码分析** | LLM 推理能力有限，复杂逻辑可能误判 | AI 回答附带"此分析基于 AI 推理，可能不准确"的提示 |
| **实时监控 APP 行为** | AppDex 是静态分析工具，不是动态调试器 | 推荐使用 Frida/Xposed 等动态工具 |
| **修改加密/混淆的代码** | 混淆代码无法被 LLM 有效理解 | AI 可以提示"检测到代码混淆，建议先反混淆" |
| **处理超大 APK（>200MB）** | DEX 字符串提取和上下文组装可能超时 | AI 提示"APK 过大，分析可能不完整" |
| **编译/运行 Java/Kotlin 代码** | AppDex 没有编译器 | AI 提供代码建议，用户需要在外部编译后注入 |
| **获取 APP 的服务端逻辑** | 服务端代码不在 APK 中 | AI 明确告知"此逻辑在服务端，无法从 APK 分析" |
| **联网查询最新漏洞** | AppDex 的 AI 不联网搜索 | AI 基于训练数据回答，提示"信息可能不是最新的" |

### 1.6 AI 的价值主张

**对普通用户：**
- 不需要学习任何技术术语
- 不需要知道什么是 DEX、Manifest、Smali
- 只需要用自然语言提问
- AI 负责翻译、定位、解释、建议
- 所有操作一键跳转到对应工具

**对中级用户：**
- AI 加速分析速度，不需要逐个工具手动操作
- AI 提供综合判断，不需要自己交叉比对多个工具的输出
- 可以直接进入高级模式，使用原始工具

**对专业用户：**
- AI 处理重复性工作（权限审计、安全扫描总结）
- 专家可以跳过 AI，直接使用所有原始工具
- AI 的分析结果可以交叉验证

---

---

## 第二部分：AI 工作流程

### 2.1 完整用户旅程

```
┌─────────────────────────────────────────────────────────────────────┐
│                        AppDex 2.0 用户旅程                           │
└─────────────────────────────────────────────────────────────────────┘

  ┌──────────┐
  │ 打开 App │
  └────┬─────┘
       │
       ▼
  ┌──────────────────────────┐
  │ 首页                      │
  │ ┌──────────────────────┐ │
  │ │  AI 欢迎卡            │ │
  │ │  "你好，我能帮你分析 APK" │ │
  │ │  [选择 APK] [扫描已安装] │ │
  │ └──────────────────────┘ │
  │ ┌──────────────────────┐ │
  │ │ 最近任务列表           │ │
  │ └──────────────────────┘ │
  └────────────┬─────────────┘
               │
               │ 用户点击 [选择 APK]
               ▼
  ┌──────────────────────────┐
  │ 文件选择器                │
  │ 选择 .apk 文件            │
  └────────────┬─────────────┘
               │
               ▼
  ┌──────────────────────────┐
  │ 自动分析阶段              │
  │                          │
  │ Step 1: ApkFile.parse()  │  ← AppDex 引擎解析 APK
  │   → Manifest             │  ← 提取包名/版本/权限/组件
  │   → Signatures           │  ← 提取 V1/V2/V3 签名
  │   → Entries              │  ← 提取文件列表
  │   → FileSize             │
  │                          │
  │ Step 2: SecurityScanner  │  ← AppDex 安全引擎扫描
  │   → 危险权限检测           │
  │   → 硬编码密钥检测         │
  │   → 追踪 SDK 检测         │
  │   → 弱加密检测            │
  │   → 签名安全检测          │
  │   → SecurityScore        │
  │                          │
  │ Step 3: DexRepository    │  ← AppDex DEX 引擎解析
  │   → DEX 文件列表          │
  │   → 类列表                │
  │   → 关键字符串提取         │
  │                          │
  │ Step 4: 体积分析          │  ← AppDex 体积分析器
  │   → 各组件大小占比         │
  │                          │
  │ Step 5: 上下文组装        │  ← AI 读取所有分析结果
  │   组装为 AI 可读的上下文文本 │
  └────────────┬─────────────┘
               │
               ▼
  ┌──────────────────────────────────────────┐
  │ AI 自动总结阶段                             │
  │                                          │
  │ AI 读取完整上下文后，自动生成：              │
  │                                          │
  │  ┌────────────────────────────────────┐  │
  │  │ 📊 安全评分：85/100  低风险          │  │
  │  │                                    │  │
  │  │ 📝 AI 总结：                       │  │
  │  │ "这是一个正常的社交应用。它申请了     │  │
  │  │  摄像头和位置权限（用于拍照和分享     │  │
  │  │  位置），集成了 Google AdMob 广告   │  │
  │  │  SDK，使用 V2+V3 签名，证书有效。   │  │
  │  │  没有发现硬编码密钥或恶意行为。"    │  │
  │  │                                    │  │
  │  │ ⚠ 发现 2 个注意事项：              │  │
  │  │  · 集成了 3 个追踪 SDK              │  │
  │  │  · 申请了 5 个危险权限              │  │
  │  │                                    │  │
  │  │ [询问 AI 更多问题]                  │  │
  │  └────────────────────────────────────┘  │
  └────────────┬─────────────────────────────┘
               │
               ▼
  ┌──────────────────────────────────────────┐
  │ 用户对话阶段                               │
  │                                          │
  │ 用户输入框 + 快捷问题按钮                  │
  │                                          │
  │ 快捷问题：                                │
  │  · 这个 APP 安全吗？                       │
  │  · 广告在哪？                             │
  │  · 为什么申请摄像头权限？                   │
  │  · 怎么修改应用名称？                       │
  │                                          │
  │ 用户：这个 APP 会不会偷隐私？               │
  │                                          │
  │ AI：（调用 SecurityScanner + Manifest）   │
  │ ┌────────────────────────────────────┐  │
  │  │ "根据我的分析：                    │  │
  │  │                                   │  │
  │  │ 隐私风险：中等                     │  │
  │  │                                   │  │
  │  │ 1. 位置权限：用于附近的人功能       │  │
  │  │ 2. 通讯录：用于好友推荐            │  │
  │  │ 3. 集成了 3 个追踪 SDK（见下）      │  │
  │  │ 4. 未发现数据上传到可疑服务器       │  │
  │  │                                   │  │
  │  │ [查看追踪 SDK详情] [查看权限详情]  │  │
  │  │ [查看完整安全报告]"                │  │
  │  └────────────────────────────────────┘  │
  │                                          │
  │ 用户继续追问：                              │
  │ "那 3 个追踪 SDK 是什么？"                 │
  │                                          │
  │ AI：（调用 SecurityScanner.scanTrackingSdk）│
  │ ┌────────────────────────────────────┐  │
  │  │ "检测到以下追踪 SDK：              │  │
  │  │                                   │  │
  │  │ 1. Google AdMob — 广告推送         │  │
  │  │ 2. Firebase Analytics — 数据分析  │  │
  │  │ 3. Umeng Analytics — 友盟统计     │  │
  │  │                                   │  │
  │  │ 这些 SDK 会收集设备信息、使用习惯   │  │
  │  │ 等数据用于广告推送和分析。          │  │
  │  │                                   │  │
  │  │ [查看 DEX 中的 SDK 代码]"           │  │
  │  └────────────────────────────────────┘  │
  └──────────────────────────────────────────┘
```

### 2.2 流程节点详细说明

#### 节点 1：打开 App

| 状态 | 行为 |
|---|---|
| 首次打开 | 显示首页 AI 欢迎卡 + 空最近任务列表 + 引导文案 |
| 非首次打开 | 显示首页 AI 欢迎卡 + 最近任务列表（可点击恢复） |
| 有未完成任务 | 首页顶部显示"继续上次任务"横幅 |

#### 节点 2：选择 APK

| 入口 | 行为 |
|---|---|
| 首页 [选择 APK] | 打开系统文件选择器，过滤 .apk 文件 |
| 首页 [扫描已安装应用] | 列出设备已安装应用，选择后导出 APK |
| 首页 FAB | 同 [选择 APK] |
| 文件管理器点击 .apk | 直接进入分析流程 |
| 最近任务点击 | 恢复之前的分析会话，跳过重新分析 |

#### 节点 3：自动分析

分析过程完全自动，用户只需等待。分析阶段展示进度：

```
┌──────────────────────────────────────┐
│          正在分析 APK...              │
│                                      │
│  ✓ 解析 Manifest                     │
│  ✓ 提取签名信息                       │
│  ✓ 扫描文件列表                       │
│  ✓ 安全扫描中...                      │
│  ○ DEX 解析                          │
│  ○ 体积分析                           │
│  ○ AI 总结生成                        │
│                                      │
│  ████████████░░░░░░░░  65%           │
└──────────────────────────────────────┘
```

**分析步骤（自动执行，不可跳过）：**

| 步骤 | 引擎 | 产出 |
|---|---|---|
| 1. APK 解析 | ApkFile.parse() | ApkInfo（Manifest + Signatures + Entries + FileSize） |
| 2. 安全扫描 | SecurityScannerRepository.scan() | SecurityScanResult（危险权限 + 硬编码密钥 + 追踪SDK + 弱加密 + 安全评分） |
| 3. DEX 解析 | DexRepository.listDexFiles() + listClasses() | DEX 文件列表 + 类列表（前 N 个） |
| 4. 体积分析 | SizeAnalyzerRepository | 各组件大小占比 |
| 5. 上下文组装 | ApkContextBuilder | 将以上结果组装为 AI 可读文本 |
| 6. AI 总结 | AiService.ask(SUMMARY_PROMPT, context) | 自然语言总结报告 |

#### 节点 4：AI 自动总结

分析完成后，AI 自动生成一份"一页纸"总结报告，包含：

| 内容 | 数据来源 | 示例 |
|---|---|---|
| 安全评分 | SecurityScoreCalculator | "85/100 低风险" |
| 一句话结论 | AI 总结 | "这是一个正常的社交应用" |
| 关键发现 | SecurityScanner + AI 解读 | "集成 3 个追踪 SDK，申请 5 个危险权限" |
| 注意事项 | AI 判断 | "签名仅 V1，建议升级 V2+" |
| 行动建议 | AI 建议 | "[查看安全报告] [询问 AI]" |

#### 节点 5：用户对话

用户在 AI 总结下方可以继续提问。AI 对话具有上下文感知能力：

- AI 始终知道当前正在分析哪个 APK
- AI 可以随时调用 AppDex 的底层能力获取更多数据
- AI 回复中可以嵌入 Action Card（操作入口按钮）
- 对话历史在当前会话内保持

### 2.3 非标准流程

#### 流程 B：从工具直接进入

```
用户打开 AppDex
  → 首页 → 工具 Tab
  → 直接使用 DEX Browser / HEX Editor / Terminal 等
  （不经过 AI，纯专家模式）
```

#### 流程 C：从文件管理器进入

```
用户打开 AppDex
  → 文件 Tab
  → 浏览文件
  → 点击 .apk 文件
  → 自动进入分析流程 → AI 总结
```

#### 流程 D：从最近任务恢复

```
用户打开 AppDex
  → 首页 → 最近任务列表
  → 点击任务
  → 恢复之前的分析会话（不重新分析）
  → 显示 AI 总结 + 对话历史
```

---

---

## 第三部分：AI 对话能力

### 3.1 对话能力分类

AppDex AI 支持以下 8 大类对话能力：

#### 能力 1：解释（Explain）

**定义：** 用通俗语言解释技术概念和分析结果。

| 用户可能说 | AI 行为 |
|---|---|
| "什么是 DEX？" | 解释 DEX 的概念和作用，不涉及当前 APK |
| "什么是签名？" | 解释 V1/V2/V3 签名的区别和作用 |
| "什么是 Smali？" | 解释 Smali 是 DEX 的反汇编表示 |
| "这个权限是什么意思？" | 解释当前 APK 中指定权限的用途和风险 |
| "什么是 resources.arsc？" | 解释资源表的作用 |
| "什么是 exported Activity？" | 解释导出组件的安全风险 |
| "debuggable 是什么？" | 解释 debuggable 标志的安全影响 |

**AI 内部行为：** 直接基于知识库回答，不需要调用 AppDex 能力。

#### 能力 2：定位（Locate）

**定义：** 在 APK 中定位用户关心的特定内容的位置。

| 用户可能说 | AI 调用的能力 | AI 输出 |
|---|---|---|
| "广告在哪？" | SecurityScanner.scanTrackingSdk + Entries 搜索 | "广告 SDK 位于：lib/ 目录下的 com/google/android/gms/ads 包中，DEX 中有对应类。" + [查看 DEX] |
| "登录逻辑在哪？" | DexRepository.listClasses + 字符串搜索 | "在 classes.dex 中找到 LoginActivity 类，位于 com.example.auth 包下。" + [查看代码] |
| "会员验证在哪？" | DexRepository + 字符串搜索（VIP/premium/verify） | "推测在 com.example.billing.VipVerifyActivity 中。" + [查看代码] |
| "Root 检测在哪？" | DexRepository + 字符串搜索（su/magisk/busybox） | "在 com.example.security.RootChecker 中发现 Root 检测代码。" + [查看代码] |
| "网络请求在哪？" | DexRepository + URL 字符串提取 | "DEX 中发现 15 个 URL 字符串，主要集中在 com.example.api 包。" + [查看 URL 列表] |
| "本地数据存在哪？" | Entries 搜索（.db 文件）+ SQLite Viewer | "应用使用 SQLite 数据库，路径：/data/data/com.example/databases/app.db" + [查看数据库] |
| "配置文件在哪？" | Entries 搜索（.json/.xml/.properties） | "在 assets/ 目录下找到 config.json" + [查看文件] |
| "加密逻辑在哪？" | DexRepository + 字符串搜索（AES/DES/Cipher） | "在 com.example.crypto.CryptoUtils 中发现加密代码。" + [查看代码] |

#### 能力 3：分析（Analyze）

**定义：** 对 APK 进行深度分析，给出综合判断。

| 用户可能说 | AI 调用的能力 | AI 输出 |
|---|---|---|
| "这个 APP 安全吗？" | SecurityScanner + SecurityScoreCalculator + Manifest | 综合安全评估报告 |
| "这个 APP 会偷隐私吗？" | SecurityScanner（权限+追踪SDK+硬编码） | 隐私风险评估 |
| "为什么打不开/闪退？" | Manifest（exported/minSdk）+ 签名 + SecurityScanner | 闪退原因排查清单 |
| "为什么联网？" | Manifest（INTERNET权限）+ DEX URL 提取 + 追踪 SDK | 网络通信目标列表 |
| "为什么检测 Root？" | DEX 字符串搜索 | Root 检测逻辑分析 |
| "为什么这么大？" | SizeAnalyzer | 各组件体积占比分析 |
| "这个 APP 用了什么框架？" | DEX 类扫描 + Entries 扫描 | 技术栈识别（Flutter/React Native/Unity 等） |
| "有没有后门？" | SecurityScanner + DEX 字符串扫描 | 可疑行为检测报告 |
| "签名是否有效？" | ApkFile.getSignatures | 签名验证报告 |
| "权限是否合理？" | Manifest.permissions + SecurityScanner | 权限合理性评估 |

#### 能力 4：总结（Summarize）

**定义：** 将复杂的分析数据浓缩为简短易懂的结论。

| 用户可能说 | AI 输出 |
|---|---|
| "给我一个总结" | 一段话概括 APK 的整体情况 |
| "权限总结" | 权限列表分类总结（必要的/可疑的/危险的） |
| "安全总结" | 安全评分 + 关键风险点 + 建议 |
| "文件结构总结" | APK 文件结构概览（代码/资源/库/数据占比） |
| "技术栈总结" | 使用的框架、库、SDK 清单 |
| "和上个版本有什么区别？" | 调用 ApkDiff 比较两个版本，总结差异 |

#### 能力 5：修改建议（Modify Guide）

**定义：** 指导用户如何修改 APK 的特定部分。

| 用户可能说 | AI 输出 |
|---|---|
| "怎么修改应用名称？" | "应用名在 resources.arsc 的 string/app_name 中。使用资源编辑器修改即可。" + [打开资源编辑器] |
| "怎么修改包名？" | "包名在 AndroidManifest.xml 的 manifest 标签 package 属性中。注意：修改包名后需要重新签名。" + [打开清单编辑器] |
| "怎么修改版本号？" | "版本号在 AndroidManifest.xml 中。versionName 是显示版本，versionCode 是内部版本号。" + [打开清单编辑器] |
| "怎么去掉广告？" | "广告通过 AdMob SDK 实现。方案：1. 在 DEX 中注释掉广告初始化代码 2. 修改 Manifest 移除广告 Activity 3. 替换广告 SDK 的初始化方法" + [查看广告代码] [打开 DEX] |
| "怎么去掉 Root 检测？" | "Root 检测在 com.example.security.RootChecker 中。方案：修改 checkRoot() 方法返回 false。" + [查看代码] [打开 DEX] |
| "怎么修改图标？" | "应用图标在 res/mipmap-*/ic_launcher.png 中。可以直接替换图片文件。" + [查看文件] |
| "怎么修改权限？" | "权限在 AndroidManifest.xml 的 uses-permission 标签中。删除对应的 uses-permission 即可。" + [打开清单编辑器] |
| "怎么改最小 SDK 版本？" | "minSdk 在 AndroidManifest.xml 的 uses-sdk 标签中。" + [打开清单编辑器] |
| "怎么禁用调试模式？" | "debuggable 属性在 AndroidManifest.xml 的 application 标签中。" + [打开清单编辑器] |
| "怎么修改默认语言？" | "在 resources.arsc 中修改 locale 配置，或添加/删除 values-xx 目录中的字符串资源。" + [打开资源编辑器] |

**重要：** AI 只提供**建议和位置**，不自动执行修改。用户需要自己使用对应工具操作。

#### 能力 6：风险说明（Risk Explain）

**定义：** 解释安全风险的含义和影响。

| 用户可能说 | AI 输出 |
|---|---|
| "硬编码密钥有什么风险？" | "硬编码密钥意味着 API 密钥、密码等敏感信息直接写在代码中。攻击者可以通过反编译提取这些密钥，可能导致服务被盗用。" |
| "debuggable=true 有什么风险？" | "debuggable=true 允许调试器附加到应用进程，攻击者可以利用调试工具查看和修改运行时数据。" |
| "allowBackup=true 有什么风险？" | "allowBackup=true 允许通过 adb backup 备份应用数据，可能导致敏感数据泄露。" |
| "仅 V1 签名有什么风险？" | "V1 签名容易受到 Janus 漏洞攻击，攻击者可以在不破坏签名的情况下修改 APK。" |
| "usesCleartextTraffic=true 有什么风险？" | "允许明文传输，网络通信不加密，可能被中间人攻击拦截。" |
| "导出组件有什么风险？" | "导出组件可以被其他应用调用，如果没有权限保护，可能导致越权访问。" |
| "检测到弱加密算法" | "DES/3DES/RC4/AES-ECB 已被认为不安全，可能被破解。" |
| "检测到追踪 SDK" | "追踪 SDK 会收集设备信息、使用习惯等数据，可能涉及隐私问题。" |

#### 能力 7：学习模式（Learn Mode）

**定义：** 以教学方式回答问题，适合想学习 Android 逆向的用户。

| 用户可能说 | AI 输出 |
|---|---|
| "教我看 Manifest" | "Manifest 是 APK 的'身份证'，记录了应用的基本信息。让我带你逐行看这个 APK 的 Manifest：..." |
| "教我看 Smali" | "Smali 是 DEX 的反汇编语言。每行指令对应一条 Dalvik 字节码。让我用当前 APK 的代码举例：..." |
| "Android 权限体系是怎样的？" | "Android 权限分为普通权限、危险权限和签名权限。让我用当前 APK 的权限列表来说明：..." |
| "APK 的结构是什么？" | "APK 本质是 ZIP 文件。让我列出当前 APK 的文件结构来说明：..." |
| "签名是怎么工作的？" | "Android 签名用于验证 APK 来源和完整性。让我用当前 APK 的签名信息来说明：..." |
| "什么是混淆？" | "代码混淆是防止反编译的措施。让我看看当前 APK 是否使用了混淆：..." |
| "什么是 multidex？" | "当方法数超过 65535 时需要拆分多个 DEX 文件。当前 APK 有 N 个 DEX 文件。" |

#### 能力 8：开发模式（Dev Mode）

**定义：** 面向开发者的辅助功能。

| 用户可能说 | AI 输出 |
|---|---|
| "这个 APP 用了什么架构？" | 分析 DEX 类结构，推测 MVVM/MVP/MVC 架构 |
| "用了什么依赖注入框架？" | 搜索 DEX 中的 Dagger/Hilt/Koin 类 |
| "用了什么网络框架？" | 搜索 DEX 中的 OkHttp/Retrofit/Volley 类 |
| "用了什么数据库框架？" | 搜索 DEX 中的 Room/GreenDAO/Realm 类 |
| "有哪些第三方库？" | 列出 DEX 中识别到的第三方库 |
| "minSdk 和 targetSdk 是多少？" | 从 Manifest 提取并解释 |
| "用了 ProGuard 混淆吗？" | 分析类名是否被混淆 |
| "有没有使用 NDK/JNI？" | 检查 lib/ 目录下的 .so 文件 |
| "列出所有 Activity" | 从 Manifest 提取所有 Activity |
| "列出所有 Service" | 从 Manifest 提取所有 Service |

### 3.2 对话能力矩阵

| 能力 | 需要调用 AppDex 能力 | 需要调用 LLM | 面向普通用户 | 面向中级用户 | 面向专家 |
|---|---|---|---|---|---|
| 解释 | 部分（读取当前 APK 数据时） | 是 | ✅ | ✅ | ✅ |
| 定位 | 是（DEX/Manifest/Security） | 是 | ✅ | ✅ | ✅ |
| 分析 | 是（多引擎综合） | 是 | ✅ | ✅ | ✅ |
| 总结 | 是（读取分析结果） | 是 | ✅ | ✅ | ✅ |
| 修改建议 | 是（定位修改位置） | 是 | ✅ | ✅ | ✅ |
| 风险说明 | 部分（引用安全扫描结果） | 是 | ✅ | ✅ | — |
| 学习模式 | 是（以当前 APK 为例） | 是 | ✅ | — | — |
| 开发模式 | 是（DEX 类分析） | 是 | — | ✅ | ✅ |

### 3.3 对话交互规则

| 规则 | 说明 |
|---|---|
| 语言跟随用户 | 用户用中文问，AI 用中文回答；用户用英文问，AI 用英文回答 |
| 普通模式隐藏术语 | 普通模式下，AI 回复中不出现 DEX/Smali/AXML 等术语，用"代码""配置文件"替代 |
| 高级模式显示术语 | 高级模式下，AI 回复中保留技术术语 |
| 每条回复附带行动建议 | AI 回复后，如果涉及可操作内容，附带 Action Card |
| 不确定时诚实声明 | AI 不确定时明确说"这个分析可能不准确，建议交叉验证" |
| 安全提示 | 涉及修改建议时，附带安全风险提示 |
| 上下文保持 | 对话历史在当前会话内保持，用户可以追问 |
| 上下文不跨 APK | 切换到另一个 APK 时，清除之前的对话历史 |

---

---

## 第四部分：AI 可调用的 AppDex 能力清单

### 4.1 能力总览

AI 可以调用以下 AppDex 底层能力来获取数据和执行分析。这些能力全部由 AppDex 已有引擎提供，AI 只负责调度和解读。

### 4.2 APK 解析能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| 读取 Manifest | ApkFile.getManifest() | 包名、版本名、版本号、minSdk、targetSdk、权限列表、Activity 列表、Service 列表、Receiver 列表、Provider 列表、meta-data | 解答"这个 APP 申请了什么权限""有哪些组件""包名是什么" |
| 读取签名信息 | ApkFile.getSignatures() | 签名版本（V1/V2/V3）、签名算法、证书主题、证书颁发者、序列号、SHA-256/SHA-1/MD5、有效期 | 解答"签名是否有效""证书是谁的""签名安全吗" |
| 读取文件列表 | ApkFile.listEntries() | APK 内所有文件名、大小、压缩后大小、是否目录 | 解答"APK 里有什么文件""文件结构是什么" |
| 读取 APK 大小 | ApkFile.fileSize | APK 总大小 | 体积分析 |
| 读取单个文件 | ApkFile.getEntryInputStream() | 指定文件的输入流 | 读取特定配置文件内容 |

### 4.3 安全扫描能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| 危险权限检测 | SecurityScanner.scanManifest() | 标记的危险权限列表（摄像头、位置、通讯录等） | 解答"有没有危险权限""隐私风险" |
| 硬编码密钥检测 | SecurityScanner.scanHardcodedSecrets() | 检测到的 API Key、密码、Token、URL | 解答"有没有泄露密钥""安全问题" |
| 追踪 SDK 检测 | SecurityScanner.scanTrackingSdk() | 检测到的广告/分析 SDK 列表 | 解答"广告在哪""有没有追踪 SDK" |
| 弱加密检测 | SecurityScanner.scanWeakCrypto() | 检测到的弱加密算法使用 | 解答"加密是否安全" |
| 安全评分 | SecurityScanResult.securityScore | 0-100 分安全评分 | 解答"安全吗""评分多少" |
| 安全问题列表 | SecurityScanResult.issues | 所有问题及严重级别 | 安全风险评估 |

### 4.4 DEX 解析能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| 列出 DEX 文件 | DexRepository.listDexFiles() | DEX 文件名、大小 | 解答"有几个 DEX""是否 multidex" |
| 列出类 | DexRepository.listClasses() | 类名、包名、父类、接口、字段数、方法数 | 解答"有哪些类""类结构""用了什么框架" |
| 反编译 Smali | DexRepository.toSmali() | 指定类的 Smali 代码 | 解答"这段代码做什么""修改哪里" |
| 字符串提取 | SecurityScanner.extractStringsFromDex() | DEX 中的字符串列表 | 解答"有没有特定字符串""URL/密钥在哪" |

### 4.5 资源分析能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| 读取资源表 | ArscEditorRepository | resources.arsc 中的字符串、颜色、尺寸等资源 | 解答"应用名在哪""怎么改名字" |
| 读取二进制 XML | AxmlEditorRepository | AndroidManifest.xml 的可读 XML | 解答"Manifest 具体内容" |
| 浏览 APK 内文件 | ApkFile.getEntryInputStream() | res/ 和 assets/ 目录文件 | 解答"有没有特定资源文件" |

### 4.6 数据库分析能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| 读取 SQLite 数据库 | SqliteViewerRepository | 表结构、表数据 | 解答"本地数据存了什么""会员信息在哪" |

### 4.7 ELF 分析能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| 读取 ELF 文件 | ElfViewerRepository | ELF 文件头、段、节、符号表 | 解答"so 库里有什么""JNI 方法" |

### 4.8 体积分析能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| APK 体积分析 | SizeAnalyzerRepository | 各组件（DEX/资源/库/资产）大小和占比 | 解答"为什么这么大""哪部分最占空间" |

### 4.9 签名能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| 签名验证 | SigningRepository | 签名版本、证书详情、是否有效 | 解答"签名信息""是否被篡改" |
| 重签名 | SigningRepository（执行） | 重签名结果 | 用户决定后执行的入口 |

### 4.10 重打包能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| APK 重打包 | RepackagingRepository（执行） | 重打包结果 | 用户修改后重新打包 APK |

### 4.11 差异分析能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| APK 对比 | ApkDiffRepository | 两个 APK 的差异（文件新增/删除/修改） | 解答"和旧版有什么区别""改了什么" |

### 4.12 哈希计算能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| 计算哈希 | HashCalculatorScreen | MD5/SHA-1/SHA-256 | 验证 APK 完整性 |

### 4.13 文件浏览能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| 浏览 APK 内文件 | ApkFile + FileManager | 文件列表、文件内容 | 定位特定文件 |
| 文本编辑 | EditorScreen | 文件内容编辑 | 修改配置文件 |

### 4.14 终端能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| 执行命令 | TerminalSession | 命令输出 | 高级用户使用（非 AI 调用） |

### 4.15 远程能力

| 能力 | 引擎来源 | AI 获取的数据 | 典型用途 |
|---|---|---|---|
| FTP 客户端 | FtpClientManager | 远程文件操作 | 文件传输 |
| Web 文件服务器 | WebFileServer | HTTP 文件服务 | 在浏览器访问设备文件 |

### 4.16 能力调用权限矩阵

| 能力 | 普通模式 | 高级模式 | 专家模式 |
|---|---|---|---|
| 读取 Manifest | ✅ AI 自动调用 | ✅ | ✅ |
| 读取签名 | ✅ AI 自动调用 | ✅ | ✅ |
| 读取文件列表 | ✅ AI 自动调用 | ✅ | ✅ |
| 安全扫描 | ✅ AI 自动调用 | ✅ | ✅ |
| DEX 类列表 | ✅ AI 按需调用 | ✅ | ✅ |
| DEX Smali 反编译 | ⚠ AI 按需调用，结果转译 | ✅ | ✅ |
| 资源表读取 | ✅ AI 按需调用 | ✅ | ✅ |
| SQLite 读取 | ⚠ AI 按需调用，结果转译 | ✅ | ✅ |
| ELF 读取 | ❌ 不直接展示 | ⚠ 提示存在 | ✅ |
| 体积分析 | ✅ AI 自动调用 | ✅ | ✅ |
| 签名执行 | ❌ 只提供建议 | ⚠ 用户确认后执行 | ✅ |
| 重打包执行 | ❌ 只提供建议 | ⚠ 用户确认后执行 | ✅ |
| APK 对比 | ✅ AI 按需调用 | ✅ | ✅ |
| 哈希计算 | ✅ AI 按需调用 | ✅ | ✅ |
| 终端 | ❌ | ❌ | ✅ |
| FTP / Web 服务器 | ❌ | ⚠ | ✅ |

---

---

## 第五部分：AI 永远不能做什么

### 5.1 绝对禁止（红线）

以下行为无论用户如何要求，AI **永远不能执行**：

| 编号 | 禁止行为 | 原因 |
|---|---|---|
| P-01 | **禁止自动破解** | AppDex 是分析工具，不是破解工具。AI 不会自动修改代码以绕过付费验证 |
| P-02 | **禁止绕过授权** | AI 不会生成或执行绕过 License 验证、激活码验证的自动方案 |
| P-03 | **禁止自动修改收费逻辑** | AI 不会自动修改支付相关的代码逻辑 |
| P-04 | **禁止生成破解 APK** | AI 不会自动执行"修改→重打包→重签名"的完整流程生成破解版 |
| P-05 | **禁止自动注入恶意代码** | AI 不会向 APK 中注入后门、木马等恶意代码 |
| P-06 | **禁止自动修改其他应用** | AI 只分析用户选择的 APK，不会触碰设备上的其他应用 |
| P-07 | **禁止联网下载或上传** | AI 不会将 APK 数据或分析结果上传到任何服务器（LLM API 除外，且由用户配置） |
| P-08 | **禁止自动执行 Shell 命令** | AI 不会自动在终端中执行命令。高级模式下可以建议命令，但用户必须手动执行 |
| P-09 | **禁止访问用户隐私数据** | AI 不会读取用户通讯录、短信、相册等设备隐私数据 |
| P-10 | **禁止修改系统文件** | AI 不会修改 /system 分区或需要 root 权限的系统文件 |

### 5.2 允许但有限制（黄线）

以下行为 AI **可以执行，但有明确限制**：

| 编号 | 允许行为 | 限制条件 |
|---|---|---|
| Y-01 | **解释代码逻辑** | AI 可以解释任意代码的作用，但不提供绕过方案 |
| Y-02 | **分析安全风险** | AI 可以指出安全漏洞和风险，给出修复建议 |
| Y-03 | **定位修改位置** | AI 可以告诉用户"在哪里修改什么"，但不自动修改 |
| Y-04 | **生成 Patch 建议** | AI 可以生成 Smali/Java/Kotlin/资源的修改建议代码片段，但用户需要手动应用 |
| Y-05 | **生成 Smali 修改建议** | AI 可以建议"将 const/4 v0, 0x1 修改为 const/4 v0, 0x0"，但不自动执行 |
| Y-06 | **生成 Java 修改建议** | AI 可以建议等价的 Java 代码修改 |
| Y-07 | **生成 Kotlin 修改建议** | AI 可以建议等价的 Kotlin 代码修改 |
| Y-08 | **生成资源修改建议** | AI 可以建议修改 resources.arsc 中的哪个资源的哪个值 |
| Y-09 | **提供操作入口** | AI 可以在回复中附带 Action Card，让用户一键跳转到对应工具 |
| Y-10 | **生成排查清单** | AI 可以生成"闪退排查清单""安全加固建议"等 |

### 5.3 边界示例

#### 示例 1：用户问"怎么破解会员"

| AI 回答 | 是否允许 |
|---|---|
| "我可以帮你定位会员验证逻辑的位置。" | ✅ 允许（定位） |
| "会员验证在 com.example.billing.VipChecker.verify() 方法中。" | ✅ 允许（定位） |
| "这段代码的逻辑是：从服务器获取 VIP 状态，如果返回 true 则解锁会员功能。" | ✅ 允许（解释） |
| "如果你想修改，可以将 verify() 方法的返回值改为 true。" | ⚠ 允许（修改建议），但附带提示："此修改仅供学习研究，请勿用于非法用途" |
| "我已经帮你修改了代码并重新打包了 APK。" | ❌ 禁止（自动修改+重打包） |
| "你可以用这个修改后的 APK 免费使用会员功能。" | ❌ 禁止（生成破解 APK） |

#### 示例 2：用户问"怎么去掉广告"

| AI 回答 | 是否允许 |
|---|---|
| "广告通过 AdMob SDK 实现，初始化代码在 com.example.AdManager.init() 中。" | ✅ 允许（定位） |
| "方案 1：在 Smali 中将 init() 方法改为空实现。方案 2：在 Manifest 中移除 AdActivity。" | ✅ 允许（修改建议） |
| [打开 DEX] [打开 Manifest 编辑器] | ✅ 允许（提供操作入口） |
| "我已经帮你去掉了广告代码。" | ❌ 禁止（自动修改） |

#### 示例 3：用户问"这个 APP 有后门吗"

| AI 回回答 | 是否允许 |
|---|---|
| "我扫描了 DEX 中的字符串和类结构，没有发现明显的后门代码。" | ✅ 允许（分析） |
| "但是检测到以下可疑行为：1. 向 192.168.x.x 发送数据 2. 运行时加载外部 DEX" | ✅ 允许（风险说明） |
| "[查看安全报告] [查看可疑 URL]" | ✅ 允许（提供操作入口） |
| "建议不要使用这个应用。" | ✅ 允许（建议） |

### 5.4 安全提示规则

| 场景 | 提示 |
|---|---|
| 涉及修改代码时 | "⚠ 修改 APK 代码可能违反应用的使用协议，请确保在合法授权下进行。" |
| 涉及绕过验证时 | "⚠ 绕过付费验证可能违反法律法规，本建议仅供安全研究。" |
| 涉及重打包时 | "⚠ 修改后的 APK 需要重新签名才能安装。重签名后的 APK 与原始签名不同。" |
| 涉及安全风险时 | "⚠ 此 APK 存在高危安全风险，建议谨慎使用。" |
| AI 不确定时 | "ℹ 此分析基于 AI 推理，结果可能不准确，建议交叉验证。" |
| 涉及硬编码密钥时 | "⚠ 此 APK 中检测到硬编码密钥，这意味着敏感信息可能已泄露。" |

---

---

## 第六部分：三级模式设计

### 6.1 设计理念

AppDex 2.0 的用户群体跨度极大：从完全不懂技术的普通用户，到资深 Android 逆向工程师。一套界面无法同时满足所有用户。

三级模式设计的核心理念是**渐进式暴露**：

- **普通模式**：完全隐藏技术。用户看到的是 AI 对话和安全评分，不需要理解任何术语。
- **高级模式**：逐渐展示工具。用户可以在 AI 引导下使用 DEX Browser、Manifest 编辑器等。
- **专家模式**：全部开放。所有工具直接可用，术语完整保留，AI 可选。

### 6.2 模式对比

| 维度 | 普通模式 | 高级模式 | 专家模式 |
|---|---|---|---|
| **目标用户** | 不懂技术的普通用户 | 有一定基础的爱好者 | 专业逆向工程师 |
| **AI 角色** | 必须使用，是唯一入口 | 主力入口，工具辅助 | 可选辅助，工具为主 |
| **术语暴露** | 完全隐藏（DEX→"代码"，Manifest→"配置"） | 逐步暴露，有解释 | 全部保留 |
| **工具入口** | 通过 AI 回复中的 Action Card 跳转 | 工具页可见，AI 引导使用 | 工具页全部直接可用 |
| **操作权限** | 只能查看，不能修改 | 需确认后修改 | 直接修改 |
| **安全提示** | 每次操作都提示 | 重要操作提示 | 不提示 |
| **默认模式** | ✅ AppDex 默认 | 需手动切换 | 需手动切换 |
| **AI 回复风格** | 通俗语言，类比解释 | 半技术语言 | 技术语言 |

### 6.3 普通模式（Normal Mode）

**定位：** 面向完全不懂技术的用户。用户只看到 AI 对话和评分。

**UI 特征：**
- 首页：AI 欢迎卡 + 最近任务
- 任务页：安全评分卡 + AI 总结卡 + AI 对话框 + Action Card
- 工具页：不显示工具列表，显示"需要更多功能？切换高级模式"
- 设置页：AI 配置 + 基础设置

**术语替换表：**

| 原始术语 | 普通模式替换 |
|---|---|
| DEX / Smali | 代码 |
| Manifest / AndroidManifest.xml | 配置文件 |
| resources.arsc | 资源文件 |
| 签名 V1/V2/V3 | 应用签名 |
| exported Activity | 可被外部调用的功能 |
| debuggable | 调试模式 |
| allowBackup | 数据备份 |
| usesCleartextTraffic | 明文传输 |
| APK Signature Scheme | 签名方案 |
| ELF / .so | 原生库 |
| SQLite | 本地数据库 |
| permissions | 权限 |
| ProGuard / 混淆 | 代码混淆 |
| multidex | 多代码文件 |
| JNI / NDK | 原生接口 |

**AI 回复风格示例：**

```
用户：这个 APP 安全吗？

AI（普通模式）：
"整体安全 ✅

这个应用是安全的。它申请了一些必要的权限（比如相机用于拍照），
没有发现恶意行为。

不过它集成了 3 个广告追踪组件，会收集一些使用数据。

评分：85/100 低风险"
```

**操作入口：** 只通过 AI 回复中的 Action Card 提供。不显示原始工具入口。

### 6.4 高级模式（Advanced Mode）

**定位：** 面向有一定基础的爱好者。AI 引导使用工具，但工具入口可见。

**UI 特征：**
- 首页：AI 欢迎卡 + 最近任务 + 快捷工具入口
- 任务页：安全评分卡 + AI 总结卡 + AI 对话框 + Action Card + 工具入口区（折叠展开）
- 工具页：分三类显示所有工具
- 设置页：AI 配置 + 完整设置

**术语策略：** 保留技术术语，但 AI 解释时附带通俗说明。

**AI 回复风格示例：**

```
用户：这个 APP 安全吗？

AI（高级模式）：
"安全评分：85/100 低风险

安全分析：
✓ 签名有效（V2+V3，SHA-256withRSA）
✓ 无硬编码密钥
⚠ 5 个危险权限（CAMERA, ACCESS_FINE_LOCATION, ...）
⚠ 集成 3 个追踪 SDK（AdMob, Firebase Analytics, Umeng）
✓ 无弱加密算法

Manifest 分析：
- minSdk: 24 (Android 7.0)
- targetSdk: 35 (Android 15)
- 12 Activities, 5 Services, 3 Receivers

[查看安全报告] [查看 Manifest] [查看 DEX]"
```

**工具入口：** 工具页分类展示，每类可展开。AI 回复中附带 Action Card。

### 6.5 专家模式（Expert Mode）

**定位：** 面向专业逆向工程师。所有工具直接可用，AI 是可选辅助。

**UI 特征：**
- 首页：最近任务 + 快捷工具网格 + 终端入口
- 任务页：完整的 Tab 布局（概览/Manifest/DEX/资源/签名/文件/安全/HEX/ELF/SQLite）
- 工具页：全部工具平铺，无折叠
- 设置页：完整设置 + AI 高级配置（温度/Top-P/系统 Prompt 自定义）
- 终端：可直接使用

**术语策略：** 完整保留所有术语，无解释。

**AI 回复风格示例：**

```
用户：分析一下安全状况

AI（专家模式）：
"SecurityScore: 85/100 (LOW RISK)

Signatures:
  V2: SHA256withRSA, valid until 2049-01-01
  V3: SHA256withRSA, key rotation supported
  Cert Subject: CN=Example, OU=Dev, O=Example Inc.

Dangerous Permissions (5):
  android.permission.CAMERA
  android.permission.ACCESS_FINE_LOCATION
  android.permission.READ_CONTACTS
  android.permission.RECORD_AUDIO
  android.permission.READ_EXTERNAL_STORAGE

Tracking SDKs (3):
  Google AdMob (com.google.android.gms.ads)
  Firebase Analytics (com.google.firebase.analytics)
  Umeng Analytics (com.umeng)

DEX: 3 files (classes.dex 2.3MB, classes2.dex 1.1MB, classes3.dex 0.8MB)
Total classes: 8,432
Obfuscated: Yes (a.b.c pattern detected)

Issues: 2 MEDIUM, 3 LOW
  MEDIUM: DANGEROUS_PERMISSION x5
  LOW: EXPORTED_COMPONENT x12
"
```

**工具入口：** 所有工具直接可用，无需 AI 引导。

### 6.6 模式切换

| 切换方式 | 说明 |
|---|---|
| 入口 | 设置页 → "分析模式" → 普通/高级/专家 |
| 全局生效 | 切换模式后，所有页面立即更新 |
| 首次引导 | 首次使用时引导选择模式："你是？（普通用户 / 开发者 / 专家）" |
| 记住选择 | 模式选择持久化，下次打开保持 |
| 随时切换 | 任何页面都可以通过设置切换模式 |
- AI 回复风格影响 AI 的 system prompt，不同模式使用不同的系统提示词

### 6.7 模式 × 页面矩阵

| 页面 | 普通模式 | 高级模式 | 专家模式 |
|---|---|---|---|
| 首页 | AI 欢迎卡 + 最近任务 | AI 欢迎卡 + 最近任务 + 快捷工具 | 最近任务 + 工具网格 + 终端 |
| 任务页 | 评分 + AI 总结 + 对话 | 评分 + AI + 折叠工具区 | 完整 Tab（概览/Manifest/DEX/资源/签名/文件/安全/HEX/ELF/SQLite） |
| 工具页 | "切换高级模式"提示 | 三分类折叠 | 全部平铺 |
| 设置页 | AI 配置 + 基础 | AI 配置 + 完整 | AI 配置 + 高级（温度/Prompt）+ 完整 |
| AI 对话 | 默认显示 | 默认显示 | 可隐藏 |
| 终端 | 隐藏 | 隐藏 | 可用 |
| HEX 编辑器 | 隐藏 | 折叠 | 可用 |
| ELF 查看器 | 隐藏 | 折叠 | 可用 |

---

---

## 第七部分：AI 驱动 UI 的交互设计

### 7.1 设计理念：从"用户找工具"到"AI 引导用户"

**1.0 时代（传统模式）：**

```
用户想看代码
  → 找到 DEX Browser 入口
  → 打开 DEX Browser
  → 选择 APK
  → 选择 DEX 文件
  → 浏览类列表
  → 找到目标类
  → 点击查看 Smali
```

用户需要自己知道"我要看代码"→"用 DEX Browser"→"在哪个 DEX"→"哪个类"。

**2.0 时代（AI 驱动）：**

```
用户：广告在哪？

AI："广告通过 AdMob SDK 实现。初始化代码在
     com.example.AdManager.init() 方法中。
     我已经帮你定位到了。"

  ┌─────────────────────────┐
  │  [📖 查看代码]            │  ← Action Card，一键跳转
  │  [📝 修改广告代码]         │  ← Action Card，一键跳转
  │  [📄 查看 Manifest]       │  ← Action Card，一键跳转
  └─────────────────────────┘

用户点击 [查看代码]
  → 自动跳转到 DEX Browser
  → 自动定位到 com.example.AdManager 类
  → 自动展示 Smali 代码
  → 顶部显示 AI 提示："这就是广告初始化的代码"
```

AI 做了所有的"定位"工作，用户只需要"决定"是否查看。

### 7.2 AI 驱动的界面层级

```
┌─────────────────────────────────────────────────┐
│ Layer 1: AI 对话层（最顶层）                      │
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │ AI 对话框                                   │  │
│  │ 用户与 AI 的自然语言交互                      │  │
│  │ AI 回复中嵌入 Action Card                    │  │
│  └───────────────────────────────────────────┘  │
│                    │                            │
│                    ▼                            │
│ Layer 2: Action Card 层                         │
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │ [查看代码] [修改资源] [查看安全报告] [签名]    │  │
│  │                                             │  │
│  │ AI 根据对话内容动态生成操作按钮                 │  │
│  └───────────────────────────────────────────┘  │
│                    │                            │
│                    ▼                            │
│ Layer 3: 工具层（底层）                          │
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │ DEX Browser  Manifest  Editor  Security ... │  │
│  │ 所有 AppDex 原始工具                          │  │
│  │ Action Card 点击后跳转到对应工具               │  │
│  │ 自动传递上下文（apkPath/className/entryName） │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### 7.3 AI 驱动场景设计

#### 场景 1：AI 定位到代码

```
AI 回复：
"我已经帮你定位到了登录逻辑。这段代码在
 LoginActivity.java 的 onCreate() 方法中。

 它做了以下事情：
 1. 读取用户输入的账号密码
 2. 发送到 https://api.example.com/login 验证
 3. 如果成功，保存 Token 到本地数据库

 ⚠ 注意：密码以明文传输（usesCleartextTraffic=true）"

┌─────────────────────────────────────────┐
│  Action Cards:                          │
│  [📖 打开代码]    → 跳转 DEX Browser     │
│  [🔐 查看网络请求] → 跳转 DEX URL 提取   │
│  [💾 查看本地数据库] → 跳转 SQLite Viewer │
│  [⚠ 查看安全报告]  → 跳转 Security Scanner│
└─────────────────────────────────────────┘
```

#### 场景 2：AI 定位到资源

```
AI 回复：
"你想修改应用名称。应用名存储在资源文件中，
 对应资源 ID: 0x7f100001，名称: app_name。
 当前值: '示例应用'。

 你可以直接在资源编辑器中修改这个值。"

┌─────────────────────────────────────────┐
│  Action Cards:                          │
│  [✏ 打开资源编辑器] → 跳转 ARSC Editor   │
│  [📱 查看 Manifest] → 跳转 AXML Editor   │
│  [📁 查看资源文件]  → 跳转 FileManager   │
└─────────────────────────────────────────┘
```

#### 场景 3：AI 建议修改方案

```
AI 回复：
"你要去掉 Root 检测。Root 检测代码在
 com.example.security.RootChecker.checkRoot() 中。

 Smali 修改建议：
 ┌──────────────────────────────────────┐
 │ .method public checkRoot()Z          │
 │   # 原始代码：检查 su 命令是否存在      │
 │   # const/4 v0, 0x0                  │
 │   # invoke-static ..., checkSu()     │
 │   # move-result v0                   │
 │                                      │
 │   # 修改为：直接返回 false             │
 │   const/4 v0, 0x0                    │
 │   return v0                          │
 │ .end method                          │
 └──────────────────────────────────────┘

 ⚠ 修改后需要重新打包和签名。"

┌─────────────────────────────────────────┐
│  Action Cards:                          │
│  [📖 查看代码]     → 跳转 DEX Browser    │
│  [🔄 开始重打包]   → 跳转 Repackage      │
│  [🔏 重新签名]     → 跳转 Signing        │
└─────────────────────────────────────────┘
```

#### 场景 4：AI 综合安全分析

```
AI 回复：
"安全分析报告：

 评分：72/100 中等风险

 🔴 严重 (1):
   - 硬编码 API 密钥 (AIza***...在 classes.dex 中)

 🟠 高危 (2):
   - 仅使用 V1 签名（Janus 漏洞风险）
   - debuggable=true（可被调试）

 🟡 中危 (5):
   - 5 个危险权限（CAMERA, LOCATION, ...）
   - 弱加密算法 (DES)

 🔵 低危 (3):
   - 3 个导出组件未设置权限保护
   - allowBackup=true

 建议：
 1. 移除硬编码密钥，使用 Android Keystore
 2. 升级到 V2+V3 签名
 3. 设置 debuggable=false
 4. 评估危险权限是否必要"

┌─────────────────────────────────────────┐
│  Action Cards:                          │
│  [🔒 查看完整安全报告] → Security Scanner │
│  [📕 查看硬编码密钥]  → DEX 字符串搜索    │
│  [🔏 查看签名详情]    → Signing Screen    │
│  [📄 查看 Manifest]   → AXML Editor      │
│  [🔧 开始修改]       → 工具列表          │
└─────────────────────────────────────────┘
```

### 7.4 AI 驱动的页面跳转规则

| AI 场景 | 跳转目标 | 自动传递的参数 |
|---|---|---|
| "打开代码" | DEX Browser | apkPath, dexName, className |
| "查看 Manifest" | AXML Editor | apkPath, entryName="AndroidManifest.xml" |
| "修改资源" | ARSC Editor | apkPath, resourceId |
| "查看安全报告" | Security Scanner | apkPath |
| "查看签名" | Signing Screen | apkPath |
| "开始重打包" | Repackage Screen | apkPath |
| "查看文件" | File Manager (APK 内浏览) | apkPath |
| "查看数据库" | SQLite Viewer | dbPath |
| "查看 ELF" | ELF Viewer | filePath (so 文件路径) |
| "查看 HEX" | HEX Editor | filePath |
| "对比版本" | APK Diff | apkPath (当前), (选择对比 APK) |
| "体积分析" | Size Analyzer | apkPath |
| "计算哈希" | Hash Calculator | apkPath |
| "使用终端" | Terminal | — |
| "编辑文本" | Editor | filePath |

### 7.5 AI 回复的结构化格式

AI 的每次回复遵循以下结构：

```
┌─────────────────────────────────────────────────┐
│ 1. 结论/回答（必须）                              │
│    用自然语言直接回答用户问题                       │
│                                                 │
│ 2. 分析依据（可选）                               │
│    列出数据来源和分析过程                          │
│                                                 │
│ 3. 风险/注意事项（可选）                          │
│    安全提示或不确定声明                            │
│                                                 │
│ 4. Action Cards（可选）                          │
│    相关操作入口                                   │
│                                                 │
│ 5. 推荐问题（可选）                               │
│    推荐用户继续追问的问题                          │
└─────────────────────────────────────────────────┘
```

---

---

## 第八部分：Action Card 设计

### 8.1 Action Card 概念

Action Card 是 AI 回复中的操作入口按钮。每个 Action Card 对应一个 AppDex 工具或功能。用户点击后自动跳转到对应工具，并传递必要的上下文参数。

**Action Card 设计原则：**
1. **动态生成** — AI 根据对话内容决定显示哪些 Action Card
2. **上下文感知** — 跳转时自动传递 APK 路径、类名、文件名等参数
3. **不超过 4 个** — 单次回复的 Action Card 不超过 4 个，避免信息过载
4. **简洁标签** — 按钮文字简洁明了，普通模式不用术语
5. **分级显示** — 普通模式只显示用户能理解的操作，高级/专家模式显示更多

### 8.2 Action Card 完整清单（55 个）

#### 分类 A：代码查看类（10 个）

| 编号 | Action Card | 普通模式标签 | 高级模式标签 | 跳转目标 | 传递参数 |
|---|---|---|---|---|---|
| A-01 | 打开 DEX 浏览 | 查看代码 | 打开 DEX Browser | DexBrowserScreen | apkPath |
| A-02 | 定位到类 | 查看这个类 | 定位 Class | DexBrowserScreen | apkPath, dexName, className |
| A-03 | 查看 Smali 代码 | 查看代码详情 | 查看 Smali | DexBrowserScreen | apkPath, dexName, className, showSmali=true |
| A-04 | 搜索 DEX 字符串 | 搜索代码内容 | DEX String Search | DexBrowserScreen | apkPath, searchString |
| A-05 | 查看 DEX 文件列表 | 查看代码文件 | List DEX Files | DexBrowserScreen | apkPath |
| A-06 | 查看所有类 | 浏览所有类 | List All Classes | DexBrowserScreen | apkPath, listMode=true |
| A-07 | 查看类的继承关系 | 查看类结构 | Class Hierarchy | DexBrowserScreen | apkPath, className, showHierarchy=true |
| A-08 | 查看方法列表 | 查看方法 | List Methods | DexBrowserScreen | apkPath, className, showMethods=true |
| A-09 | 查看字段列表 | 查看属性 | List Fields | DexBrowserScreen | apkPath, className, showFields=true |
| A-10 | 导出 Smali | 保存代码 | Export Smali | DexBrowserScreen | apkPath, dexName, className, export=true |

#### 分类 B：配置/清单类（8 个）

| 编号 | Action Card | 普通模式标签 | 高级模式标签 | 跳转目标 | 传递参数 |
|---|---|---|---|---|---|
| B-01 | 打开 Manifest | 查看配置 | 打开 AndroidManifest | AxmlEditorScreen | apkPath, entryName="AndroidManifest.xml" |
| B-02 | 查看权限列表 | 查看权限 | View Permissions | AxmlEditorScreen | apkPath, filter="uses-permission" |
| B-03 | 查看 Activity 列表 | 查看页面 | List Activities | AxmlEditorScreen | apkPath, filter="activity" |
| B-04 | 查看 Service 列表 | 查看后台服务 | List Services | AxmlEditorScreen | apkPath, filter="service" |
| B-05 | 查看 Receiver 列表 | 查看接收器 | List Receivers | AxmlEditorScreen | apkPath, filter="receiver" |
| B-06 | 查看 Provider 列表 | 查看数据提供 | List Providers | AxmlEditorScreen | apkPath, filter="provider" |
| B-07 | 修改 Manifest | 修改配置 | Edit Manifest | AxmlEditorScreen | apkPath, editMode=true |
| B-08 | 查看 meta-data | 查看额外配置 | View meta-data | AxmlEditorScreen | apkPath, filter="meta-data" |

#### 分类 C：资源类（6 个）

| 编号 | Action Card | 普通模式标签 | 高级模式标签 | 跳转目标 | 传递参数 |
|---|---|---|---|---|---|
| C-01 | 打开资源编辑器 | 修改资源 | ARSC Editor | ArscEditorScreen | apkPath |
| C-02 | 查找字符串资源 | 查找文字 | Find String Resource | ArscEditorScreen | apkPath, searchType="string" |
| C-03 | 修改应用名称 | 改名字 | Edit app_name | ArscEditorScreen | apkPath, resourceId="app_name" |
| C-04 | 修改应用图标 | 换图标 | Replace app icon | FileManagerScreen | apkPath, filter="mipmap" |
| C-05 | 查看资源文件 | 查看素材 | Browse Resources | FileManagerScreen | apkPath, filter="res/" |
| C-06 | 查看 assets | 查看内置文件 | Browse assets | FileManagerScreen | apkPath, filter="assets/" |

#### 分类 D：安全类（8 个）

| 编号 | Action Card | 普通模式标签 | 高级模式标签 | 跳转目标 | 传递参数 |
|---|---|---|---|---|---|
| D-01 | 查看安全报告 | 查看安全详情 | Security Report | SecurityScannerScreen | apkPath |
| D-02 | 查看危险权限 | 查看风险权限 | Dangerous Permissions | SecurityScannerScreen | apkPath, filter="DANGEROUS_PERMISSION" |
| D-03 | 查看硬编码密钥 | 查看泄露密钥 | Hardcoded Secrets | SecurityScannerScreen | apkPath, filter="HARDCODED" |
| D-04 | 查看追踪 SDK | 查看广告组件 | Tracking SDKs | SecurityScannerScreen | apkPath, filter="TRACKING_SDK" |
| D-05 | 查看弱加密 | 查看加密问题 | Weak Crypto | SecurityScannerScreen | apkPath, filter="WEAK_CRYPTO" |
| D-06 | 查看签名问题 | 查看签名问题 | Signature Issues | SecurityScannerScreen | apkPath, filter="SIGNATURE" |
| D-07 | 查看导出组件 | 查看安全风险 | Exported Components | SecurityScannerScreen | apkPath, filter="EXPORTED" |
| D-08 | 完整安全扫描 | 开始安全扫描 | Full Security Scan | SecurityScannerScreen | apkPath, runScan=true |

#### 分类 E：签名/打包类（6 个）

| 编号 | Action Card | 普通模式标签 | 高级模式标签 | 跳转目标 | 传递参数 |
|---|---|---|---|---|---|
| E-01 | 查看签名详情 | 查看签名 | View Signatures | SigningScreen | apkPath |
| E-02 | 验证签名 | 验证签名 | Verify Signature | SigningScreen | apkPath, verify=true |
| E-03 | 重新签名 | 重新签名 | Re-sign APK | SigningScreen | apkPath, signMode=true |
| E-04 | 开始重打包 | 重新打包 | Repackage | RepackagingScreen | apkPath |
| E-05 | APK 对比 | 对比版本 | APK Diff | ApkDiffScreen | apkPath (current) |
| E-06 | 体积分析 | 查看大小 | Size Analysis | SizeAnalyzerScreen | apkPath |

#### 分类 F：数据/存储类（5 个）

| 编号 | Action Card | 普通模式标签 | 高级模式标签 | 跳转目标 | 传递参数 |
|---|---|---|---|---|---|
| F-01 | 查看数据库 | 查看本地数据 | SQLite Viewer | SqliteViewerScreen | dbPath |
| F-02 | 查看 ELF | 查看原生库 | ELF Viewer | ElfViewerScreen | filePath (so 文件) |
| F-03 | HEX 编辑 | 十六进制编辑 | HEX Editor | HexEditorScreen | filePath |
| F-04 | 文件浏览 | 查看文件 | File Manager | FileManagerScreen | apkPath 或 directoryPath |
| F-05 | 文本编辑 | 编辑文件 | Editor | EditorScreen | filePath |

#### 分类 G：工具类（7 个）

| 编号 | Action Card | 普通模式标签 | 高级模式标签 | 跳转目标 | 传递参数 |
|---|---|---|---|---|---|
| G-01 | 计算哈希 | 查看指纹 | Hash Calculator | HashCalculatorScreen | apkPath |
| G-02 | 编码转换 | 转换编码 | Encoding Converter | EncodingConverterScreen | — |
| G-03 | 设备信息 | 查看设备 | Device Info | DeviceInfoScreen | — |
| G-04 | 终端 | 打开终端 | Terminal | TerminalScreen | — |
| G-05 | FTP 客户端 | 远程文件 | FTP Client | FtpClientScreen | — |
| G-06 | Web 服务器 | 文件分享 | Web Server | WebServerScreen | — |
| G-07 | 插件中心 | 更多工具 | Plugins | PluginListScreen | — |

#### 分类 H：AI 辅助类（5 个）

| 编号 | Action Card | 所有模式标签 | 跳转目标 | 传递参数 |
|---|---|---|---|---|
| H-01 | 询问 AI 更多 | 问 AI 更多问题 | AI 对话框（展开） | context=currentQuestion |
| H-02 | AI 深度分析 | AI 深度分析 | AI 对话框（自动发送预设问题） | presetQuestion |
| H-03 | 配置 AI | 配置 AI 助手 | SettingsScreen（AI 配置区） | — |
| H-04 | 切换模式 | 切换分析模式 | SettingsScreen（模式选择） | — |
| H-05 | 分享报告 | 分析报告 | 系统分享（导出文本） | reportText |

### 8.3 Action Card 显示规则

| 规则 | 说明 |
|---|---|
| 相关性 | Action Card 必须与当前对话内容相关 |
| 最多 4 个 | 单条 AI 回复最多附带 4 个 Action Card |
| 优先级 | 主要操作 > 次要操作 > 查看类操作 > 工具类操作 |
| 模式过滤 | 普通模式不显示 HEX/ELF/Terminal 等高级操作 |
| 上下文传递 | 跳转时自动传递必要参数（apkPath/className/entryName 等） |
| 返回提示 | 从 Action Card 跳转后，工具页顶部显示"← 返回 AI 对话"按钮 |

### 8.4 Action Card 视觉设计

```
┌─────────────────────────────┐
│  📖  查看代码                │  ← 图标 + 标签
│      在 DEX Browser 中打开    │  ← 副标题（高级模式显示）
└─────────────────────────────┘

普通模式：
┌──────────┐ ┌──────────┐
│ 📖 查看代码│ │ 🔒 安全报告│
└──────────┘ └──────────┘

高级模式：
┌────────────────┐ ┌────────────────┐
│ 📖 打开 DEX     │ │ 🔒 Security    │
│    Browser     │ │    Report      │
└────────────────┘ └────────────────┘

专家模式：
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│DEX Browser│ │Security  │ │Signing   │ │Repackage │
└──────────┘ └──────────┘ └──────────┘ └──────────┘
```

---

---

## 第九部分：Prompt Library

### 9.1 设计理念

Prompt Library 是 AppDex AI 内置的推荐问题库。用户不一定知道可以问什么，Prompt Library 提供"灵感"。

**设计原则：**
1. **用普通用户的语言** — 问题用日常语言表述，不用技术术语
2. **分类清晰** — 按使用场景分类
3. **一键发送** — 点击即自动发送给 AI
4. **上下文感知** — 根据当前 APK 状态推荐相关问题
5. **100 个问题** — 覆盖常见场景

### 9.2 分类与问题清单

#### 分类 1：安全（20 个）

| 编号 | 问题 |
|---|---|
| S-01 | 这个 APP 安全吗？ |
| S-02 | 这个 APP 会偷隐私吗？ |
| S-03 | 有没有后门？ |
| S-04 | 有没有恶意行为？ |
| S-05 | 有没有硬编码密钥？ |
| S-06 | 权限是否合理？ |
| S-07 | 签名是否有效？ |
| S-08 | 有没有漏洞？ |
| S-09 | 数据传输是否加密？ |
| S-10 | 会不会泄露通讯录？ |
| S-11 | 会不会泄露位置信息？ |
| S-12 | 会不会偷偷录音？ |
| S-13 | 会不会偷偷拍照？ |
| S-14 | 会不会偷读短信？ |
| S-15 | 有没有追踪 SDK？ |
| S-16 | 广告 SDK 有哪些？ |
| S-17 | 有没有不安全的加密？ |
| S-18 | debuggable 是什么？是否开启？ |
| S-19 | allowBackup 是什么？是否开启？ |
| S-20 | 导出的组件有哪些安全风险？ |

#### 分类 2：逆向（15 个）

| 编号 | 问题 |
|---|---|
| R-01 | 登录逻辑在哪？ |
| R-02 | 会员验证逻辑在哪？ |
| R-03 | Root 检测在哪？ |
| R-04 | 网络请求在哪？ |
| R-05 | 加密逻辑在哪？ |
| R-06 | 数据存储在哪？ |
| R-07 | 广告代码在哪？ |
| R-08 | 有哪些 URL？ |
| R-09 | 有没有混淆？ |
| R-10 | 用的什么混淆？ |
| R-11 | 有没有反调试？ |
| R-12 | 有没有签名校验？ |
| R-13 | 有没有模拟器检测？ |
| R-14 | 本地数据存在哪？ |
| R-15 | 配置文件在哪？ |

#### 分类 3：学习（15 个）

| 编号 | 问题 |
|---|---|
| L-01 | 教我看 Manifest |
| L-02 | 教我看 Smali 代码 |
| L-03 | Android 权限体系是怎样的？ |
| L-04 | APK 的结构是什么？ |
| L-05 | 签名是怎么工作的？ |
| L-06 | 什么是混淆？ |
| L-07 | 什么是 multidex？ |
| L-08 | 什么是 exported Activity？ |
| L-09 | 什么是 resources.arsc？ |
| L-10 | 什么是 JNI/NDK？ |
| L-11 | 什么是 V1/V2/V3 签名？ |
| L-12 | 这个 APP 用了什么架构？ |
| L-13 | Activity 的生命周期是怎样的？ |
| L-14 | 什么是 ContentProvider？ |
| L-15 | 什么是 BroadcastReceiver？ |

#### 分类 4：修改（15 个）

| 编号 | 问题 |
|---|---|
| M-01 | 怎么修改应用名称？ |
| M-02 | 怎么修改包名？ |
| M-03 | 怎么修改版本号？ |
| M-04 | 怎么修改应用图标？ |
| M-05 | 怎么去掉广告？ |
| M-06 | 怎么去掉 Root 检测？ |
| M-07 | 怎么修改权限？ |
| M-08 | 怎么禁用调试模式？ |
| M-09 | 怎么禁用备份？ |
| M-10 | 怎么修改最小 SDK 版本？ |
| M-11 | 怎么修改默认语言？ |
| M-12 | 怎么修改字符串内容？ |
| M-13 | 怎么修改颜色资源？ |
| M-14 | 怎么禁用某个 Activity？ |
| M-15 | 修改后怎么重新打包和签名？ |

#### 分类 5：开发（15 个）

| 编号 | 问题 |
|---|---|
| D-01 | 这个 APP 用了什么网络框架？ |
| D-02 | 用了什么数据库框架？ |
| D-03 | 有哪些第三方库？ |
| D-04 | 用了什么依赖注入框架？ |
| D-05 | 有没有用 NDK/JNI？ |
| D-06 | minSdk 和 targetSdk 是多少？ |
| D-07 | 列出所有 Activity |
| D-08 | 列出所有 Service |
| D-09 | 有几个 DEX 文件？ |
| D-10 | 总共有多少个类？ |
| D-11 | 有没有用 Flutter/React Native？ |
| D-12 | 有没有用 Unity？ |
| D-13 | 有没有热修复框架？ |
| D-14 | 有没有用插件化框架？ |
| D-15 | 这个 APP 的架构分析 |

#### 分类 6：分析（10 个）

| 编号 | 问题 |
|---|---|
| A-01 | 为什么打不开/闪退？ |
| A-02 | 为什么联网？ |
| A-03 | 为什么这么大？ |
| A-04 | 和上个版本有什么区别？ |
| A-05 | 技术栈是什么？ |
| A-06 | 文件结构是什么？ |
| A-07 | 有哪些组件？ |
| A-08 | 安全评分是多少？ |
| A-09 | 给我一个完整总结 |
| A-10 | 这个 APP 是做什么的？ |

#### 分类 7：杂项（10 个）

| 编号 | 问题 |
|---|---|
| X-01 | 这个 APK 的 SHA-256 是多少？ |
| X-02 | 这个 APK 的大小是多少？ |
| X-03 | APK 里有多少个文件？ |
| X-04 | 谁签名的？ |
| X-05 | 证书有效期到什么时候？ |
| X-06 | 有没有 .so 文件？ |
| X-07 | 有没有 assets 目录？ |
| X-08 | 有没有 assets 下的配置文件？ |
| X-09 | 这个 APP 是原版吗？还是被修改过？ |
| X-10 | 帮我导出分析报告 |

### 9.3 上下文感知推荐

AppDex AI 根据当前 APK 的状态，智能推荐相关问题：

| 状态 | 推荐问题 |
|---|---|
| 刚完成分析 | S-01（安全吗）, A-09（总结）, A-10（做什么的）, D-15（架构分析） |
| 检测到广告 SDK | S-16（广告 SDK）, R-07（广告代码）, M-05（去掉广告） |
| 检测到危险权限 | S-06（权限合理吗）, S-10~S-14（隐私问题）, M-07（修改权限） |
| 检测到硬编码密钥 | S-05（硬编码密钥）, S-08（有没有漏洞） |
| 检测到 Root 检测 | R-03（Root 检测在哪）, M-06（去掉 Root 检测） |
| 仅 V1 签名 | S-07（签名有效吗）, S-08（有没有漏洞） |
| 有多个 DEX | D-09（几个 DEX）, L-07（什么是 multidex） |
| 有 .so 文件 | D-05（NDK/JNI）, F-02（查看 ELF） |
| 有 SQLite 数据库 | R-14（本地数据在哪）, F-01（查看数据库） |
| 代码混淆 | R-09（有没有混淆）, L-06（什么是混淆） |
| debuggable=true | S-18（debuggable）, M-08（禁用调试） |
| allowBackup=true | S-19（allowBackup）, M-09（禁用备份） |
| 导出组件多 | S-20（导出组件风险）, S-07（安全风险） |
| APK 体积大 | A-03（为什么大）, E-06（体积分析） |
| 有追踪 SDK | S-15（追踪 SDK）, S-16（广告 SDK） |

### 9.4 Prompt Library UI

```
┌──────────────────────────────────────────────────┐
│  💡 你可以问我...                                 │
│                                                  │
│  [安全] [逆向] [学习] [修改] [开发] [分析] [更多]   │
│                                                  │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐   │
│  │ 这个APP安全吗│ │广告在哪？   │ │怎么改名字？  │   │
│  └────────────┘ └────────────┘ └────────────┘   │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐   │
│  │为什么闪退？  │ │登录逻辑在哪？│ │有没有追踪SDK？│   │
│  └────────────┘ └────────────┘ └────────────┘   │
│                                                  │
│  或直接输入你的问题...                             │
│  ┌──────────────────────────────────────────┐    │
│  │ 输入问题...                          [发送]│    │
│  └──────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
```

---

---

## 第十部分：未来 Agent 能力规划

### 10.1 当前阶段 vs 未来阶段

| 阶段 | 定位 | 能力 |
|---|---|---|
| **2.0（当前）** | AI Assistant | AI 理解问题 → 调用工具 → 解读结果 → 提供建议和入口。用户执行操作。 |
| **2.5（近期）** | AI Auxiliary | AI 可以在用户确认后自动执行部分操作（如自动定位代码、自动生成 Patch 文件） |
| **3.0（未来）** | AI Agent | AI 可以自主规划任务、多步执行、记忆历史、持续学习 |

### 10.2 未来 Agent 能力规划

#### 能力 1：MCP（Model Context Protocol）

**定义：** 标准化的 AI 与工具之间的通信协议。

**规划：**
- AppDex 将自身能力注册为 MCP Server
- 外部 AI 客户端（如 Claude Desktop、Cursor）可以通过 MCP 调用 AppDex 的分析能力
- AppDex 自身的 AI 也可以通过 MCP 调用外部工具

**场景示例：**
```
用户在 Claude Desktop 中：
"帮我分析这个 APK 的安全状况"

Claude（通过 MCP 调用 AppDex）：
→ AppDex.MCP.scan_security(apk_path)
→ AppDex.MCP.read_manifest(apk_path)
→ AppDex.MCP.list_dex_classes(apk_path)

Claude 综合分析后输出报告
```

**扩展方向：**
- AppDex 成为 Android 生态的 MCP 工具标准
- 支持其他 APK 分析工具通过 MCP 接入 AppDex
- 支持自定义 MCP 工具注册

#### 能力 2：Agent（自主代理）

**定义：** AI 可以自主规划任务步骤，多步执行，而不需要用户逐步指导。

**当前（2.0）：**
```
用户："检查安全"
AI：[显示安全报告]
用户："看看那个硬编码密钥"
AI：[跳转 DEX 搜索]
用户："看看那个类"
AI：[跳转 DEX Browser]
```

**未来（3.0）：**
```
用户："全面分析这个 APK"

AI Agent 自主规划：
  Step 1: 调用 SecurityScanner → 获取安全问题
  Step 2: 调用 DexRepository → 搜索硬编码密钥所在类
  Step 3: 调用 DexRepository.toSmali() → 获取相关代码
  Step 4: 分析代码逻辑 → 判断密钥用途
  Step 5: 调用 ApkFile → 检查签名
  Step 6: 调用 SizeAnalyzer → 体积分析
  Step 7: 综合所有结果 → 生成完整报告

AI 输出：
"全面分析完成。以下是我的发现：
 1. 安全评分：72/100
 2. 发现 1 个硬编码 API 密钥 → [查看]
 3. 签名仅 V1 → [查看]
 4. 集成 3 个追踪 SDK → [查看]
 5. ...

 建议操作：
 → [修复硬编码密钥]
 → [升级签名方案]
 → [查看追踪 SDK]"
```

#### 能力 3：Planning（任务规划）

**定义：** AI 可以将用户的复杂目标拆解为多个子任务，并规划执行顺序。

**场景示例：**
```
用户："我要把这个 APK 改成中文版"

AI Planning：
  Task 1: 分析当前语言资源 → 读取 resources.arsc
  Task 2: 识别所有需要翻译的字符串 → 列出 string 资源
  Task 3: 生成翻译建议 → AI 翻译
  Task 4: 提供修改入口 → [打开资源编辑器]
  Task 5: 修改后重打包 → [开始重打包]
  Task 6: 重新签名 → [重新签名]

AI 输出任务清单：
"已规划 6 个步骤：
 ✓ Step 1: 分析语言资源（完成）
 ✓ Step 2: 识别 152 个字符串（完成）
 → Step 3: 生成翻译建议
 ○ Step 4: 修改资源
 ○ Step 5: 重打包
 ○ Step 6: 签名

 [开始翻译] [查看字符串列表]"
```

#### 能力 4：Memory（记忆）

**定义：** AI 可以记住用户的历史分析、偏好和修改记录。

**短期记忆（当前 2.0 已有）：**
- 当前 APK 的对话历史
- 当前分析会话的上下文

**长期记忆（未来 3.0）：**
- 记住用户分析的每个 APK 的历史记录
- 记住用户的常见问题偏好
- 记住用户的修改历史
- 跨 APK 对比（"这个版本比上次分析的安全评分提高了"）

**场景示例：**
```
用户："和上次分析的版本比，有什么变化？"

AI（使用 Memory）：
"上次你分析的是 v1.2.0（2026-07-01），当前是 v1.3.0。

 变化：
 1. 新增 2 个权限（NFC、BLUETOOTH_CONNECT）
 2. 移除了 AdMob SDK
 3. 新增了 15 个类
 4. 签名从 V1 升级到 V2+V3
 5. 安全评分从 72 提升到 85

 [查看详细对比]"
```

#### 能力 5：Workflow（工作流）

**定义：** 预定义的多步骤自动化流程，AI 按流程执行。

**预定义工作流：**

| 工作流 | 步骤 |
|---|---|
| 安全审计 | 安全扫描 → 权限审计 → 硬编码检测 → 签名验证 → 生成报告 |
| 逆向分析 | DEX 类列表 → 字符串提取 → 关键词搜索 → 代码定位 → 逻辑分析 |
| 修改打包 | 资源/代码修改 → 重打包 → 重签名 → 验证 |
| 版本对比 | 新旧 APK Diff → 变化分析 → 安全影响评估 |
| 全面体检 | 安全扫描 → 体积分析 → 技术栈识别 → 架构分析 → 综合报告 |

**自定义工作流（未来）：**
- 用户可以定义自己的分析流程
- AI 按流程自动执行
- 每步骤可设置自动/手动确认

#### 能力 6：Task（任务管理）

**定义：** AI 可以管理多个并行分析任务。

**场景示例：**
```
用户："同时分析这两个 APK"

AI Task Manager：
  Task 1: 分析 app-v1.apk → 进行中... 45%
  Task 2: 分析 app-v2.apk → 进行中... 30%

  Task 1 完成 → 生成报告
  Task 2 完成 → 生成报告

  "两个 APK 分析完成。
   [查看对比报告]"
```

### 10.3 演进路线图

```
2.0 (当前)
  │ AI Assistant — 理解、解读、建议
  │
  ▼
2.5
  │ AI Auxiliary — 确认后自动执行部分操作
  │ · 自动定位代码并展示
  │ · 自动生成 Patch 文件
  │ · 自动执行安全扫描
  │
  ▼
3.0
  │ AI Agent — 自主规划、多步执行
  │ · Planning: 任务拆解
  │ · Memory: 历史记忆
  │ · Workflow: 自动化流程
  │ · Task: 多任务管理
  │ · MCP: 工具协议标准化
  │
  ▼
3.5+
  │ · 插件化 AI 能力扩展
  │ · 社区共享 Workflow
  │ · AI 自定义技能学习
  │ · 跨设备协同分析
```

---

---

## 第十一部分：AI Provider 设计

### 11.1 设计理念

**AppDex 没有服务器。** 所有 AI 调用由用户设备直接发送到用户选择的 LLM 服务商。AppDex 只是一个 HTTP 客户端，负责：
1. 组装 Prompt
2. 发送 HTTP 请求到用户配置的 LLM API
3. 接收流式响应
4. 解析并展示

用户完全控制：
- 选择哪个 AI 服务商
- 使用哪个模型
- API Key 存储在本地
- 数据发送到哪里

### 11.2 支持的 Provider

| 编号 | Provider | API 协议 | 默认 Base URL | 备注 |
|---|---|---|---|---|
| 1 | **OpenAI** | OpenAI Chat Completions | `https://api.openai.com/v1` | GPT-4o, GPT-4o-mini, o1 等 |
| 2 | **Anthropic** | Anthropic Messages API | `https://api.anthropic.com/v1` | Claude 3.5 Sonnet, Claude 3 Opus 等 |
| 3 | **OpenRouter** | OpenAI Compatible | `https://openrouter.ai/api/v1` | 聚合多家模型 |
| 4 | **DeepSeek** | OpenAI Compatible | `https://api.deepseek.com/v1` | DeepSeek-V3, DeepSeek-R1 |
| 5 | **智谱 (GLM)** | OpenAI Compatible | `https://open.bigmodel.cn/api/paas/v4` | GLM-4, GLM-4-Flash |
| 6 | **Qwen (通义千问)** | OpenAI Compatible | `https://dashscope.aliyuncs.com/compatible-mode/v1` | Qwen-Max, Qwen-Plus |
| 7 | **Gemini (OpenAI 兼容)** | OpenAI Compatible | `https://generativelanguage.googleapis.com/v1beta/openai` | Gemini 1.5 Pro/Flash |
| 8 | **Ollama** | OpenAI Compatible | `http://localhost:11434/v1` | 本地部署模型 |
| 9 | **LM Studio** | OpenAI Compatible | `http://localhost:1234/v1` | 本地部署模型 |
| 10 | **AnythingLLM** | OpenAI Compatible | 用户自定义 | 本地部署 |
| 11 | **LocalAI** | OpenAI Compatible | 用户自定义 | 本地部署 |
| 12 | **Custom API** | OpenAI Compatible | 用户自定义 | 任意 OpenAI 兼容 API |
| 13 | **Anthropic Compatible API** | Anthropic Messages | 用户自定义 | 任意 Anthropic 兼容 API |

### 11.3 Provider 分类

```
┌─────────────────────────────────────────────────┐
│              AI Provider 分类                    │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌─────────────┐  ┌─────────────┐              │
│  │  云端服务    │  │  本地服务    │              │
│  │             │  │             │              │
│  │ · OpenAI    │  │ · Ollama    │              │
│  │ · Anthropic │  │ · LM Studio │              │
│  │ · OpenRouter│  │ · AnythingLLM│             │
│  │ · DeepSeek  │  │ · LocalAI   │              │
│  │ · 智谱(GLM) │  │             │              │
│  │ · Qwen      │  │ 特点：      │              │
│  │ · Gemini    │  │ · 无需联网  │              │
│  │             │  │ · 完全私密  │              │
│  │ 特点：      │  │ · 需要配置  │              │
│  │ · 质量高    │  │   本地环境  │              │
│  │ · 需要联网  │  │ · 质量取决于 │             │
│  │ · 需要API Key│ │   本地硬件  │              │
│  └─────────────┘  └─────────────┘              │
│                                                 │
│  ┌─────────────────────────────────┐           │
│  │        自定义服务                 │           │
│  │                                 │           │
│  │ · Custom API (OpenAI 兼容)      │           │
│  │ · Anthropic Compatible API      │           │
│  │                                 │           │
│  │ 特点：                          │           │
│  │ · 任意兼容 API 端点             │           │
│  │ · 自建/内网 LLM 服务            │           │
│  └─────────────────────────────────┘           │
└─────────────────────────────────────────────────┘
```

### 11.4 配置项设计

#### 通用配置（所有 Provider）

| 配置项 | 类型 | 说明 |
|---|---|---|
| Provider | 选择器 | 从 13 个 Provider 中选择 |
| Base URL | 文本输入 | API 端点地址。选择 Provider 后自动填充默认值，可修改 |
| API Key | 密码输入 | API 密钥。本地加密存储，不明文显示 |
| Model | 文本输入 | 模型名称。如 gpt-4o-mini、claude-3-5-sonnet-20241022 等 |
| Temperature | 滑块 (0-2) | 生成随机性。默认 0.7。专家模式可调 |
| Max Tokens | 数字输入 | 最大输出 Token 数。默认 4096 |
| System Prompt | 文本域 | 系统提示词。专家模式可自定义。默认使用 AppDex 内置提示词 |
| Stream | 开关 | 是否使用流式响应。默认开启 |

#### Provider 预设

选择 Provider 后，自动填充推荐配置：

| Provider | 默认 Base URL | 推荐 Model | API Key 获取提示 |
|---|---|---|---|
| OpenAI | `https://api.openai.com/v1` | `gpt-4o-mini` | "在 platform.openai.com 获取 API Key" |
| Anthropic | `https://api.anthropic.com/v1` | `claude-3-5-sonnet-20241022` | "在 console.anthropic.com 获取 API Key" |
| OpenRouter | `https://openrouter.ai/api/v1` | `openai/gpt-4o-mini` | "在 openrouter.ai 获取 API Key" |
| DeepSeek | `https://api.deepseek.com/v1` | `deepseek-chat` | "在 platform.deepseek.com 获取 API Key" |
| 智谱 (GLM) | `https://open.bigmodel.cn/api/paas/v4` | `glm-4-flash` | "在 open.bigmodel.cn 获取 API Key" |
| Qwen | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus` | "在 dashscope.aliyun.com 获取 API Key" |
| Gemini | `https://generativelanguage.googleapis.com/v1beta/openai` | `gemini-1.5-flash` | "在 ai.google.dev 获取 API Key" |
| Ollama | `http://localhost:11434/v1` | `llama3.2` | "无需 API Key（本地运行）" |
| LM Studio | `http://localhost:1234/v1` | (用户选择) | "无需 API Key（本地运行）" |

### 11.5 API 协议处理

#### OpenAI Compatible API（覆盖大部分 Provider）

**请求格式：**
```
POST {base_url}/chat/completions
Authorization: Bearer {api_key}
Content-Type: application/json

{
  "model": "{model}",
  "messages": [
    {"role": "system", "content": "{system_prompt}"},
    {"role": "user", "content": "{user_message}"},
    ...
  ],
  "temperature": 0.7,
  "max_tokens": 4096,
  "stream": true
}
```

**响应处理：**
- 支持 SSE 流式响应
- 逐 Token 展示
- 解析 `choices[0].delta.content`

#### Anthropic Messages API

**请求格式：**
```
POST {base_url}/messages
x-api-key: {api_key}
anthropic-version: 2023-06-01
Content-Type: application/json

{
  "model": "{model}",
  "system": "{system_prompt}",
  "messages": [
    {"role": "user", "content": "{user_message}"},
    ...
  ],
  "temperature": 0.7,
  "max_tokens": 4096,
  "stream": true
}
```

**响应处理：**
- 支持 SSE 流式响应
- 解析 `content_block_delta.delta.text`

### 11.6 数据安全设计

| 安全项 | 措施 |
|---|---|
| API Key 存储 | 使用 Android EncryptedSharedPreferences 或 DataStore + EncryptedFile 加密存储 |
| API Key 显示 | UI 中密码模式显示，不明文展示 |
| 数据传输 | 使用 HTTPS（用户自定义 URL 时警告） |
| 数据范围 | 只发送当前 APK 的分析上下文给 LLM，不发送用户设备其他数据 |
| 不记录 | AppDex 不记录用户的对话内容到任何日志 |
| 用户知情 | 首次配置时显示隐私声明："你的问题和 APK 分析数据将直接发送到你配置的 AI 服务商。AppDex 不会存储或中转这些数据。" |
| 本地模式 | 选择 Ollama/LM Studio/LocalAI 时，数据完全不离开设备 |
| 超时处理 | 请求超时 60 秒，流式响应 30 秒无数据超时 |

### 11.7 Provider 配置 UI

```
设置 → AI 服务

┌──────────────────────────────────────────────────┐
│  AI 服务配置                                      │
│                                                  │
│  Provider:                                       │
│  ┌──────────────────────────────────────────┐    │
│  │ OpenAI                              ▾    │    │
│  └──────────────────────────────────────────┘    │
│  ┌─┐ ┌─────┐ ┌───────────┐ ┌──────┐ ┌──────┐  │
│  │ │ │Anthropic│ │OpenRouter│ │DeepSeek│ │智谱│ │  │
│  └─┘ └─────┘ └───────────┘ └──────┘ └──────┘  │
│  ┌──────┐ ┌──────┐ ┌────────┐ ┌──────┐ ┌─────┐│
│  │Qwen  │ │Gemini│ │Ollama  │ │LM Stu│ │...│ │  │
│  └──────┘ └──────┘ └────────┘ └──────┘ └─────┘│
│                                                  │
│  Base URL:                                       │
│  ┌──────────────────────────────────────────┐    │
│  │ https://api.openai.com/v1               │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  API Key:                                        │
│  ┌──────────────────────────────────────────┐    │
│  │ ••••••••••••••••••••••••••••••••••••  👁│    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  Model:                                          │
│  ┌──────────────────────────────────────────┐    │
│  │ gpt-4o-mini                              │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ▸ 高级设置（Temperature / Max Tokens / Prompt）  │
│                                                  │
│  ┌──────────┐                                    │
│  │ 测试连接  │                                    │
│  └──────────┘                                    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ ⚠ 你的问题和 APK 分析数据将直接发送到你    │    │
│  │   配置的 AI 服务商。AppDex 不会存储或中转  │    │
│  │   这些数据。选择本地模型（如 Ollama）可   │    │
│  │   实现完全离线。                          │    │
│  └──────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
```

### 11.8 测试连接

用户点击"测试连接"后，AppDex 发送一条测试消息：

**测试消息：** "你好，请回复'AppDex AI 连接测试成功'。"

**成功响应：**
```
✅ 连接成功
Provider: OpenAI
Model: gpt-4o-mini
响应时间: 1.2s
```

**失败响应：**
```
❌ 连接失败
错误：401 Unauthorized
请检查 API Key 是否正确。
```

### 11.9 多 Provider 切换

用户可以保存多个 Provider 配置，快速切换：

```
┌──────────────────────────────────────────────────┐
│  已保存的配置                                     │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ ★ OpenAI (gpt-4o-mini)          [当前]  │    │
│  └──────────────────────────────────────────┘    │
│  ┌──────────────────────────────────────────┐    │
│  │   DeepSeek (deepseek-chat)              │    │
│  └──────────────────────────────────────────┘    │
│  ┌──────────────────────────────────────────┐    │
│  │   Ollama (llama3.2) — 本地              │    │
│  └──────────────────────────────────────────┘    │
│  ┌──────────────────────────────────────────┐    │
│  │   智谱 (glm-4-flash)                     │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  [+ 添加新配置]                                   │
└──────────────────────────────────────────────────┘
```

---

---

## 第十二部分：首页重新设计

### 12.1 设计理念

**AI 第一。工具第二。但工具必须全部保留。只是隐藏。不能删除。**

首页是用户打开 AppDex 后的第一眼。它决定了用户对 AppDex 的第一印象。

**1.0 首页的问题：**
- 标题"多功能 Android 逆向工程工具箱"暴露技术定位
- 快速工具网格直接使用技术术语（DEX、权限审计、签名验证）
- 无任务驱动流程
- "最近分析"永远为空

**2.0 首页的设计目标：**
- 第一眼看到的是 AI，不是工具
- 用户的第一反应是"我可以问它问题"
- 所有工具仍然可达，只是层级更深
- 最近任务可恢复

### 12.2 首页布局

#### 普通模式首页

```
┌─────────────────────────────────────────────────────┐
│  AppDex                                              │
│  2026年7月15日                                       │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌─────────────────────────────────────────────┐    │
│  │                                               │    │
│  │    🤖 你好，我是你的 APK 分析助手              │    │
│  │                                               │    │
│  │    选择一个 APK，我来帮你：                    │    │
│  │    · 检查安全                                  │    │
│  │    · 找广告                                    │    │
│  │    · 分析代码                                  │    │
│  │    · 修改建议                                  │    │
│  │    · 解答疑问                                  │    │
│  │                                               │    │
│  │  ┌──────────────┐  ┌──────────────────────┐  │    │
│  │  │ 📁 选择 APK  │  │ 📱 扫描已安装应用     │  │    │
│  │  └──────────────┘  └──────────────────────┘  │    │
│  │                                               │    │
│  └─────────────────────────────────────────────┘    │
│                                                     │
│  最近任务                                            │
│  ┌─────────────────────────────────────────────┐    │
│  │ [🌐] 微信 v8.0.38                            │    │
│  │       安全评分 82 · 低风险                    │    │
│  │       2 小时前                               │    │
│  ├─────────────────────────────────────────────┤    │
│  │ [🎮] 原神 v4.3.0                            │    │
│  │       安全评分 75 · 中风险                    │    │
│  │       昨天 14:30                             │    │
│  ├─────────────────────────────────────────────┤    │
│  │ [📦] test.apk                               │    │
│  │       安全评分 92 · 低风险                    │    │
│  │       3 天前                                 │    │
│  └─────────────────────────────────────────────┘    │
│                                                     │
│  ┌─────────────────────────────────────────────┐    │
│  │ 💡 提示：你也可以在文件管理器中直接点击       │    │
│  │    APK 文件来开始分析                         │    │
│  └─────────────────────────────────────────────┘    │
│                                                     │
├─────────────────────────────────────────────────────┤
│ [首页] [任务] [文件] [工具] [设置]                    │
└─────────────────────────────────────────────────────┘
```

#### 高级模式首页

```
┌─────────────────────────────────────────────────────┐
│  AppDex                                              │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌─────────────────────────────────────────────┐    │
│  │  🤖 AI 分析助手                               │    │
│  │  选择 APK 开始分析，AI 自动总结               │    │
│  │  ┌──────────────┐  ┌──────────────────────┐  │    │
│  │  │ 📁 选择 APK  │  │ 📱 扫描已安装应用     │  │    │
│  │  └──────────────┘  └──────────────────────┘  │    │
│  └─────────────────────────────────────────────┘    │
│                                                     │
│  最近任务                                            │
│  ┌─────────────────────────────────────────────┐    │
│  │ [🌐] 微信 v8.0.38  82分  2小时前             │    │
│  ├─────────────────────────────────────────────┤    │
│  │ [🎮] 原神 v4.3.0   75分  昨天               │    │
│  └─────────────────────────────────────────────┘    │
│                                                     │
│  快捷工具                                            │
│  ┌────────┐ ┌────────┐ ┌────────┐                │
│  │DEX浏览 │ │安全扫描 │ │签名验证 │                │
│  └────────┘ └────────┘ └────────┘                │
│  ┌────────┐ ┌────────┐ ┌────────┐                │
│  │HEX编辑 │ │APK对比 │ │体积分析 │                │
│  └────────┘ └────────┘ └────────┘                │
│                                                     │
├─────────────────────────────────────────────────────┤
│ [首页] [任务] [文件] [工具] [设置]                    │
└─────────────────────────────────────────────────────┘
```

#### 专家模式首页

```
┌─────────────────────────────────────────────────────┐
│  AppDex                                              │
├─────────────────────────────────────────────────────┤
│                                                     │
│  最近任务                                            │
│  ┌─────────────────────────────────────────────┐    │
│  │ com.tencent.mm  v8.0.38  82分  classes.dex │    │
│  ├─────────────────────────────────────────────┤    │
│  │ com.miHoYo.Yuanshen  v4.3.0  75分  3 DEX   │    │
│  └─────────────────────────────────────────────┘    │
│                                                     │
│  ┌────────────┐  ┌────────────┐                    │
│  │ 选择 APK   │  │ 扫描已安装  │                    │
│  └────────────┘  └────────────┘                    │
│                                                     │
│  工具                                                │
│  ┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐         │
│  │ DEX  ││ AXML ││ ARSC ││ HEXd ││ ELF  │         │
│  └──────┘└──────┘└──────┘└──────┘└──────┘         │
│  ┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐         │
│  │ Sign ││Repack││ Diff ││ Sec  ││ Size │         │
│  └──────┘└──────┘└──────┘└──────┘└──────┘         │
│  ┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐         │
│  │SQLite││ Term ││ FTP  ││ Web  ││ Hash │         │
│  └──────┘└──────┘└──────┘└──────┘└──────┘         │
│  ┌──────┐┌──────┐┌──────┐                         │
│  │Encode││ Dev  ││Plugin│                         │
│  └──────┘└──────┘└──────┘                         │
│                                                     │
├─────────────────────────────────────────────────────┤
│ [首页] [任务] [文件] [工具] [设置]                    │
└─────────────────────────────────────────────────────┘
```

### 12.3 首页元素详细设计

#### 12.3.1 顶部栏

| 模式 | 标题 | 副标题 |
|---|---|---|
| 普通 | "AppDex" | 当前日期（如"7月15日"） |
| 高级 | "AppDex" | "AI APK 分析助手" |
| 专家 | "AppDex" | 无副标题 |

#### 12.3.2 AI 欢迎卡

| 元素 | 普通模式 | 高级模式 | 专家模式 |
|---|---|---|---|
| 标题 | "你好，我是你的 APK 分析助手" | "AI 分析助手" | 不显示（专家不需要引导） |
| 描述 | 列出能做的事（安全/广告/代码/修改/疑问） | "选择 APK 开始分析，AI 自动总结" | — |
| 按钮 | [选择 APK] [扫描已安装] | [选择 APK] [扫描已安装] | [选择 APK] [扫描已安装] |
| 样式 | 大卡片，AmberGold 边框 | 中等卡片 | 小按钮 |

#### 12.3.3 最近任务列表

| 属性 | 说明 |
|---|---|
| 数据源 | RecentTaskRepository（Room 持久化） |
| 数量 | 最多显示 5 条 |
| 每条显示 | 应用图标 + 应用名 + 版本 + 安全评分 + 风险等级 + 时间 |
| 交互 | 点击 → 恢复分析会话 → 跳转任务页 |
| 长按 | 删除记录 |
| 空状态 | "暂无任务，点击上方按钮开始分析" |
| 排序 | 按时间倒序 |

#### 12.3.4 快捷工具（高级模式）

| 位置 | 工具 | 跳转 |
|---|---|---|
| 1 | DEX 浏览 | DexBrowserScreen |
| 2 | 安全扫描 | SecurityScannerScreen |
| 3 | 签名验证 | SigningScreen |
| 4 | HEX 编辑 | HexEditorScreen |
| 5 | APK 对比 | ApkDiffScreen |
| 6 | 体积分析 | SizeAnalyzerScreen |

注意：快捷工具需要 APK 上下文。如果没有当前 APK，点击时提示"请先选择 APK"。

#### 12.3.5 工具网格（专家模式）

专家模式首页直接显示所有工具的平铺网格，无需折叠。

#### 12.3.6 提示卡（普通模式）

普通模式首页底部显示使用提示：
- "你也可以在文件管理器中直接点击 APK 文件来开始分析"
- "在设置中切换到高级模式可以使用更多工具"
- "长按最近任务可以删除记录"

### 12.4 FAB（浮动操作按钮）

| 模式 | FAB 功能 |
|---|---|
| 普通 | [+] → 打开文件选择器 |
| 高级 | [+] → 打开文件选择器 |
| 专家 | [+] → 打开文件选择器 |

### 12.5 首页交互流程

```
首页
├── 点击 AI 欢迎卡 [选择 APK]
│   → 文件选择器 → 选择 .apk → 自动分析 → 任务页
├── 点击 AI 欢迎卡 [扫描已安装]
│   → 包列表 → 选择应用 → 导出 APK → 自动分析 → 任务页
├── 点击最近任务项
│   → 恢复分析会话 → 任务页
├── 长按最近任务项
│   → 删除确认 → 删除记录
├── 点击快捷工具（高级模式）
│   ├── 有当前 APK → 跳转对应工具
│   └── 无当前 APK → 提示"请先选择 APK"
├── 点击工具网格（专家模式）
│   → 跳转对应工具
├── FAB
│   → 文件选择器 → 选择 .apk → 自动分析 → 任务页
└── 底部导航
    ├── 首页 → 当前页
    ├── 任务 → 任务页（如果有当前 APK）/ 空状态
    ├── 文件 → 文件管理器
    ├── 工具 → 工具页
    └── 设置 → 设置页
```

### 12.6 首页 × 底部导航的关系

```
┌─────────────────────────────────────────────────┐
│                  底部导航 5 Tab                    │
├──────────┬──────────┬──────────┬──────────┬──────┤
│   首页    │   任务    │   文件    │   工具    │ 设置 │
│          │          │          │          │      │
│ AI 入口   │ APK 工作台 │ 文件浏览  │ 全部工具  │ 配置  │
│ 选择 APK  │ 安全评分  │ 浏览文件  │ 分类入口  │ AI   │
│ 最近任务  │ AI 对话   │ 打开文件  │           │ 模式  │
│          │ 操作中心  │          │           │      │
│          │ 高级分析  │          │           │      │
└──────────┴──────────┴──────────┴──────────┴──────┘

关系说明：
· 首页 = 入口（选 APK + 最近任务 + AI）
· 任务 = 工作台（当前 APK 的所有信息和操作）
· 文件 = 文件管理（独立功能）
· 工具 = 工具箱（所有专业工具的入口，全保留）
· 设置 = 配置（AI 配置 + 模式切换 + 其他设置）
```

### 12.7 "任务" Tab 的角色

"任务" Tab 是首页的延伸。用户在首页选择 APK 后，自动跳转到任务 Tab。如果用户直接点击"任务" Tab：

| 状态 | 显示内容 |
|---|---|
| 有当前 APK | 显示当前 APK 的任务工作台（安全评分 + AI 总结 + 对话 + 操作中心） |
| 无当前 APK | 空状态："还没有任务。去首页选择一个 APK 开始分析吧。" + [去首页] 按钮 |

### 12.8 工具保留确认

**所有现有工具必须 100% 保留，一个都不删除。**

| 工具 | 普通模式入口 | 高级模式入口 | 专家模式入口 |
|---|---|---|---|
| DEX Browser | AI Action Card | 首页快捷工具 + 工具页 | 首页工具网格 + 工具页 |
| Manifest 编辑器 (AXML) | AI Action Card | 工具页 | 首页工具网格 + 工具页 |
| 资源编辑器 (ARSC) | AI Action Card | 工具页 | 首页工具网格 + 工具页 |
| HEX 编辑器 | 隐藏（AI 不建议普通用户使用） | 工具页（折叠） | 首页工具网格 + 工具页 |
| ELF 查看器 | 隐藏 | 工具页（折叠） | 首页工具网格 + 工具页 |
| SQLite 查看器 | AI Action Card | 工具页 | 首页工具网格 + 工具页 |
| 签名工具 | AI Action Card | 首页快捷工具 + 工具页 | 首页工具网格 + 工具页 |
| 重打包工具 | AI Action Card | 工具页 | 首页工具网格 + 工具页 |
| 安全扫描器 | AI Action Card | 首页快捷工具 + 工具页 | 首页工具网格 + 工具页 |
| APK 对比 | AI Action Card | 首页快捷工具 + 工具页 | 首页工具网格 + 工具页 |
| 体积分析器 | AI Action Card | 首页快捷工具 + 工具页 | 首页工具网格 + 工具页 |
| 文件编辑器 | AI Action Card | 工具页 | 首页工具网格 + 工具页 |
| 终端 | 隐藏 | 隐藏 | 首页工具网格 + 工具页 |
| FTP 客户端 | 隐藏 | 工具页 | 首页工具网格 + 工具页 |
| Web 服务器 | 隐藏 | 工具页 | 首页工具网格 + 工具页 |
| 哈希计算器 | AI Action Card | 工具页 | 首页工具网格 + 工具页 |
| 编码转换器 | 隐藏 | 工具页 | 首页工具网格 + 工具页 |
| 设备信息 | 隐藏 | 工具页 | 首页工具网格 + 工具页 |
| 插件中心 | 隐藏 | 工具页 | 首页工具网格 + 工具页 |
| 文件管理器 | 底部导航 Tab | 底部导航 Tab | 底部导航 Tab |
| 远程管理 | 隐藏 | 工具页 | 首页工具网格 + 工具页 |
| 音频播放器 | 文件管理器点击音频 | 文件管理器点击音频 | 文件管理器点击音频 |
| 图片查看器 | 文件管理器点击图片 | 文件管理器点击图片 | 文件管理器点击图片 |
| 视频播放器 | 文件管理器点击视频 | 文件管理器点击视频 | 文件管理器点击视频 |

**关键确认：每个工具至少在专家模式下可达。普通模式下通过 AI Action Card 触达。没有任何工具被删除。**

---

---

## 附录：AppDex 2.0 产品愿景

### 一句话定位

**AppDex 2.0 = 目前最好用的 AI APK 分析助手**

### 核心价值

> 用户不需要懂技术。只需要会提问。

### 用户体验目标

一个完全不懂技术的用户，打开 AppDex 后：
1. 看到 AI 欢迎卡，知道可以"问问题"
2. 选择一个 APK
3. 自动分析完成，AI 给出"安全/不安全"的结论
4. 用户问"广告在哪"，AI 直接定位并给出"查看代码"按钮
5. 用户问"怎么改名字"，AI 给出修改位置和"打开编辑器"按钮
6. 用户全程不需要知道什么是 DEX、什么是 Manifest、什么是 Smali

### 产品差异化

| 对比项 | 传统逆向工具 | AppDex 2.0 |
|---|---|---|
| 用户门槛 | 需要专业知识 | 零门槛 |
| 交互方式 | 找工具 → 用工具 → 看原始数据 | 问问题 → AI 解读 → 一键操作 |
| 数据解读 | 用户自己解读 | AI 翻译成人话 |
| 操作引导 | 无 | AI 提供 Action Card 一键跳转 |
| 安全评估 | 需要多工具交叉分析 | AI 自动综合所有工具结果 |
| 学习成本 | 高 | 低（AI 帮你学习） |

### 产品边界

AppDex 2.0 是**静态分析工具 + AI 助手**，不是：
- 不是动态调试器（推荐 Frida/Xposed）
- 不是编译器（需要修改代码时在外部编译）
- 不是破解工具（不提供自动破解）
- 不是云端服务（所有数据在本地）

---

*本文档为 AppDex 2.0 最核心的产品设计规格。所有后续设计、开发、测试工作均以此文档为准。*
