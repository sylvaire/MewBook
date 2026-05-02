# MewBook 第三方数据导入设计文档

> 最后更新：2026-05-02

## 1. 概述

MewBook 支持两条导入路径，将第三方记账数据转换为本地记录：

- **智能导入（AI 辅助）**：文本或文件发送到 OpenAI 兼容 API，AI 返回结构化 JSON，解析后合并到本地
- **CSV 导入（本地解析）**：CSV 文件由 `BackupImportPolicy.parseExternalCsv` 直接解析，无需网络

两条路径共享统一的合并管线：`BackupImportPolicy.mergeRecordImport`，通过语义分类匹配、去重和事务写入完成导入。

## 2. 架构

```
┌───────────────────────────────────────────────────────────┐
│                     SmartImportScreen                      │
│  配置区（API URL/Model/Key）+ 输入区（文本/文件）+ 预览确认  │
└──────────────┬────────────────────────────────────────────┘
               │
┌──────────────▼────────────────────────────────────────────┐
│              SmartImportViewModel                           │
│  saveConfig / selectImportFile / convertWithAi              │
│  importPendingEnvelope                                      │
└──────────────┬────────────────────────────────────────────┘
               │
┌──────────────▼────────────────────────────────────────────┐
│            SmartImportRepository                            │
│  convertTextToEnvelope / convertFileToEnvelope              │
│  mergeSmartImportEnvelopes                                  │
│                                                             │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐ │
│  │ Stage A     │  │ Stage B      │  │ Stage C           │ │
│  │ CSV 本地解析│  │ Responses API│  │ Chat Completions  │ │
│  │ (无需 AI)   │  │ 文件上传     │  │ 文本分块          │ │
│  └──────┬──────┘  └──────┬───────┘  └───────┬───────────┘ │
│         └────────────────┼───────────────────┘             │
└──────────────┬───────────┘────────────────────────────────┘
               │
┌──────────────▼────────────────────────────────────────────┐
│           SmartImportPolicy                                │
│  parseAiResponseToEnvelope                                 │
│  extractJsonPayload / parseRecords / normalizeType         │
│  ensureLedger / ensureCategory / ensureAccount             │
└──────────────┬────────────────────────────────────────────┘
               │
┌──────────────▼────────────────────────────────────────────┐
│        BackupImportPolicy (合并管线)                        │
│  buildRecordImportPlan                                     │
│  ├─ 账本映射（按名称）                                      │
│  ├─ 分类映射（CategorySemanticPolicy 语义匹配）              │
│  ├─ 账户映射（按 ledgerId+名称）                            │
│  └─ 记录合并（指纹去重 + 余额调整）                          │
└──────────────┬────────────────────────────────────────────┘
               │
┌──────────────▼────────────────────────────────────────────┐
│          BackupRepository                                  │
│  previewImportRecordsFromEnvelope                           │
│  importRecordsFromEnvelope (安全备份 → Room 事务写入)        │
└───────────────────────────────────────────────────────────┘
```

## 3. 智能导入（AI 辅助）

### 3.1 配置

| 配置项 | 默认值 | 存储方式 |
|--------|--------|----------|
| API Base URL | `https://api.openai.com/v1` | EncryptedSharedPreferences |
| Model | `gpt-4o-mini` | EncryptedSharedPreferences |
| API Key | — | EncryptedSharedPreferences (AES256_GCM) |

存储使用 AndroidX Security 的 `EncryptedSharedPreferences`：
- Master Key：`AES256_GCM` key scheme
- Key 加密：`AES256_SIV`
- Value 加密：`AES256_GCM`
- Preferences 名：`"smart_import_config"`
- 若硬件 Keystore 不可用，`secureStorageAvailable = false`，UI 提示用户

### 3.2 输入方式

**文本模式**：
- 用户直接粘贴账单文本
- 最大 100,000 字符
- 发送到 `/chat/completions` 端点

**文件模式**：
- 支持格式：`txt`, `csv`, `json`
- 最大 10 MB
- 三级策略：

```
文件输入
  │
  ├─ Stage A：CSV 本地解析
  │  检测 CSV（扩展名或 MIME）→ BackupImportPolicy.parseExternalCsv
  │  成功则直接返回，不调用 AI
  │
  ├─ Stage B：Responses API 文件上传
  │  POST /files (multipart, purpose="user_data") → file_id
  │  POST /responses (input_file content) → 结果
  │
  └─ Stage C：Chat Completions 文本回退
     若 Stage B 返回 404 → 读取文件为 UTF-8 文本
     超过 80,000 字符时分块（保留 CSV 表头）
     每块独立发送到 /chat/completions
```

### 3.3 AI 系统提示词

