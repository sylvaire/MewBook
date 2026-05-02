# 路线图

## 里程碑：网盘自动备份

### Phase 1 — 自动备份决策与状态

- 新增 `DavAutoBackupPolicy`，覆盖是否应执行与远端保留文件筛选。
- 新增 DataStore 状态仓库，保存最近尝试日期、尝试时间、成功时间和错误/警告信息。
- 新增单元测试锁定每日一次、配置条件与保留 30 份策略。

### Phase 2 — DAV 上传、清理与前台触发

- 扩展 DAV remote/repository，支持 DELETE 与 `pruneBackupFiles(config, keepLatestCount = 30)`。
- 新增 `DavAutoBackupCoordinator`，由 `MainActivity.onStart` 调用，并用 mutex 防止并发上传。
- 自动备份复用现有 DAV 导出快照逻辑；上传成功后只清理旧自动备份，不删除手动导出文件。

### Phase 3 — 设置页展示与验证

- DAV 设置页新增“打开 App 自动备份”开关，保存配置后生效。
- DAV 设置页显示最近自动备份尝试、成功、错误或清理警告。
- DAV 手动导出支持自定义文件名；留空时使用默认文件名。
- DAV 导入流程改为先列出远端备份，用户手动选择后预览并确认恢复同一文件。
- 运行单元测试、Debug 构建与 lint。

## 后续（Backlog，非本里程碑）

- 可选备份频率、保留份数配置。
- 后台定时备份或网络条件约束。
