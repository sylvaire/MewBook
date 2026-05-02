# Archived Implementation Plan

> 状态：历史参考。此计划记录的是早期“分类图标错乱、分类网格、虚拟产品分类”工作，不是当前活跃任务。当前项目状态见 `.planning/PROJECT.md` 与 `.planning/STATE.md`。

[Overview]
修复MewBook记账应用中分类记录界面的图标错乱问题，并将分类布局重新设计为更合理、统一、整齐的网格布局。同时添加虚拟产品相关的一级分类和二级分类。

本次修改涉及3个核心文件的改动，通过优化 `CenteredCategoryFlowRow` 布局逻辑、完善图标映射、以及增加虚拟产品分类来提升用户体验。修改遵循最小化变更原则，确保不影响现有功能。

[Types]

数据模型无变化，但需确保分类关联关系正确：
- `Category.parentId`: Long? - 父分类ID，null表示一级分类，非null表示二级分类
- 分类名称自动匹配规则：通过预定义的关系映射表建立二级分类与一级分类的关联

分类名称匹配规则（需更新到 `AddEditRecordSheet.kt`）：
```
餐饮 -> 早餐、午餐、晚餐、零食、下午茶、外卖、水果、奶茶、咖啡、茶饮、肉类、蔬菜
交通 -> 公交、地铁、打车、油费、停车、火车、飞机
购物 -> 服装、数码、日用品、化妆品、母婴、家电
居住 -> 房租、水电费、物业费、燃气、话费
娱乐 -> 电影、游戏、旅游、健身、演唱会、ktv
通讯 -> 手机话费、宽带费
运动健身 -> 健身房、运动装备、游泳
宠物 -> 宠物食品、宠物医疗、宠物用品
人情往来 -> 红包、礼物、请客
书籍文具 -> 书籍、文具、电子书
医疗 -> 药品、门诊、体检
教育 -> 培训、课程、教材
烟酒 -> 香烟、酒类
虚拟产品 -> 游戏充值、会员订阅、软件订阅、直播打赏、虚拟货币
```

[Files]

### Modify Files:

1. **app/src/main/java/com/mewbook/app/ui/screens/add/AddEditRecordSheet.kt**
   - 扩展 `isRelatedCategory` 函数中的分类关联映射，新增虚拟产品相关分类
   - 优化 `CenteredCategoryFlowRow` 布局组件，改为真正的网格布局
   - 确保一级分类和二级分类使用统一的网格布局样式

2. **app/src/main/java/com/mewbook/app/ui/components/RecordItem.kt**
   - 补充 `getIconForCategory` 函数中缺失的图标映射
   - 添加新图标映射：`inbox`（虚拟产品）、`redeem`（会员订阅）、`sports_esports`（游戏充值）、`cloud`（软件订阅）等
   - 修复现有映射中的不一致问题

3. **app/src/main/java/com/mewbook/app/domain/model/Category.kt**
   - 添加"虚拟产品"一级分类
   - 添加虚拟产品相关的二级分类（游戏充值、会员订阅、软件订阅、直播打赏、虚拟货币）
   - 为所有二级分类添加正确的 `parentId` 关联

[Functions]

### Modified Functions:

1. **`isRelatedCategory` in AddEditRecordSheet.kt**
   - 更新分类关联映射表，添加虚拟产品相关分类
   - 新增关联规则：虚拟产品 -> 游戏充值、会员订阅、软件订阅、直播打赏、虚拟货币

2. **`CenteredCategoryFlowRow` in AddEditRecordSheet.kt**
   - 重写布局逻辑，改为真正的网格布局
   - 每行显示固定数量的项目（每行4-5个）
   - 所有行居中对齐，而非最后一行动态对齐
   - 统一每个网格项的宽度和高度

3. **`getIconForCategory` in RecordItem.kt**
   - 补充缺失图标：`cookie`、`house`、`eco`、`inbox`、`cloud_download` 等
   - 规范化图标名称映射

[Classes]

无新增或删除类，仅修改现有组件的内部实现。

[Dependencies]

无新增依赖，使用项目现有的 Material Icons Extended。

[Testing]

1. **手动测试要点：**
   - 验证一级分类网格布局显示正确
   - 验证二级分类网格布局显示正确
   - 验证选中状态下的视觉反馈
   - 验证虚拟产品相关分类显示正确
   - 验证切换支出/收入时分类正确更新
   - 验证分类图标显示正确（无默认兜底图标）

2. **兼容性测试：**
   - 测试不同屏幕尺寸下的布局表现
   - 测试分类数量变化时的布局适应性

[Implementation Order]

1. **Step 1**: 更新 `Category.kt`，添加虚拟产品分类和关联关系
2. **Step 2**: 补充 `RecordItem.kt` 中的图标映射
3. **Step 3**: 重构 `AddEditRecordSheet.kt` 中的 `CenteredCategoryFlowRow` 布局组件
4. **Step 4**: 扩展 `isRelatedCategory` 函数的分类关联映射
5. **Step 5**: 运行应用验证修改效果