AI 被要求返回 JSON 格式，包含 `records` 数组，每条记录：

| 字段 | 类型 | 说明 |
|------|------|------|
| `date` | `String?` | `yyyy-MM-dd`，空则用当天 |
| `type` | `String` | `"EXPENSE"` 或 `"INCOME"` |
| `amount` | `Double` | 始终为正数 |
| `category` | `String` | 一级分类名称 |
| `categorySemantic` | `String?` | 英文语义标签（如 `food`, `transport`） |
| `subCategory` | `String?` | 二级分类名称 |
| `subCategorySemantic` | `String?` | 二级分类语义标签 |
| `account` | `String?` | 账户名称 |
| `ledger` | `String?` | 账本名称 |
| `note` | `String?` | 备注 |
| `icon` | `String?` | Material icon 名称 |

### 3.4 AI 响应解析

`SmartImportPolicy.parseAiResponseToEnvelope` 流程：

1. **提取 JSON**：优先查找 ` ```json ``` ` 代码块，否则扫描首个 `{`/`[` 到末尾 `}`/`]`
2. **解析记录列表**：尝试 `JsonArray`，再查找 `records` 或 `data` 键
3. **逐条构建**：
   - 类型规范化：`expense`/`支出`/`花费`/`消费` → `EXPENSE`；`income`/`收入`/`进账` → `INCOME`
   - 日期解析：`yyyy-MM-dd` → `LocalDate.parse`，失败用当天
   - 账本创建：按规范化名称去重，默认 `"我的账本"`
   - 分类创建：支持父子层级，用 `CategorySemanticPolicy.chooseIcon` 选图标
   - 账户创建：按名称推断类型（支付宝/微信/信用卡/银行卡/现金/投资）
   - 记录创建：自增 ID，epoch-day 日期，UUID syncId

### 3.5 大文件分块

当文本超过 80,000 字符时：
- 按行分割
- CSV 表头在每个分块中保留
- 每块标注 `"文件第 X/Y 段"`
- 各块独立调用 AI
- `mergeSmartImportEnvelopes` 合并多个响应：
  - 全局唯一顺序 ID
  - 分类 `parentId` 引用重映射

### 3.6 隐私保护

转换前弹出确认对话框："数据将发送到您配置的 AI 服务"，用户确认后才执行。

## 4. CSV 导入（本地解析）

### 4.1 分隔符检测

`detectDelimiter` 采样首个非空行，统计各候选分隔符出现次数（引号外），选最多的：

| 分隔符 | 字符 |
|--------|------|
| 逗号 | `,` |
| 分号 | `;` |
| 制表符 | `\t` |
| 竖线 | `\|` |
| 全角逗号 | `，` |

### 4.2 列头识别

所有表头规范化处理（小写、去空格/下划线/连字符/括号）：

| 逻辑列 | 必需 | 别名示例 |
|--------|------|----------|
| 日期 | 是 | `日期`, `date`, `time`, `记账时间`, `交易日期`, `timestamp` |
| 分类 | 是 | `分类`, `category`, `一级分类`, `类目`, `分类路径` |
| 金额 | 是（三选一） | `金额`, `amount`, `money`, `交易金额` |
| 收入金额 | 替代 | `收入金额`, `入账金额`, `incomeamount` |
| 支出金额 | 替代 | `支出金额`, `出账金额`, `expenseamount` |
| 类型 | 否 | `类型`, `type`, `收支类型` |
| 子分类 | 否 | `子分类`, `subcategory`, `二级分类` |
| 备注 | 否 | `备注`, `note`, `memo`, `remark`, `description` |
| 账户 | 否 | `账户`, `account`, `支付方式`, `钱包` |
| 账本 | 否 | `账本`, `ledger`, `book` |
| 流水号 | 否 | `uuid`, `syncid`, `id`, `流水号` |
| 时间戳 | 否 | `时间戳`, `timestamp`, `createdat` |

### 4.3 金额解析

`resolveAmount` 三种策略：

1. 单一 `amount` 列 → 直接使用
2. 分列 `incomeAmount` / `expenseAmount` → 非零值暗示类型
3. 仅一列非零时使用该列

`parseAmountOrNull` 清理：
- 去除货币符号：`¥`, `￥`, `元`
- 去除千分位：`,`
- 括号表示负数：`(100)` → `-100`
- 去除非断行空格、全角运算符

### 4.4 日期解析

支持格式（按优先级）：

