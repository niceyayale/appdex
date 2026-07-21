# AppX Genesis RC3 Workspace Intelligence Report

## 1. 完整体验链

```
APP 启动
  ↓
AI 页面（显示当前 APK: com.appdex）
  ↓ 点击搜索图标
Command Palette（"搜索类名、Activity、权限、资源、文件..."）
  ↓ 搜索 "dex" → 找到 DEX 浏览器
  ↓ ESC 关闭
  ↓ 点击底部"工作区"
工作区仪表盘
  ↓ 显示：包名、版本、大小、安全评分(56)
  ↓ 快捷操作：Manifest / DEX / 资源 / 签名 / 重打包
  ↓ 分析：安全分析 / 查看报告
  ↓ 点击"安全分析"
Security Scanner
  ↓ 执行深度扫描
  ↓ 显示评分(56) → 同步到 Session
  ↓ 返回
工作区仪表盘
  ↓ 评分已更新为 56（与 Security 一致）
  ↓ 点击"查看报告"
Report 页面
  ↓ 显示评分 56/100（与 Workspace 一致）
  ↓ 显示安全发现 8 项
  ↓ 按下 Home 键
后台 → 恢复
  ↓ 仍在 Report 页面，评分保持 56
  ↓ 杀进程 → 重启
工作区仪表盘
  ↓ 会话恢复：包名/版本/大小/评分(56) 全部保持
```

## 2. Context 流转图

```
┌─────────────────────────────────────────────────────┐
│                   SessionManager                     │
│  ┌───────────────────────────────────────────────┐  │
│  │           AnalysisSession                     │  │
│  │  • securityScore = 56 (唯一来源)              │  │
│  │  • summary { dexCount, resourceCount, ... }   │  │
│  │  • lastTool / lastRoute                       │  │
│  └──────────────────┬────────────────────────────┘  │
│                     │                                │
│         ┌───────────┼───────────┐                    │
│         ↓           ↓           ↓                    │
│    ┌─────────┐ ┌─────────┐ ┌─────────┐              │
│    │Workspace│ │ Report  │ │Security │              │
│    │  score  │ │  score  │ │ Scanner │              │
│    │   56    │ │   56    │ │   56    │              │
│    └────┬────┘ └────┬────┘ └────┬────┘              │
│         │           │           │                    │
│         ↓           ↓           ↓                    │
│    RiskScoreCalculator (统一算法)                     │
│    100 - CRITICAL×25 - HIGH×15 - MEDIUM×8 - LOW×3   │
└─────────────────────────────────────────────────────┘

Navigation Context Flow:
  Command Palette
    ↓ searchQuery
  DexBrowser(searchQuery="MainActivity")
    ↓ auto-select first DEX → auto-search classes
  DEX Class List (filtered by query)
    ↓ click class
  Smali Viewer
    ↓ back
  DEX Class List (search query preserved)
    ↓ back
  Workspace (context preserved)
```

## 3. 修复的问题

### P0 — 评分不一致（已修复）
- **问题**: Workspace 评分(92)、Report 评分(92)、Security Scanner 评分(56) 三者不一致
- **根因**: ToolBridge.generateFindings() 和 SecurityScannerRepository.scan() 使用不同算法独立计算评分
- **修复**: 建立 `RiskScoreCalculator` 作为唯一评分计算源；SecurityScannerViewModel 扫描后通过 `SessionManager.setFindings()` 同步评分到 Session
- **验证**: Security 扫描后 Workspace 和 Report 评分自动更新为 56

### P1 — 工具跳转无 Context（已修复）
- **问题**: 点击组件(Activity/Service)只打开 DEX 浏览器首页，不自动搜索
- **修复**: Route.DexBrowser 新增 `searchQuery` 参数；Command Palette 组件点击传递类名
- **验证**: 点击 Activity → DexBrowser 自动选择 DEX 并搜索类名

### P1 — HEX 返回丢失 offset（已修复）
- **问题**: HEX 编辑器返回后无法恢复之前的浏览位置
- **修复**: Route.HexEditor 新增 `offset` 参数；HexEditorScreen 自动滚动到指定位置

### P1 — ARSC 无资源定位（已修复）
- **问题**: 跳转到 ARSC 查看器无法自动定位资源
- **修复**: Route.ArscViewer 新增 `resourceId` 参数；ArscEditorScreen 自动搜索资源

### P2 — 重启后工作区数据丢失（已修复）
- **问题**: APP 重启后工作区显示 "0 个 DEX"、"无签名" 等空数据
- **修复**: SessionMetadata 新增 15 个持久化字段；AnalysisSession 新增 SessionSummary；WorkspaceScreen 和 ReportScreen 使用 summary 作为 fallback
- **验证**: 重启后工作区恢复包名、版本、大小、评分

