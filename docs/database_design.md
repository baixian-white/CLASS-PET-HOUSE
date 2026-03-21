# 石榴果宠物屋 - 数据库设计文档

本文档基于 `backend/src/models` 中的 Sequelize 模型生成了系统的数据关系和各表的字段定义。

## 一、 数据表结构

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
- **growth_stages**: JSON, 默认 `[0, 5, 10, 20, 30, 45, 60, 75, 90, 100]`（宠物成长各个阶段所需的食物数量配置）
- **sort_order**: INTEGER, 默认 `0`（拖拽排序权重）

### 3. 学生表 (`students`)
存储班级中的学生及对应的虚拟宠物状态（积分由 `food_count` 承载）。
- **id**: INTEGER, 主键, 自增
- **class_id**: INTEGER, 必填（外键，关联班级：`classes.id`）
- **name**: STRING(50), 必填（学生姓名）
- **pet_type**: STRING(50), 默认 `null`（当前宠物的类型或品种）
- **pet_name**: STRING(50), 默认 `null`（学生给宠物起的昵称）
- **food_count**: INTEGER, 默认 `0`（宠物当下的食物数量，等同于传统系统中的可用积分）
- **badges**: JSON, 默认 `[]`（学生已获得的荣誉徽章列表集合）
- **sort_order**: INTEGER, 默认 `0`（拖拽或指定顺序号）
- **group_id**: INTEGER, 默认 `null`（外键，关联小组：`groups.id`）

### 4. 学生账号表 (`student_accounts`)
存储学生的登入凭证，便于学生通过独立端进入。
- **id**: INTEGER, 主键, 自增
- **student_id**: INTEGER, 必填, 唯一（外键，关联具体的学生：`students.id`，呈 1:1 关系）
- **class_id**: INTEGER, 必填（外键，冗余关联所在班级以防跨班或查询优化：`classes.id`）
- **username**: STRING(50), 必填, 唯一（学生端的唯一登录用户名）
- **password_hash**: STRING(255), 必填（加密密码哈希，同样采用 bcrypt 处理）

### 5. 小组表 (`groups`)
用于对班级内的部分学生进行编组及结构化管理。
- **id**: INTEGER, 主键, 自增
- **class_id**: INTEGER, 必填（外键，隶属于班级：`classes.id`）
- **name**: STRING(50), 必填（小组名称）
- **sort_order**: INTEGER, 默认 `0`（显示顺序排序权值）

### 6. 积分规则表 (`score_rules`)
定义教师进行加减分的行为规则库。
- **id**: INTEGER, 主键, 自增
- **class_id**: INTEGER, 必填（外键，隶属于各班级自定义：`classes.id`）
- **name**: STRING(50), 必填（规则名称定义，如“课堂发言积极”）
- **icon**: STRING(50), 默认 `'⭐'`（前端显示的 Icon 字符代码或类名）
- **value**: INTEGER, 必填（奖励或惩罚的分数规则值，对应增扣多少“食物”）
- **sort_order**: INTEGER, 默认 `0`（排序权重）

### 7. 历史流水表 (`history`)
用于追溯各项行为记录和流水，用于后期审计和操作回撤功能。
- **id**: INTEGER, 主键, 自增
- **class_id**: INTEGER, 必填（外键，关联班级：`classes.id`）
- **student_id**: INTEGER, 必填（外键，关联涉事学生：`students.id`）
- **rule_id**: INTEGER, 默认 `null`（如果是由规则动作触发，记录涉及的 `score_rules.id`）
- **rule_name**: STRING(50), 默认 `null`（创建时快照保存规则名称，以防原规则被删除后无法追溯）
- **value**: INTEGER, 默认 `0`（积分变更时的增量或减量值）
- **type**: ENUM `('score', 'graduate', 'exchange', 'revoke')`, 默认 `'score'`（事件类型。分别为：积分变动、宠物满级毕业、商城道具兑换、事件操作已被撤销）
- **is_revoked**: BOOLEAN, 默认 `false`（判断本条流水本身是否已被教师主动撤销过（红冲机制））

### 8. 商城物品表 (`shop_items`)
配置学生使用已有食物（积分）可购买的各类激励系统物品。
- **id**: INTEGER, 主键, 自增
- **class_id**: INTEGER, 必填（外键，隶属于班级：`classes.id`）
- **name**: STRING(50), 必填（商品或实物奖励名称，如“免写作业卡”）
- **description**: STRING(200), 默认 `''`（商品说明介绍）
- **icon**: STRING(50), 默认 `'🎁'`（前台商城里展示的图标）
- **price**: INTEGER, 必填, 默认 `1`（兑换当前物品需消耗的宠物食物数量）
- **stock**: INTEGER, 默认 `-1`（当前商品库存限制，若定为 `-1` 则代表无限量不扣减）

### 9. 兑换记录表 (`exchange_records`)
仅记录积分换物成功所产生的实质性日志。
- **id**: INTEGER, 主键, 自增
- **class_id**: INTEGER, 必填（外键，关联班级：`classes.id`）
- **student_id**: INTEGER, 必填（外键，发起兑换操作的学生：`students.id`）
- **item_id**: INTEGER, 默认 `null`（所兑换的物品ID：`shop_items.id`）
- **item_name**: STRING(50), 必填（快照留档，发生兑换时所换物品的名称，以防后续商品下架或改名）
- **cost**: INTEGER, 必填（本次兑换实质上锁定的开销，即当时的商品定价扣减原值）

### 10. 激活码授权表 (`licenses`)
系统激活码颁发以及使用轨迹审计。
- **id**: INTEGER, 主键, 自增
- **code**: STRING(100), 必填, 唯一（具体的 CDKey 或系统激活长段字符串）
- **is_used**: BOOLEAN, 默认 `false`（当前激活码是否已被消耗挂起）
- **used_by**: INTEGER, 默认 `null`（外键，关联使用该授权码被激活上线的教师：`users.id`）
- **used_at**: DATE, 默认 `null`（实际消费授权码时记录的对应日期时间）

---

## 二、 实体关系 (ER 关系图梳理)

*   **`User` (教师) — `Class` (班级)**  
    关系：1对多。一个 `User` 可以在其名下掌管并创建多个隔离的 `Class`。
*   **`Class` (班级) — `Student` (学生)** / **`Group` (分组)** / **`ScoreRule` (规则)** / **`ShopItem` (商品)**  
    关系：均为 1对多。以上四类基础维护实体的外键统一挂在对应的一个 `Class` ID 之下，实现不同班级之间数据的彻底孤立及相互闭环。
*   **`Group` (分组) — `Student` (学生)**  
    关系：1对多。每个小组内囊括多名学生，目前设定上一个学生同一时间只挂靠于一个组别。
*   **`Student` (学生) — `StudentAccount` (独立账号)**  
    关系：1对1。一个学生可激活分配一个对应的账号用于脱离教师主视图的前台登录操作。
*   **`User` / `Class` / `Student` — `History` (历史流水)**  
    关系：历史记录既归属于班级层面视图下，同时也关联落实到了具体的某位 `Student` 身上，充当了关系明细记录节点。
*   **`Student` — `ExchangeRecord` (兑换记录)**  
    关系：1对多。学生发起购物产生的历史小票单据列表。
