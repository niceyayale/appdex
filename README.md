# appdex

面向极客的 Android 工具集,目标是在逆向相关核心能力上比肩 MT 管理器、架构支持多年演进。

## 状态

- 子项目 #1a: `:core:io` KMP 文件系统抽象 —— ✅ 完成
- 子项目 #2: `:core:apk` / `:core:axml` / `:core:arsc` 只读解析 —— ✅ 完成(MVP)
- 子项目 #4: `:core:dex` DEX 只读解析(smali 反汇编) —— ✅ 完成(MVP)

## 构建

要求:JDK 17+(当前用 JDK 25)、Gradle 8.14.4(已包含 wrapper)。

```bash
./gradlew :core:io:build          # 全部构建 + 测试
./gradlew :core:io:jvmTest        # 仅 JVM 单测
./gradlew detekt                 # 静态检查
```

## 模块结构

详见 `docs/superpowers/specs/2026-07-07-appdex-global-architecture-design.md`。

## 测试分工

- 核心逻辑:本项目自测,JVM 单测覆盖
- Android UI:需用户装机验证(后续子项目)