### P2 — Command Palette 仅搜索菜单（已修复）
- **问题**: Command Palette 搜索仅限于工具名称，不支持 IDE 级搜索
- **修复**: 升级占位符文本；组件(Activity/Service/Receiver)点击直接定位到 DEX；文件点击携带 entryName

## 4. 评分一致性验证

| 页面 | 扫描前评分 | 扫描后评分 | 一致性 |
|------|-----------|-----------|--------|
| Workspace | 92 | 56 | ✓ |
| Report | N/A | 56 | ✓ |
| Security Scanner | N/A | 56 | ✓ |
| 重启后 Workspace | N/A | 56 | ✓ |

**评分算法**: `RiskScoreCalculator.calculate(critical, high, medium, low)`
- 公式: `100 - CRITICAL×25 - HIGH×15 - MEDIUM×8 - LOW×3`
- 唯一来源: ToolBridge 和 SecurityScannerRepository 共用

## 5. 工具联动验证

| 起始页面 | 操作 | 目标页面 | Context 携带 | 验证结果 |
|---------|------|---------|-------------|---------|
| Command Palette | 点击 Activity | DEX Browser | searchQuery=类名 | ✓ 自动搜索 |
| Command Palette | 点击 Service | DEX Browser | searchQuery=类名 | ✓ 自动搜索 |
| Command Palette | 点击 .xml 文件 | AXML Editor | entryName=文件名 | ✓ 自动加载 |
| Workspace | 点击"查看代码结构" | DEX Browser | apkPath=当前APK | ✓ 自动加载 |
| Workspace | 点击"查看应用配置" | AXML Editor | apkPath=当前APK | ✓ 自动加载 |
| Workspace | 点击"安全分析" | Security Scanner | apkPath=当前APK | ✓ 自动加载 |
| Workspace | 点击"查看报告" | Report | session=当前会话 | ✓ 评分一致 |
| Security Scanner | 返回 | Workspace | 评分已同步 | ✓ 评分更新 |
| 任意工具 | 返回 | Workspace | APK上下文保持 | ✓ 无需重选 |

## 6. Command Palette 验证

| 搜索词 | 结果 | 直接定位 | 验证结果 |
|--------|------|---------|---------|
| dex | DEX 浏览器 | 打开 DEX Browser | ✓ |
| AppDex | 无结果（无组件匹配） | 提供"问 AI"选项 | ✓ |
| (占位符) | "搜索类名、Activity、权限、资源、文件..." | — | ✓ 升级文本 |
| 组件点击 | Activity/Service/Receiver | DEX Browser + searchQuery | ✓ 直接定位 |

## 7. 剩余体验问题

1. **旧会话 DEX/资源数量显示为 0**: 在 RC3 之前创建的会话，其 SessionMetadata 没有新的 summary 字段，重启后 dexCount/resourceCount 为 0。需要重新分析 APK 才能获得准确数据。（新分析的 APK 不受影响）
2. **Security Scanner 深度扫描会覆盖初始评分**: 安全扫描比初始分析(ToolBridge)更深入（检测硬编码密钥、弱加密等），扫描后评分会降低。这是正确行为（更全面=更准确），但可能导致用户看到评分从 92 突然变为 56。
3. **DEX Browser searchQuery 需要等待 DEX 加载**: 当从 Command Palette 跳转到 DEX Browser 时，如果 APK 较大（如 24 个 DEX），需要等待加载完成后才能自动搜索。LaunchedEffect 已处理此情况，但用户可能短暂看到加载状态。

## 8. 最终评分

**体验评分: 88/100**

### 扣分项
- -5: 旧会话 summary 为空（需重新分析）
- -4: Security Scanner 覆盖初始评分可能让用户困惑
- -3: DEX 加载等待期间的空状态

### 加分项
- 统一评分系统完全工作，所有页面一致
- Navigation Context 携带 searchQuery/offset/resourceId
- 重启后会话完整恢复（包名/版本/大小/评分）
- Command Palette 升级为 IDE 级搜索
- 工具间跳转不再丢失 APK 上下文

## 9. RC3 状态: 达到 Workspace Intelligence 标准

### 已实现
- ✅ 唯一 RiskScore Pipeline (RiskScoreCalculator)
- ✅ Navigation Context System (searchQuery / offset / resourceId)
- ✅ Command Palette IDE 级搜索
- ✅ Session Metadata 持久化增强 (15 个新字段)
- ✅ 重启后完整 Workspace 恢复
- ✅ 所有页面读取同一 Session.securityScore

### 核心体验
> 用户感觉：我不是在打开不同工具。我是在操作同一个 APK。AppX 不是工具集合。AppX 是 APK Workspace。
