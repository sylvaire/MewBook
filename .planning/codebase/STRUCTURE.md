# 目录结构（STRUCTURE）

## 仓库根目录

```
MewBook/
├── app/                    # 唯一 Android 应用模块
├── build.gradle.kts        # 根构建脚本（插件版本）
├── settings.gradle.kts
├── gradle/
├── signing/                # 签名相关资源（勿提交密钥材料到公开仓库）
├── keystore.properties     # 本地可选，用于 release 签名配置
└── ...
```

## `app/src/main/java/com/mewbook/app/`

| 包/目录 | 职责 |
|---------|------|
| `MainActivity.kt` | 单一 Activity，Compose 入口、主题与 Splash |
| `MewBookApplication.kt` | `@HiltAndroidApp` Application |
| `di/` | Hilt：`DatabaseModule`、`NetworkModule`、`RepositoryModule` |
| `data/local/` | Room：`entity/`、`dao/`、`database/`、`Converters` |
| `data/remote/` | `DavClient`、`DavRemoteDataSource` |
| `data/repository/` | 各 Repository 实现、备份、导出等 |
| `data/backup/` | 备份模型、迁移 |
| `data/preferences/` | DataStore 主题等 |
| `domain/model/` | 领域模型 |
| `domain/repository/` | Repository 接口 |
| `domain/usecase/` | `account/`、`category/`、`dav/`、`ledger/`、`record/` 等 |
| `ui/navigation/` | `NavHost.kt`、`Screen.kt` |
| `ui/screens/` | 各功能屏：`home`、`statistics`、`asset`、`budget`、`settings`、`dav`、`export`、`ledger`、`categories`、`add` 等 |
| `ui/components/` | 复用组件（如 `RecordItem`、`CategoryChip`、TopBar） |
| `ui/theme/` | `Theme`、`Color`、`Type`、`ThemeViewModel` |
| `util/` | 扩展工具 |

## 资源

- `app/src/main/res/`：`values` 字符串与主题、`xml/file_paths`、`mipmap` 图标等。

## 测试

- `app/src/test/java/...`：单元测试（DAV、备份迁移、表达式辅助等）。
- `app/src/androidTest/`：当前为空（依赖已声明）。

## 规划产物

- `.planning/codebase/`：本套 GSD codebase 文档。
