# 风险与关注点（CONCERNS）

## 安全与隐私

- **WebDAV 凭据**：存储与传输路径需持续审计（Security Crypto、HTTPS、服务端配置）；日志中避免完整密码与 URL 敏感片段。备份导出时密码字段已清空，防止泄露到备份文件。
- **DAV 自动备份**：打开 App 后可能立即发起网络上传；失败后延迟 5 秒重试一次，然后静默记录，不阻塞启动或弹出打扰式 UI。
- **导入安全回滚**：`restoreEnvelope` 执行前自动创建本地安全备份到 `context.filesDir/safety_backups/`，最多保留 3 份；安全备份失败不阻塞导入流程。
- **智能导入 API Key 与账单内容**：API Key 通过 encrypted preferences 保存（可用时），但用户输入账单内容会发送到配置的 OpenAI 兼容服务；UI 与文档需保持明确提示。
- **应用内更新安装**：`REQUEST_INSTALL_PACKAGES` 用于 APK 安装 handoff；安装权限、下载文件路径与 asset 选择策略都属于安全敏感路径。用户可通过"跳过此版本"或"关闭更新功能"禁用自动检查；关闭后静默检查不再触发，设置页可重新开启。
- **签名与密钥**：`signing/`、`keystore.properties`、`*.jks` 已在 `.gitignore` 中排除；不要把本地签名材料纳入版本控制。

## 发布构建

- Release **未开启 R8/混淆**（`isMinifyEnabled = false`），APK 体积与反编译可读性较差防护；若上架商店，建议评估 `minify` + 规则。
- `.github/workflows/release.yml` 的 GitHub Release body 仍含 1.0.6 的静态发布说明；下一次正式发版前应改为读取生成的 release notes 或手动同步正文，避免重复发布旧内容。

## 数据可靠性

- Room `exportSchema = false`：团队内应通过迁移测试与备份路径保证升级安全；当前数据库版本为 `4`。
- **多设备同步**：WebDAV 与本地合并冲突策略需在业务层明确（若尚未文档化，属于产品/技术债）。

## 测试缺口

- **无 androidTest 实现**：回归依赖手动；关键路径（导出、备份恢复、DAV 同步、APK 更新安装、智能导入 UI）仪器自动化覆盖不足。

## 依赖健康

- Compose BOM、AGP、Kotlin 版本为 2024 年前后组合；长期需跟进 **Compose Compiler** 与 **Kotlin** 的兼容矩阵。

## 运维与可观测性

- 无内置崩溃上报/SDK；生产问题依赖用户反馈与 `adb` 日志。