| 格式 | 示例 |
|------|------|
| `yyyy-MM-dd` | `2026-05-02` |
| `yyyy/M/d` | `2026/5/2` |
| `yyyy年M月d日` | `2026年5月2日` |
| `yyyy.MM.dd` | `2026.05.02` |
| `yyyyMMdd` | `20260502` |
| `M/d/yyyy` | `5/2/2026` |
| `MM/dd/yyyy` | `05/02/2026` |
| 以上 + `HH:mm:ss` 或 `HH:mm` | `2026-05-02 14:30:00` |
| `ISO_LOCAL_DATE_TIME` | `2026-05-02T14:30:00` |
| `OffsetDateTime` | `2026-05-02T14:30:00+08:00` |
| Epoch day (5-6 位数字) | `19845` |
| Epoch seconds (7-10 位) | `1746172200` |
| Epoch millis (11-13 位) | `1746172200000` |

### 4.5 类型推断

优先级从高到低：

1. 显式类型列（匹配 `EXPENSE_TYPE_ALIASES` / `INCOME_TYPE_ALIASES`）
2. 收入/支出金额列暗示
3. 分类名暗示（`工资`, `奖金`, `退款` → INCOME）
4. 负数金额 → EXPENSE
5. 默认 → EXPENSE

### 4.6 分类路径解析

`resolveCategoryPath` 处理层级分类：

- 若 `subCategory` 列存在且非空 → `category` 为父，`subCategory` 为子
- 否则按路径分隔符拆分：`/`, `／`, `>`, `＞`, `|`, `｜`
  - 含分隔符 → 首段为父，末段为子
  - 无分隔符 → 单级分类

## 5. 语义分类匹配

### 5.1 匹配瀑布

`CategorySemanticPolicy.resolveExistingCategory` 四级匹配：

```
① 同类型 + 同父级 + 精确规范化名称匹配
   ↓ 未命中
② 同类型 + 精确规范化名称匹配（忽略父级，仅当目标为顶级分类时）
   ↓ 未命中
③ 同类型 + 同父级 + 语义标签匹配
   ↓ 未命中
④ 同类型 + 语义标签匹配（忽略父级，仅当目标为顶级分类时）
   ↓ 未命中
→ CREATE_NEW
```

所有匹配要求唯一性（`singleOrNull`），避免歧义映射。

### 5.2 名称规范化

`normalize()` 处理：小写、去 BOM、去空格/下划线/连字符/括号/斜杠/冒号/点号。

### 5.3 语义标签

`semanticLabelFor` 两级查找：

1. **别名组**：中英文名称映射到标准键
   - `早餐`/`早饭`/`breakfast` → `breakfast`
   - `打车`/`出租车`/`taxi` → `taxi`
   - `房租`/`水电`/`housing` → `housing`
   - 支持子串匹配：`normalizedName.contains(normalize(alias))`

2. **语义标签别名**：AI 返回的 `categorySemantic` 值映射
   - `food`/`meal`/`dining` → `food`
   - `transport`/`commute`/`travel` → `transport`

### 5.4 图标选择

`chooseIcon` 优先级：

1. AI 提议的图标（在 `supportedIcons` 中且非通用兜底图标）
2. 语义标签 → `semanticIconByKey` 查找
3. 兜底：收入用 `payments`，子分类用 `sell`，其他用 `category`

### 5.5 已定义的语义别名组

| 标签 | 中文别名 | 英文别名 |
|------|----------|----------|
| `breakfast` | 早餐、早饭 | breakfast |
| `salary` | 工资、薪资 | salary |
| `taxi` | 打车、出租车 | taxi |
| `housing` | 房租、水电、物业 | housing |
| `refund` | 退款、退货 | refund |
| `investment` | 投资、理财 | investment |
| `daily` | 日用、日用品 | daily |
| `transport` | 交通、公交、地铁 | transport |
| `food` | 餐饮、吃饭、外卖 | food |
| `shopping` | 购物、网购 | shopping |
| `medical` | 医疗、看病 | medical |
| `education` | 教育、培训 | education |
| `entertainment` | 娱乐、游戏 | entertainment |
| `income` | 收入、进账 | income |

## 6. 合并管线

### 6.1 RecordImportPlan 构建

`buildRecordImportPlan(current, incoming)` 统一处理所有导入源：

**Step 1 — 账本映射**
- 按规范化名称匹配现有账本
- 未匹配 → 新建（自增 ID，颜色 `0xFF4CAF50`，类型 `PERSONAL`）
- 构建 `ledgerIdMap[incomingId] → targetId`

**Step 2 — 分类映射**
- 排序：父分类优先处理
- 对每个传入分类调用 `CategorySemanticPolicy.resolveExistingCategory`
- 匹配成功 → `REUSE_EXISTING`，记录目标 ID
- 匹配失败 → `CREATE_NEW`，分配新 ID
- 所有映射记录到 `categoryMappings` 供预览展示

**Step 3 — 账户映射**
- 按 `(targetLedgerId, normalizedName)` 匹配
- 未匹配 → 新建（推断类型/图标/颜色）

