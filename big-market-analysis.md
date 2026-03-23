# Big-Market 营销抽奖平台——领域设计与数据库设计分析文档

> 基于源码深度阅读，整理日期：2026-03-23

---

## 目录

1. [项目概览](#1-项目概览)
2. [模块结构](#2-模块结构)
3. [整体架构思想：DDD 分层](#3-整体架构思想ddd-分层)
4. [领域设计思想详解](#4-领域设计思想详解)
5. [核心设计模式汇总](#5-核心设计模式汇总)
6. [基础设施层设计](#6-基础设施层设计)
7. [数据库设计思想详解](#7-数据库设计思想详解)
8. [领域与数据库的映射关系](#8-领域与数据库的映射关系)
9. [关键流程梳理](#9-关键流程梳理)

---

## 1. 项目概览

`big-market` 是一个大型营销抽奖平台，支持：
- 多策略抽奖（概率权重、黑名单过滤、积分门槛权重）
- 活动额度管理（总次数 / 日次数 / 月次数）
- 奖品多类型发放（积分、实物等，策略模式扩展）
- 行为返利（签到、下单等行为触发积分回馈）
- 积分账户体系（充值、消费、流水）
- 可靠消息投递（本地消息表 Outbox 模式）

**技术栈：**

| 组件 | 说明 |
|------|------|
| 语言 | Java 8 |
| 框架 | Spring Boot 2.7.12 |
| ORM | MyBatis + 分库分表中间件 db-router |
| 缓存 | Redis（Redisson） |
| 搜索 | Elasticsearch |
| 消息 | MQ（EventPublisher 抽象，解耦具体 MQ 实现） |
| 数据库 | MySQL，分库（big_market / big_market_01 / big_market_02） |
| 配置中心 | Nacos（DCC 动态配置） |
| 任务调度 | XXL-Job |

---

## 2. 模块结构

```
big-market/
├── big-market-api/            # API 接口定义层（HTTP 接口声明、DTO）
├── big-market-app/            # 应用启动层（Spring Boot 入口、配置、AOP）
├── big-market-domain/         # 核心领域层（业务逻辑的唯一来源）
├── big-market-trigger/        # 触发器层（HTTP Controller、MQ Consumer）
├── big-market-infrastructure/ # 基础设施层（仓储实现、DAO、Redis、ES）
└── big-market-types/          # 公共类型层（枚举、异常、常量）
```

**依赖方向（严格单向，不允许反向）：**

```
trigger ──▶ domain ◀── infrastructure
   │           │
  api        types
   │
  app（组装所有模块，Spring Boot 启动）
```

`domain` 层不依赖任何基础设施实现，通过 Repository 接口由 `infrastructure` 注入，
这是 DDD 依赖倒置原则（DIP）的核心体现。

---

## 3. 整体架构思想：DDD 分层

### 3.1 分层职责

| 层 | 包 | 职责 |
|----|----|------|
| Trigger 触发器层 | `big-market-trigger` | HTTP 接口、MQ 消费者、定时任务入口；只做参数校验与格式转换，不含业务逻辑 |
| Domain 领域层 | `big-market-domain` | 所有业务规则、聚合、实体、值对象、领域服务、领域事件 |
| Infrastructure 基础设施层 | `big-market-infrastructure` | Repository 接口实现、DAO、Redis、Elasticsearch、MQ 发布 |
| Types 公共层 | `big-market-types` | 枚举、常量、异常、公共事件基类 |
| API 接口层 | `big-market-api` | 对外暴露的 Feign/HTTP 接口定义及 DTO |

### 3.2 DDD 核心概念映射

| DDD 概念 | 本项目实现 |
|---------|----------|
| 聚合根（Aggregate） | `CreateQuotaOrderAggregate`、`CreatePartakeOrderAggregate`、`UserAwardRecordAggregate`、`TradeAggregate`、`BehaviorRebateAggregate` |
| 实体（Entity） | `ActivityEntity`、`StrategyAwardEntity`、`UserRaffleOrderEntity`、`CreditAccountEntity` 等 |
| 值对象（Value Object） | `ActivityStateVO`、`RuleLogicCheckTypeVO`、`OrderStateVO`、`TaskSendStateVO` 等 |
| 领域服务（Domain Service） | `AwardService`、`BehaviorRebateService`、`CreditAdjustService`、`AbstractRaffleStrategy` |
| 仓储接口（Repository） | `IStrategyRespository`、`IActivityRespository`、`ICreditRepository`、`IBehaviorRebateRespository` |
| 领域事件（Domain Event） | `ActivitySkuStockZeroMessageEvent`、`SendAwardMessageEvent`、`SendRebateMessageEvent`、`CreditAdjustSuccessMessageEvent` |

---

## 4. 领域设计思想详解

### 4.1 Strategy 抽奖策略领域

**包路径：** `cn.bugstack.domain.strategy`

这是系统最核心的领域，包含三层过滤机制，形成「前置责任链 + 后置决策树」的双重过滤架构。

#### 4.1.1 策略装配（Armory）

```
IStrategyArmory.assembleLotteryStrategy(strategyId)
    └─ 从库中读取策略下所有奖品及概率
    └─ 计算概率范围（如总概率 10000），生成随机散列表
    └─ 存入 Redis（key: STRATEGY_RATE_TABLE_KEY + strategyId）
```

**设计思想：** 将概率计算「预装配」到 Redis 中，抽奖时只需随机一个数字后查表，O(1) 完成，避免每次抽奖重新计算概率。

#### 4.1.2 前置过滤：责任链（Logic Chain）

```
AbstractLogicChain（责任链抽象，持有 next 指针）
    ├─ RuleBlacklistLogicChain   （黑名单拦截，命中直接返回兜底奖品）
    ├─ RuleWeightLogicChain      （积分权重，积分够用走权重池子）
    └─ DefaultLogicChain         （默认抽奖，走全量概率表随机）
```

`DefaultLogicChainFactory.openLogicChain(strategyId)` 根据策略配置的 `rule_models` 字段，
动态组装责任链。Spring 将所有 `ILogicChain` 实现注入为 Map，按 Bean 名称路由。

**责任链核心代码：**
```java
// AbstractLogicChain
private ILogicChain next;
public ILogicChain appendnext(ILogicChain chain) {
    this.next = chain;
    return next;
}
```

每个节点若不拦截则调用 `next().logic(userId, strategyId)` 放行，形成链式处理。

#### 4.1.3 后置过滤：决策树（Decision Tree）

责任链给出初始 awardId 后，进入决策树做进一步规则校验：

```
DecisionTreeEngine.process(userId, strategyId, awardId)
    从 rule_tree 根节点出发，遍历节点
    每个节点调用对应 ILogicTreeNode.logic()
        ├─ RuleLockLogicTreeNode   （抽奖次数锁，达到次数才解锁奖品）
        ├─ RuleStockLogicTreeNode  （库存校验，Redis 扣减库存）
        └─ RuleLuckAwardLogicTreeNode（兜底奖励，库存不足时返回兜底）
    根据节点返回的 RuleLogicCheckTypeVO（ALLOW/TAKE_OVER）决定走向下一节点
```

**树结构存储在数据库（rule_tree / rule_tree_node / rule_tree_node_line），运行时加载为 RuleTreeVO 对象。**

#### 4.1.4 抽奖主流程

```
AbstractRaffleStrategy.performRaffle()
    1. raffleLogicChain()  → 责任链过滤，得到 awardId
    2. raffleLogicTree()   → 决策树过滤，得到最终 awardId（可能换为兜底奖）
    3. 返回 RaffleAwardEntity
```

### 4.2 Activity 活动领域

**包路径：** `cn.bugstack.domain.activity`

活动领域负责管理抽奖活动的生命周期，核心职责分两块：

#### 4.2.1 额度订单（Quota）

用户参与抽奖前，先通过「充值/购买」获得抽奖次数。

```
AbstractRaffleActivityAccountQuota（模板方法抽象类）
    └─ createQuotaOrder()
        1. 查询 SKU 库存（Redis 原子扣减）
        2. 构建聚合根 CreateQuotaOrderAggregate
              ├─ ActivityOrderEntity（活动订单）
              ├─ totalCount / dayCount / monthCount（次数信息）
        3. 调用 IActivityRespository.saveCreateQuotaOrder()
              └─ 事务内：写订单 + 更新账户额度（含月/日子账户）
```

`CreateQuotaOrderAggregate` 聚合了订单实体和次数配置，作为一个整体存储，保证原子性。

#### 4.2.2 参与订单（Partake）

用户实际参与一次抽奖时，消耗额度并生成「用户抽奖订单」。

```
AbstractRaffleActivityPartake（模板方法抽象类）
    └─ createOrder(partakeEntity)
        1. 校验活动状态（open/时间范围）
        2. 查询是否有未消费的 user_raffle_order（幂等处理）
        3. doFilterAccount() → 子类实现，检查账户日/月/总次数
        4. 构建 CreatePartakeOrderAggregate
              ├─ ActivityAccountEntity   （总账户扣减）
              ├─ ActivityAccountDayEntity（日账户扣减）
              ├─ ActivityAccountMonthEntity（月账户扣减）
              └─ UserRaffleOrderEntity  （用户抽奖订单）
        5. 事务内一次性保存聚合
```

**设计亮点：** 通过聚合根将多个子账户的扣减封装为一个原子操作，防止账户超扣。

### 4.3 Award 奖品领域

**包路径：** `cn.bugstack.domain.award`

```
AwardService
    └─ saveUserAwardRecord(userAwardRecordEntity)
        1. 构建 MQ 消息对象 SendAwardMessage
        2. 构建 TaskEntity（本地消息表，状态=create）
        3. 封装 UserAwardRecordAggregate
        4. 事务内：写 user_award_record + 写 task 消息表
        5. 事务提交后异步发送 MQ

    └─ distributeAward(distributeAwardEntity)
        1. 查询 award_key（奖品对接标识）
        2. 从 distributeAwardMap 按 key 路由到 IDistributeAward 实现
        3. 调用 giveOutPrizes() 发奖（积分/实物等策略）
```

**设计亮点：** 奖品发放采用**策略模式（Strategy Pattern）**，`IDistributeAward` 的不同实现对应不同发奖方式，通过 `award_key` 路由，新增奖品类型只需新增一个实现类，无需修改核心逻辑（开闭原则）。

### 4.4 Rebate 行为返利领域

**包路径：** `cn.bugstack.domain.rebate`

用户完成特定行为（签到、下单等）后，系统自动发放积分奖励。

```
BehaviorRebateService.createOrder(behaviorEntity)
    1. 查询该行为类型对应的返利配置（daily_behavior_rebate 表）
    2. 对每条返利配置构建 BehaviorRebateAggregate
          ├─ BehaviorRebateOrderEntity（返利订单，bizId 防重）
          └─ TaskEntity（本地消息表）
    3. 批量保存聚合（事务）
    4. 发送返利 MQ 消息触发积分入账
```

**bizId 构成：** `userId_rebateType_outBusinessNo`，确保同一用户同一行为同一业务流水只处理一次（幂等）。

### 4.5 Credit 积分账户领域

**包路径：** `cn.bugstack.domain.credit`

```
CreditAdjustService.createOrder(tradeEntity)
    1. TradeAggregate.createCreditAccountEntity()  → 构建账户变更实体
    2. TradeAggregate.createCreditOrderEntity()    → 构建积分流水订单
    3. TradeAggregate.createTaskEntity()           → 构建可靠消息任务
    4. 封装 TradeAggregate 聚合根
    5. creditRepository.saveUserCreditTradeOrder() → 事务落库
```

**设计亮点：** 积分的每次变动都有对应的 `user_credit_order` 流水记录，支持对账与追溯。

### 4.6 Task 任务可靠消息领域

**包路径：** `cn.bugstack.domain.task`

采用**本地消息表（Outbox Pattern）**保证消息可靠投递：
1. 业务操作与写 task 消息表在同一事务
2. 定时任务（XXL-Job）扫描状态为 `create` 的消息，重新发送
3. MQ 消费成功后更新 task 状态为 `complete`
4. 消费失败保持 `create` 状态，等待下次补偿

---

## 5. 核心设计模式汇总

| 设计模式 | 应用位置 | 说明 |
|---------|---------|------|
| 模板方法（Template Method） | `AbstractRaffleStrategy`、`AbstractRaffleActivityPartake`、`AbstractRaffleActivityAccountQuota` | 定义算法骨架，子类实现差异步骤 |
| 责任链（Chain of Responsibility） | `AbstractLogicChain` + 各 `LogicChain` 实现 | 抽奖前置规则过滤，可动态组装链 |
| 决策树（Decision Tree / Composite） | `DecisionTreeEngine` + `ILogicTreeNode` | 抽奖后置规则过滤，数据库驱动的树结构 |
| 策略模式（Strategy） | `IDistributeAward` 各实现，`ILogicFilter` 各实现 | 奖品发放策略、过滤逻辑按 key 路由 |
| 工厂模式（Factory） | `DefaultLogicChainFactory`、`DefaultTreeFactory`、`DefaultLogicFactory` | 根据配置动态创建责任链/决策树实例 |
| 聚合根（DDD Aggregate） | `CreateQuotaOrderAggregate`、`TradeAggregate` 等 | 将多个实体封装为一个原子操作单元 |
| 本地消息表（Outbox Pattern） | `TaskEntity` + XXL-Job 补偿 | 保证业务操作与消息投递的最终一致性 |
| 仓储模式（Repository） | 所有 `IXxxRespository` 接口 | 领域层不感知存储细节，由基础设施层实现 |

---

## 6. 基础设施层设计

### 6.1 仓储实现（Repository Impl）

每个领域仓储接口都有对应的基础设施实现，遵循以下规范：

```
StrategyRespositoryImpl implements IStrategyRespository
    ├─ 先查 Redis 缓存（减少 DB 压力）
    ├─ 缓存 miss 则查 MySQL（通过对应 DAO）
    ├─ 结果写回 Redis
    └─ PO（持久化对象）与 Entity（领域对象）通过 BeanUtils 转换
```

**Redis Key 规范（Constants.RedisKey）：**

| Key 模板 | 用途 |
|---------|------|
| `strategy_award_list_key:{strategyId}` | 策略奖品列表缓存 |
| `strategy_rate_range_key:{key}` | 概率范围值（散列表长度） |
| `strategy_rate_table_key:{key}` | 概率散列表（随机数→奖品ID映射） |
| `activity_key:{activityId}` | 活动信息缓存 |
| `activity_sku_stock_count_key:{sku}` | SKU 库存（原子扣减） |

### 6.2 分库分表路由

基础设施层引入 `db-router` 中间件（`IDBRouterStrategy`），对用户相关数据按 `userId` 哈希路由到对应的分库（big_market_01 / big_market_02）和分表（_000 ~ _003）。

### 6.3 双数据源配置

`DataSourceConfig` 配置两套 SqlSessionFactory：
- **mysqlSqlSessionFactory**：扫描 `cn.bugstack.infrastructure.dao`，挂载分库路由插件
- **elasticsearchSqlSessionFactory**：扫描 `cn.bugstack.infrastructure.elasticsearch`，连接 ES 数据源

### 6.4 动态配置（DCC）

`DCCValueBeanFactory` 通过 Nacos 配置中心实现动态配置注入，支持在不重启服务的情况下修改限流、开关等配置项（如 `@DCCValue` 注解标注的字段）。

### 6.5 限流 AOP

`RateLimiterAOP` 在 `big-market-app` 层通过 AOP 对触发器层接口做全局限流，防止流量突增打穿领域层。

---

## 7. 数据库设计思想详解

### 7.1 数据库分层：配置库与用户数据库分离

系统使用三个数据库，职责严格分离：

| 数据库 | 类型 | 存储内容 |
|-------|------|----------|
| `big_market` | 配置库（低写高读） | 策略、奖品、活动、规则树等配置数据 |
| `big_market_01` | 用户分库一（高写） | 用户抽奖订单、账户额度等用户行为数据 |
| `big_market_02` | 用户分库二（高写） | 同上，按 userId 哈希分流 |

**设计思想：** 配置数据变更频率低、可大量缓存；用户行为数据写入频繁，通过分库减少单库压力。

### 7.2 big_market 主配置库表详解

#### award（奖品表）
```sql
award_id      -- 奖品ID（内部流转）
award_key     -- 奖品对接标识（如 user_credit_random），对应 IDistributeAward 实现
award_config  -- 奖品配置（如积分范围 "1,100"）
award_desc    -- 奖品描述
```
`award_key` 是奖品发放的路由键，对应策略模式中的 Map Key，实现了配置驱动的发奖扩展。

#### strategy（抽奖策略表）
```sql
strategy_id   -- 策略ID
strategy_desc -- 策略描述
rule_models   -- 规则模型列表（如 "rule_weight,rule_blacklist"），驱动责任链组装
```
`rule_models` 字段直接驱动 `DefaultLogicChainFactory` 的责任链组装，是「配置即代码」的体现。

#### strategy_award（策略奖品概率表）
```sql
strategy_id      -- 关联策略
award_id         -- 关联奖品
award_count      -- 库存总量
award_count_surplus -- 库存剩余
award_rate       -- 中奖概率（decimal(6,4)）
rule_models      -- 该奖品的后置规则（驱动决策树节点）
sort             -- 排序（兜底奖品 sort 最大）
```

#### strategy_rule（策略规则表）
```sql
strategy_id  -- 关联策略
award_id     -- 可为空（策略级规则为空，奖品级规则有值）
rule_type    -- 规则类型（1=策略，2=奖品）
rule_model   -- 规则标识（rule_weight / rule_blacklist / rule_lock 等）
rule_value   -- 规则值（如黑名单用户ID列表，权重阈值配置）
```

#### rule_tree / rule_tree_node / rule_tree_node_line（规则树三表）

决策树以三张表存储：

```
rule_tree            -- 树的元信息（tree_id, tree_node_rule_key=根节点）
rule_tree_node       -- 树的每个节点（rule_key, rule_value, 所属 tree_id）
rule_tree_node_line  -- 节点间连线（from→to, 触发条件 rule_limit_type/value）
```

运行时加载为 `RuleTreeVO`（含 treeNodeMap），`DecisionTreeEngine` 遍历执行。
这种「数据库驱动的行为树」设计使得规则的增删改无需发布代码。

#### raffle_activity（抽奖活动表）
```sql
activity_id    -- 活动ID
activity_name  -- 活动名称
strategy_id    -- 关联的抽奖策略ID（活动→策略→奖品）
state          -- 活动状态（open/close/create/finish）
begin_datetime -- 活动开始时间
end_datetime   -- 活动结束时间
```

#### raffle_activity_count（活动次数配置表）
```sql
count_id      -- 次数配置ID
total_count   -- 总次数上限
day_count     -- 每日次数上限
month_count   -- 每月次数上限
```
按「次数套餐」抽象，一个配置可复用于多个活动，体现了配置复用思想。

#### raffle_activity_sku（活动 SKU 商品表）
```sql
sku            -- 商品SKU
activity_id    -- 关联活动
activity_count_id -- 关联次数配置
stock_count    -- 库存总量
stock_count_surplus -- 库存剩余
price          -- 积分价格
```
SKU 是用户「购买」抽奖次数的商品单元，引入电商概念，使活动次数的销售可量化管理。

#### daily_behavior_rebate（日行为返利配置表）
```sql
behavior_type  -- 行为类型（如 sign=签到）
rebate_desc    -- 返利描述
rebate_type    -- 返利类型（sku/integral）
rebate_config  -- 返利配置值
```

### 7.3 big_market_01/02 用户分库表详解

#### raffle_activity_account（用户活动总账户）
```sql
user_id               -- 用户ID（分库路由键）
activity_id           -- 活动ID
total_count           -- 购买的总次数
total_count_surplus   -- 总次数剩余
day_count             -- 日次数上限
day_count_surplus     -- 日次数剩余
month_count           -- 月次数上限
month_count_surplus   -- 月次数剩余
```
唯一索引：`uq_user_id_activity_id`，一个用户一个活动只有一条总账户记录。

#### raffle_activity_account_day（用户活动日账户）
```sql
user_id / activity_id / day  -- 三字段联合唯一索引
day_count / day_count_surplus
```
按日存储，自动隔日重置（新的一天在 day 维度创建新记录）。

#### raffle_activity_account_month（用户活动月账户）
```sql
user_id / activity_id / month  -- 三字段联合唯一索引
month_count / month_count_surplus
```

#### raffle_activity_order（抽奖活动订单，分表 _000~_003）
```sql
order_id        -- 唯一订单ID（唯一索引）
out_business_no -- 外部业务流水号（唯一索引，幂等）
user_id / sku / activity_id / strategy_id
state           -- 订单状态（completed）
```
分4张表（raffle_activity_order_000 ~ 003），按 userId 哈希路由。

#### user_raffle_order（用户抽奖订单）
```sql
user_id / activity_id / strategy_id
order_id        -- 唯一订单ID
order_state     -- 订单状态（create/used/cancel）
```
`create` 状态表示已分配但未消耗（幂等入口，防止重复参与）。

#### user_award_record（用户中奖记录）
```sql
user_id / activity_id / strategy_id / award_id
order_id        -- 抽奖订单ID（唯一索引）
award_state     -- 发奖状态（create/completed）
```

#### user_credit_account（用户积分账户）
```sql
user_id         -- 唯一索引
available_amount -- 可用积分
```

#### user_credit_order（积分流水订单）
```sql
user_id / order_id / out_business_no
trade_name      -- 交易名称
trade_type      -- 交易类型（Forward=入账 / Reverse=出账）
amount          -- 变动金额
```
每笔积分变动均有流水，支持对账。

#### task（可靠消息任务表）
```sql
user_id / message_id
topic           -- MQ Topic
message         -- 消息 JSON
state           -- 状态（create/completed/fail）
```
与业务操作同一事务写入，由 XXL-Job 定时扫描补偿投递。

### 7.4 分库分表策略

**分库：** 用户相关数据按 `userId` 哈希，路由到 `big_market_01` 或 `big_market_02`。

**分表：** 订单类数据在每个分库内再按 `userId` 哈希分为 4 张子表（_000 ~ _003）。

```
userId → hash → 库索引（01 or 02）→ 表索引（000~003）
```

`IDBRouterStrategy`（db-router 中间件）在 MyBatis 插件层拦截 SQL，动态替换表名，应用层代码无感知。

**索引设计规律：**
- 每张用户表均有 `idx_user_id_activity_id` 复合索引，覆盖最常见的「按用户+活动」查询
- 订单表增加 `uq_order_id`、`uq_out_business_no` 唯一索引，保障幂等

### 7.5 幂等与防重设计

项目在多个层次做了幂等保障：

| 场景 | 防重手段 |
|------|----------|
| 购买活动次数 | `out_business_no` 唯一索引，重复插入抛 `DuplicateKeyException` 捕获处理 |
| 参与抽奖 | 先查 `user_raffle_order` 是否有 `create` 状态订单，有则直接返回 |
| 行为返利 | bizId = `userId_rebateType_outBusinessNo`，写入时唯一索引拦截 |
| 积分流水 | `user_credit_order.out_business_no` 唯一索引 |
| MQ 消费 | 消费前先检查 `user_award_record.award_state`，已完成则跳过 |

### 7.6 账户额度多粒度设计

用户的抽奖次数被设计为三个粒度的账户，互相独立扣减：

```
raffle_activity_account      → 总账户（lifetime）
raffle_activity_account_day  → 日账户（每日重置）
raffle_activity_account_month→ 月账户（每月重置）
```

每次参与抽奖需同时扣减三个账户，任何一个不足都会拒绝（由 `doFilterAccount` 抽象方法实现）。
日/月账户不足时，通过 `INSERT ... ON DUPLICATE KEY UPDATE` 模式按需创建或更新。

---

## 8. 领域与数据库的映射关系

```
领域层对象                    数据库表（库）
─────────────────────────────────────────────────────────────
StrategyEntity           ←→  strategy                (big_market)
StrategyAwardEntity      ←→  strategy_award          (big_market)
StrategyRuleEntity       ←→  strategy_rule           (big_market)
RuleTreeVO               ←→  rule_tree + rule_tree_node
                              + rule_tree_node_line   (big_market)
ActivityEntity           ←→  raffle_activity         (big_market)
ActivityCountEntity      ←→  raffle_activity_count   (big_market)
ActivitySkuEntity        ←→  raffle_activity_sku     (big_market)
ActivityOrderEntity      ←→  raffle_activity_order_xxx(big_market_0x)
ActivityAccountEntity    ←→  raffle_activity_account (big_market_0x)
ActivityAccountDayEntity ←→  raffle_activity_account_day(big_market_0x)
UserRaffleOrderEntity    ←→  user_raffle_order       (big_market_0x)
UserAwardRecordEntity    ←→  user_award_record       (big_market_0x)
BehaviorRebateOrderEntity←→  user_behavior_rebate_order(big_market_0x)
CreditAccountEntity      ←→  user_credit_account     (big_market_0x)
CreditOrderEntity        ←→  user_credit_order       (big_market_0x)
TaskEntity               ←→  task                    (big_market_0x)
```

**规律：** 配置类数据（奖品、策略、活动配置）存 `big_market`；用户行为数据（订单、账户、记录）存分库 `big_market_0x`。

---

## 9. 关键流程梳理

### 9.1 用户参与抽奖完整流程

```
[HTTP 请求] IRaffleActivityService.draw()
    │
    ▼
[Trigger] RaffleActivityController
    │  参数校验、转换
    ▼
[Domain - Activity] AbstractRaffleActivityPartake.createOrder()
    │  1. 校验活动状态与时间
    │  2. 查幂等（user_raffle_order 是否有 create 订单）
    │  3. doFilterAccount() 检查总/日/月额度
    │  4. 构建 CreatePartakeOrderAggregate
    │  5. 事务落库（扣账户 + 写抽奖订单）
    ▼
[Domain - Strategy] AbstractRaffleStrategy.performRaffle()
    │  1. 责任链前置过滤
    │     黑名单 → 权重 → 默认（随机查 Redis 概率表）
    │  2. 决策树后置过滤
    │     次数锁 → 库存扣减 → 兜底奖励
    │  3. 返回最终 awardId
    ▼
[Domain - Award] AwardService.saveUserAwardRecord()
    │  1. 构建 UserAwardRecordAggregate
    │  2. 事务内：写 user_award_record + 写 task 消息
    │  3. 发送 MQ 消息（SendAwardMessageEvent）
    ▼
[MQ Consumer] 消费 SendAwardMessage
    └─ AwardService.distributeAward()
       按 award_key 路由到 IDistributeAward 实现
       执行实际发奖（如积分入账）
```

### 9.2 活动 SKU 库存扣减流程

```
用户购买 SKU
    │
    ▼
Redis 原子扣减（DECR strategy:sku:stock:count:key:{sku}）
    │  扣减成功 → 继续
    │  扣减失败（库存为0）→ 发送 ActivitySkuStockZeroMessageEvent
    ▼                         └─ MQ 消费 → 更新 DB 库存为0
写 raffle_activity_order（订单落库）
    ▼
异步从 Redis 延迟队列消费，同步更新 DB 库存剩余
```

**设计思想：** Redis 做库存的「一级扣减」，DB 做最终一致性同步，高并发场景下避免 DB 成为瓶颈。

### 9.3 行为返利 → 积分入账流程

```
用户完成行为（如签到）
    │
    ▼
BehaviorRebateService.createOrder()
    │  写返利订单 + task 消息（同一事务）
    │  发送 SendRebateMessageEvent
    ▼
[MQ Consumer] 消费返利消息
    └─ CreditAdjustService.createOrder()
       写积分账户变动 + 积分流水订单 + task 消息（同一事务）
       发送 CreditAdjustSuccessMessageEvent
    ▼
[MQ Consumer] 消费积分调整成功消息
    └─ 更新 user_credit_account 可用积分
```

---

## 总结

### 领域设计核心思想

1. **DDD 分层 + 依赖倒置**：`domain` 层纯粹，不依赖任何框架和基础设施，通过 Repository 接口解耦存储。
2. **聚合根保证原子性**：将多表操作封装为聚合对象，在仓储层的事务中一次性提交，防止数据不一致。
3. **双重过滤架构**：责任链（前置，决定 awardId）+ 决策树（后置，校验并可能替换 awardId），两种结构各司其职，高度可扩展。
4. **配置驱动行为**：规则链由 `strategy.rule_models` 字段驱动，决策树由数据库三张表驱动，业务规则变更无需改代码。
5. **策略模式解耦发奖**：`IDistributeAward` 按 `award_key` 路由，新增奖品类型只需新增实现，符合开闭原则。
6. **本地消息表保障最终一致性**：业务写入与消息写入同一事务，XXL-Job 补偿投递，避免消息丢失。

### 数据库设计核心思想

1. **配置库与用户库分离**：配置数据可大量缓存，用户数据高频写入走分库，职责清晰。
2. **分库分表**：用户数据按 userId 哈希分 2 库 × 4 表，线性扩展写入能力。
3. **多粒度账户**：总/日/月三级账户独立管理，精细控制抽奖频次，防止薅羊毛。
4. **多层幂等防重**：唯一索引 + 状态机 + 业务 bizId 三重保障，覆盖各种重试场景。
5. **Redis 一级库存**：高并发库存扣减走 Redis，DB 做异步同步，兼顾性能与数据安全。
6. **流水可追溯**：积分变动、抽奖、返利均有完整流水表，支持对账和问题追查。

