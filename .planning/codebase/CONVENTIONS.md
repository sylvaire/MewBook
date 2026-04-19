# 约定（CONVENTIONS）

## 命名

- **包名**：`com.mewbook.app`，功能按层（`data` / `domain` / `ui`）与特性（`screens/home` 等）划分。
- **屏幕**：`*Screen.kt` 为 Composable 页面，`*ViewModel.kt` 为对应逻辑。
- **持久化**：Room 实体后缀 `Entity`，访问对象 `*Dao`，数据库 `MewBookDatabase`。
- **仓库**：接口在 `domain.repository`，实现类 `*RepositoryImpl` 位于 `data.repository`。

## Kotlin / Compose

- ViewModel 使用 Hilt `@HiltViewModel`（各屏一致）与构造函数注入。
- 导航路由集中在 `Screen` sealed class，带参数路由提供 `createRoute` 工厂方法。
- 主题与色板定义于 `ui/theme`，与 Material3 `MaterialTheme` 配合。

## 依赖注入

- 单例组件：`@Singleton` 用于 Database、OkHttp、Repository 实现等。
- 抽象绑定集中在 `RepositoryModule`（`@Binds`），避免在实现类重复作用域声明（以模块为准）。

## 日志与调试

- `DavClient` 使用 `Log.d` 输出探测请求信息（含部分脱敏的用户名前缀），发布前可考虑统一日志开关或 ProGuard 规则。

## 版本与数据库

- Room `exportSchema = false`：未导出 schema JSON；迁移依赖手写 `Migration` 与备份逻辑时需自行保证可回滚与测试覆盖。

## 国际化

- 字符串主要位于 `res/values/strings.xml`（具体以资源文件为准）；界面文案以中文产品语境为主（如底部导航「记账」「报表」等）。
