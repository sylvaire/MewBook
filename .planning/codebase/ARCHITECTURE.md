# 架构（ARCHITECTURE）

## 风格

项目采用 **分层 + Repository 模式**，与 **Hilt 单例作用域** 结合：

1. **UI**：Compose 屏幕与 `*ViewModel`（按功能分屏），共享组件放在 `ui.components`，视觉系统放在 `ui.theme`。
2. **Domain**：`domain.model` 领域模型；`domain.repository` 接口；`domain.usecase` 按业务拆分的用例类；可测试业务规则优先放在 `domain.policy`。
3. **Data**：`data.local`（Room Entity/DAO/Database/Converters）、`data.remote`（WebDAV）、`data.repository`（实现类）、`data.backup`、`data.preferences`、`data.smartimport`、`data.update`。

## 依赖方向

- ViewModel / UseCase 依赖 **repository 接口**（`domain.repository`）。
- `RepositoryModule` 使用 `@Binds` 将 `*RepositoryImpl`、`DavClient` 等绑定到接口，安装于 `SingletonComponent`。
- `DatabaseModule`、`NetworkModule` 提供 Room、OkHttp 等基础设施。

## 导航

- 单一 Activity（`MainActivity`）承载 Compose；`MewBookNavHost` 使用 Navigation Compose。
- 主导航为底部四 Tab：`Home`、`Statistics`、`Asset`、`Settings`；其他功能（账本管理、分类、WebDAV、导出、智能导入、周期模板、账户编辑等）通过路由进入。
- `Screen` sealed class 集中定义 route，当前包含带参数的 `AccountEdit` 与统计分类下钻 `CategoryExpenseDetail`。

## 状态与主题

- `MainActivity` 注入 `ThemeViewModel`，从 DataStore 读取 `AppThemeMode`，在 `MewBookTheme` 外层控制浅色/深色/跟随系统。
- 启动背景由系统 launch theme 承担；`MainActivity` 不再额外叠加 Compose 启动遮罩。
- `MainActivity` 同时持有 `AppUpdateViewModel`，负责 GitHub Release 更新弹窗、下载进度、安装权限与系统安装器 handoff。
- `MainActivity.onStart` 触发 `DavAutoBackupCoordinator`，在 DAV 自动备份开启时每天首次进入前台上传一次备份。

## 并发

- 远程调用（如 `DavClient`、智能导入、应用更新）使用 `Dispatchers.IO` 包装 suspend 函数。
- 自动备份 coordinator 使用 `Mutex` 防止同一进程内重复前台触发并发上传；失败后延迟 5 秒重试一次；清理旧备份操作有 60 秒超时保护。
- UI 层通过 `collectAsStateWithLifecycle` 等收集 Flow。

## 数据边界

- Room 实体与领域模型在 Repository 实现中映射；当前数据库版本为 `4`。
- 备份/迁移逻辑在 `data.backup` 与数据库版本演进中体现；新增实体或字段时同步更新备份模型、迁移与测试。
- 导入恢复前自动创建本地安全备份（`context.filesDir/safety_backups/`），最多保留 3 份。
- 账户初始化：`EnsureDefaultAccountForLedgerUseCase` 按 `ledgerId` 检查，若该账本下无账户则自动创建"现金"默认账户。`HomeViewModel` 初始化时调用，确保首页加载时当前账本已有可用账户。新建账本时 `LedgerManagementViewModel.addLedger()` 也会自动创建默认账户。