**Step 4 — 记录合并**
- 映射所有 ID（账本/分类/账户）
- 缺失 syncId → 生成 UUID
- 计算记录指纹：`ledgerId|type|categoryId|accountId|date|normalizedNote|normalizedAmount`
- 指纹已存在 → 跳过（`duplicateRecords++`）
- 指纹不存在 → 追加到合并记录 + 调整账户余额

### 6.2 账户类型推断

`defaultAccountMeta` 按名称模式推断：

| 匹配模式 | 类型 | 图标 | 颜色 |
|----------|------|------|------|
| 支付宝/alipay | ALIPAY | `alipay` | `0xFF1890FF` |
| 微信/wechat | WECHAT | `wechat` | `0xFF07C160` |
| 信用卡/credit | CREDIT_CARD | `credit_card` | `0xFFFF5722` |
| 银行/bank/储蓄 | BANK | `account_balance` | `0xFF2196F3` |
| 现金/cash | CASH | `account_balance_wallet` | `0xFF4CAF50` |
| 投资/理财 | INVESTMENT | `savings` | `0xFFFF9800` |
| 其他 | OTHER | `more_horiz` | `0xFF9E9E9E` |

### 6.3 记录去重

指纹算法：`ledgerId|type|categoryId|accountId|date|normalizedNote|normalizedAmount`

- `normalizedNote`：小写 + 去空格
- `normalizedAmount`：保留两位小数
- 匹配到已有指纹 → 视为重复，不导入

## 7. 预览与确认

### 7.1 预览数据

`BackupRecordImportPreview` 展示：

| 字段 | 说明 |
|------|------|
| `recordsToImport` | 待导入记录数 |
| `duplicateRecords` | 重复跳过数 |
| `categoriesToCreate` | 待新建分类数 |
| `accountsToCreate` | 待新建账户数 |
| `ledgersToCreate` | 待新建账本数 |
| `categoryMappings` | 分类映射列表 |

### 7.2 分类映射展示

每条映射显示：
- 源名称 → 目标名称
- 动作：`REUSE_EXISTING`（复用已有）或 `CREATE_NEW`（新建）
- 原因：`同名分类` 或 `同义分类`
- 图标

### 7.3 确认后写入

```
用户确认
  │
  ▼
BackupRepository.importRecordsFromEnvelope(incoming)
  │
  ├─ createSafetyBackup()  → 最多保留 3 份
  │
  ├─ buildRecordImportPlan(current, incoming)
  │    → mergedEnvelope
  │
  └─ restoreEnvelope(mergedEnvelope)
       └─ Room.withTransaction {
            delete all → insert all (依赖序：账本→分类→账户→预算→模板→记录)
            DAV 密码合并
            主题模式恢复
          }
```

## 8. 安全与限制

| 项目 | 限制 |
|------|------|
| 文本输入最大长度 | 100,000 字符 |
| 文件最大大小 | 10 MB |
| 文本分块阈值 | 80,000 字符 |
| 支持文件扩展名 | `txt`, `csv`, `json` |
| 安全备份保留数 | 3 份 |
| API Key 存储 | EncryptedSharedPreferences (AES256) |

## 9. 用户流程

```
设置页 → 智能导入
  │
  ├─ 首次使用：配置 API URL / Model / API Key → 保存
  │
  ├─ 输入数据：
  │  ├─ 粘贴文本（支持银行账单、支付宝账单等格式）
  │  └─ 选择文件（CSV / TXT / JSON）
  │
  ├─ 点击"智能转换"：
  │  ├─ 隐私确认弹窗 → 用户同意
  │  ├─ CSV 文件 → 本地解析（不调用 AI）
  │  └─ 其他 → AI 转换（显示加载状态）
  │
  ├─ 预览结果：
  │  ├─ 查看待导入记录数、重复数
  │  ├─ 查看待创建分类/账户/账本
  │  └─ 查看分类映射（复用 vs 新建）
  │
  └─ 确认导入：
     ├─ 安全备份 → 合并写入
     └─ 成功提示
```

## 10. 扩展点

| 方向 | 现状 | 扩展建议 |
|------|------|----------|
| 新增第三方格式 | 仅 CSV | 在 `parseExternalCsv` 中添加新表头别名 |
| 新增 AI 提示词 | 固定系统提示词 | `SmartImportRepository` 第 431 行可扩展 |
| 新增语义标签 | 14 组别名 | `CategorySemanticPolicy` 第 138 行添加新组 |
| 新增账户类型推断 | 6 种模式 | `defaultAccountMeta` 列表添加新模式 |
| 新增日期格式 | 12+ 种 | `parseDate` 添加新 formatter |
