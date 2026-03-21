# 石榴果宠物屋 - 数据库设计文档（v2）

本文档基于 `backend/src/models` 中的 Sequelize 模型整理，描述 v2 版本的表结构与关系。v2 相比 v1 主要新增了学生端账号注册所需字段。

## 一、数据表结构

### 1. 教师用户表 (`users`)
存储系统中的教师账户信息。
- **id**: INTEGER, 主键, 自增
- **username**: STRING(50), 必填, 唯一（登录用户名）
- **password_hash**: STRING(255), 必填（加密后的密码哈希，通过 bcrypt 存储）
- **activation_code**: STRING(100), 默认 `null`（激活码，用于验证教师身份）
- **is_activated**: BOOLEAN, 默认 `false`（是否已激活）
- **settings**: JSON, 默认 `{}`（教师自定义设置，可用于存储界面偏好等）
- **createdAt / updatedAt**: DATE (Sequelize 自动生成)

### 2. 班级表 (`classes`)
存储教师创建的班级设置及主题信息。
- **id**: INTEGER, 主键, 自增
- **user_id**: INTEGER, 必填（外键，关联教师：`users.id`）
- **name**: STRING(100), 必填, 默认 `'默认班级'`
- **system_name**: STRING(100), 默认 `'石榴果宠物屋'`（前台系统显示的自定义名称）
- **theme**: STRING(50), 默认 `'pink'`（主题风格色）
- **growth_stages**: JSON, 默认 `[0, 5, 10, 20, 30, 45, 60, 75, 90, 100]`（宠物成长各阶段所需食物数量）
- **sort_order**: INTEGER, 默认 `0`（拖拽排序权重）

### 3. 学生表 (`students`)
存储班级中的学生及对应的虚拟宠物状态（积分由 `food_count` 承载）。
- **id**: INTEGER, 主键, 自增
- **class_id**: INTEGER, 必填（外键，关联班级：`classes.id`）
- **name**: STRING(50), 必填（学生姓名）
- **pet_type**: STRING(50), 默认 `null`（当前宠物类型）
- **pet_name**: STRING(50), 默认 `null`（宠物昵称）
- **food_count**: INTEGER, 默认 `0`（食物数量/积分）
- **badges**: JSON, 默认 `[]`（已获得徽章列表）
- **sort_order**: INTEGER, 默认 `0`（显示顺序）
- **group_id**: INTEGER, 默认 `null`（外键，关联小组：`groups.id`）

### 4. 学生账号表 (`student_accounts`)
存储学生端登录与注册信息（v2 重点新增）。
- **id**: INTEGER, 主键, 自增
- **student_id**: INTEGER, 必填, 唯一（外键，关联学生：`students.id`）
- **class_id**: INTEGER, 必填（外键，关联班级：`classes.id`）
- **username**: STRING(50), 必填, 唯一（学生端登录用户名）
- **invite_code**: STRING(20), 必填, 唯一（邀请码，学生注册的唯一 ID）
- **phone**: STRING(20), 可空（手机号，用于判断是否已完成注册）
- **password_hash**: STRING(255), 必填（加密后的密码哈希，通过 bcrypt 存储）

### 5. 小组表 (`groups`)
用于对班级内的学生进行分组管理。
- **id**: INTEGER, 主键, 自增
- **class_id**: INTEGER, 必填（外键，隶属于班级：`classes.id`）
- **name**: STRING(50), 必填（小组名称）
- **sort_order**: INTEGER, 默认 `0`（显示顺序）

### 6. 积分规则表 (`score_rules`)
定义教师进行加减分的行为规则库。
- **id**: INTEGER, 主键, 自增
- **class_id**: INTEGER, 必填（外键，隶属于班级：`classes.id`）
- **name**: STRING(50), 必填（规则名称）
- **icon**: STRING(50), 默认 `'⭐'`（前端展示图标）
- **value**: INTEGER, 必填（加减分值）
- **sort_order**: INTEGER, 默认 `0`（排序权重）

### 7. 历史流水表 (`history`)
记录积分变动与操作流水。
- **id**: INTEGER, 主键, 自增
- **class_id**: INTEGER, 必填（外键，关联班级：`classes.id`）
- **student_id**: INTEGER, 必填（外键，关联学生：`students.id`）
- **rule_id**: INTEGER, 默认 `null`（关联规则：`score_rules.id`）
- **rule_name**: STRING(50), 默认 `null`（规则名称快照）
- **value**: INTEGER, 默认 `0`（积分增量或减量）
- **type**: ENUM `('score', 'graduate', 'exchange', 'revoke')`, 默认 `'score'`
- **is_revoked**: BOOLEAN, 默认 `false`

### 8. 商城物品表 (`shop_items`)
配置学生可兑换的奖励物品。
- **id**: INTEGER, 主键, 自增
- **class_id**: INTEGER, 必填（外键，隶属于班级：`classes.id`）
- **name**: STRING(50), 必填（商品名称）
- **description**: STRING(200), 默认 `''`
- **icon**: STRING(50), 默认 `'🎁'`
- **price**: INTEGER, 必填, 默认 `1`
- **stock**: INTEGER, 默认 `-1`（`-1` 表示无限量）

### 9. 兑换记录表 (`exchange_records`)
记录学生兑换物品的流水。
- **id**: INTEGER, 主键, 自增
- **class_id**: INTEGER, 必填（外键，关联班级：`classes.id`）
- **student_id**: INTEGER, 必填（外键，关联学生：`students.id`）
- **item_id**: INTEGER, 默认 `null`（关联物品：`shop_items.id`）
- **item_name**: STRING(50), 必填（物品名称快照）
- **cost**: INTEGER, 必填（兑换扣减值）

### 10. 激活码授权表 (`licenses`)
系统激活码颁发与使用记录。
- **id**: INTEGER, 主键, 自增
- **code**: STRING(100), 必填, 唯一（激活码）
- **is_used**: BOOLEAN, 默认 `false`
- **used_by**: INTEGER, 默认 `null`（外键，关联教师：`users.id`）
- **used_at**: DATE, 默认 `null`

---

## 二、实体关系（ER 关系）

- **`User` (教师) — `Class` (班级)**：1 对多
- **`Class` — `Student` / `Group` / `ScoreRule` / `ShopItem`**：1 对多
- **`Group` — `Student`**：1 对多
- **`Student` — `StudentAccount`**：1 对 1
- **`Class` / `Student` — `History`**：1 对多
- **`Student` — `ExchangeRecord`**：1 对多

---

## 三、v2 相比 v1 的变化
- `student_accounts` 新增 `invite_code`（唯一邀请码）
- `student_accounts` 新增 `phone`（手机号，完成注册标识）
- 学生端登录/注册基于 `invite_code` 完成账号绑定

