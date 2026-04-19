# 风险与关注点（CONCERNS）

## 安全与隐私

- **WebDAV 凭据**：存储与传输路径需持续审计（Security Crypto、HTTPS、服务端配置）；日志中避免完整密码与 URL 敏感片段。
- **签名与密钥**：`signing/`、`keystore.properties` 不应进入公开版本控制；已打开 `mewbook-release.jks` 的用户需确认 `.gitignore` 策略。

## 发布构建

- Release **未开启 R8/混淆**（`isMinifyEnabled = false`），APK 体积与反编译可读性较差防护；若上架商店，建议评估 `minify` + 规则。

## 数据可靠性

- Room `exportSchema = false`：团队内应通过迁移测试与备份路径保证升级安全。
- **多设备同步**：WebDAV 与本地合并冲突策略需在业务层明确（若尚未文档化，属于产品/技术债）。

## 测试缺口

- **无 androidTest 实现**：回归依赖手动；关键路径（导出、备份恢复、DAV 同步）自动化覆盖不足。

## 依赖健康

- Compose BOM、AGP、Kotlin 版本为 2024 年前后组合；长期需跟进 **Compose Compiler** 与 **Kotlin** 的兼容矩阵。

## 运维与可观测性

- 无内置崩溃上报/SDK；生产问题依赖用户反馈与 `adb` 日志。
