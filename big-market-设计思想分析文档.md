# Big-Market 大营销平台 — 领域设计与数据库设计思想分析

> 文档生成日期：2026-03-23
> 项目作者：小傅哥 (fuzhengwei)
> 技术栈：Java 8 · Spring Boot · DDD · MyBatis · Redis(Redisson) · RocketMQ · XXL-Job · ShardingSphere

---

## 目录

1. [项目概述](#1-项目概述)
2. [工程结构与模块职责](#2-工程结构与模块职责)
3. [领域驱动设计（DDD）思想](#3-领域驱动设计ddd思想)
   - 3.1 分层架构
   - 3.2 领域模块划分
   - 3.3 聚合根与聚合设计
   - 3.4 仓储模式（Repository Pattern）
   - 3.5 领域事件与可靠消息投递
   - 3.6 责任链模式（抽奖规则链）
   - 3.7 决策树模式（奖品后置过滤）
   - 3.8 模板方法模式
   - 3.9 策略模式
4. [数据库设计思想](#4-数据库设计思想)
   - 4.1 分库设计思路
   - 4.2 主库（big_market）表设计
   - 4.3 分库（big_market_01/02）表设计
   - 4.4 分表策略
   - 4.5 Redis 缓存设计
5. [核心业务流程分析](#5-核心业务流程分析)
   - 5.1 抽奖全流程
   - 5.2 活动参与与次数扣减流程
   - 5.3 奖品发放流程
   - 5.4 积分流转流程
   - 5.5 行为返利流程
6. [设计思想总结](#6-设计思想总结)

---

## 1. 项目概述

Big-Market 是一个**企业级大营销平台**，核心功能为可配置的抽奖活动系统，支持：

- 多策略抽奖（权重抽奖、黑名单拦截、次数解锁奖品）
- 活动 SKU 商品下单、积分兑换抽奖次数
- 用户行为返利（签到、下单等行为触发积分奖励）
- 奖品异步可靠发放（MQ + Task 表兜底）
- 积分账户管理（充值、消费、流水）
- 动态配置中心（DCC）+ 熔断降级（Hystrix）

该项目以 **DDD（领域驱动设计）** 为核心架构思想，是学习企业级 DDD 落地实践的典型参考项目。

---

## 2. 工程结构与模块职责

```
big-market/
├── big-market-app            # 启动层：Spring Boot 入口，装配所有模块
├── big-market-api            # API 接口层：对外暴露的接口定义 + DTO
├── big-market-trigger        # 触发器层：HTTP Controller、MQ Consumer、定时任务
├── big-market-domain         # 领域层：核心业务逻辑（DDD 核心）
├── big-market-infrastructure # 基础设施层：DB/Cache/MQ 的具体实现
└── big-market-types          # 公共类型层：枚举、异常、工具、注解
```

### 模块依赖关系

```
app
 └── trigger  ──depends──>  api
      └── domain  <──implements──  infrastructure
           └── types
```

| 模块 | 职责 | 典型类 |
|---|---|---|
| `big-market-app` | Spring Boot 启动、Bean 组装 | `Application.java` |
| `big-market-api` | 对外接口契约（DTO/接口）| `IRaffleActivityService`, `IRaffleStrategyService` |
| `big-market-trigger` | HTTP 入口、MQ 消费、定时扫描 | `IRaffleActivityController`, `SendAwardCustomer`, `SendMessageTaskJob` |
| `big-market-domain` | 领域服务、聚合、实体、值对象、仓储接口 | `AbstractRaffleStrategy`, `AwardService`, `CreditAdjustService` |
| `big-market-infrastructure` | 仓储实现、DAO、PO、Redis、事件发布 | `ActivityRespositoryImpl`, `StrategyRespositoryImpl` |
| `big-market-types` | 响应码、通用异常、常量、注解 | `ResponseCode`, `AppException`, `Constants` |

### 设计意图

> **trigger 层只调用 domain 层的接口**，不直接调用 infrastructure，实现了**依赖倒置**：domain 定义仓储接口，infrastructure 提供实现，Spring IoC 注入。这是 DDD 六边形架构（Ports & Adapters）的典型体现。

---

## 3. 领域驱动设计（DDD）思想

### 3.1 分层架构

本项目采用严格的 **四层 DDD 架构**：

```
┌─────────────────────────────────────────┐
│  Trigger Layer（用户接口层）              │
│  HTTP / MQ Consumer / XxlJob            │
├─────────────────────────────────────────┤
│  Domain Layer（领域层）                  │
│  聚合 · 实体 · 值对象 · 领域服务 · 仓储接口 │
├─────────────────────────────────────────┤
│  Infrastructure Layer（基础设施层）       │
│  仓储实现 · DAO · Redis · EventPublisher │
├─────────────────────────────────────────┤
│  Types Layer（公共层）                   │
│  枚举 · 异常 · 工具类 · 注解             │
└─────────────────────────────────────────┘
```

**核心原则**：Domain 层不依赖任何具体技术框架（不 import Spring Data、不 import MyBatis），只定义仓储**接口**（`IActivityRespository`、`IStrategyRespository` 等），由 Infrastructure 层实现。


### 3.2 领域模块划分

`big-market-domain` 下划分了 **6 个子领域**，每个子领域独立、内聚：

```
domain/
├── strategy/   # 抽奖策略领域：策略装配、规则链、决策树、随机抽奖
├── activity/   # 活动领域：活动参与、次数账户、SKU商品、订单下单
├── award/      # 奖品领域：奖品发放记录、分发策略
├── rebate/     # 返利领域：行为返利规则、返利订单
├── credit/     # 积分领域：积分账户、积分流水
└── task/       # 任务领域：MQ消息补偿任务
```

| 领域 | 核心问题 | 关键聚合/服务 |
|---|---|---|
| strategy | 如何抽奖？按什么规则？ | `AbstractRaffleStrategy`, 责任链, 决策树 |
| activity | 用户如何参与活动、扣次数？ | `AbstractRaffleActivityPartake`, `AbstractRaffleActivityQuotaAccount` |
| award | 抽到奖品后如何异步发放？ | `AwardService`, `IDistributeAward` |
| rebate | 用户行为如何触发积分返利？ | `BehaviorRebateService` |
| credit | 积分如何增减、流水如何记录？ | `CreditAdjustService`, `TradeAggregate` |
| task | MQ 消息失败如何补偿？ | `SendMessageTaskJob` + task 表 |

### 3.3 聚合根与聚合设计

DDD 的聚合（Aggregate）是本项目最核心的设计模式。每一个需要**跨表原子写入**的操作，都被封装成一个聚合对象，通过仓储的单一方法完成事务保存。

#### 典型聚合示例

**① CreatePartakeOrderAggregate（创建参与订单聚合）**
```
CreatePartakeOrderAggregate
├── UserRaffleOrderEntity    # 用户抽奖订单（写 user_raffle_order 表）
├── ActivityAccountEntity    # 总账户（更新 raffle_activity_account）
├── ActivityAccountDayEntity # 日账户（更新 raffle_activity_account_day）
└── ActivityAccountMonthEntity # 月账户（更新 raffle_activity_account_month）
```
仓储方法：`activityRespository.saveCreatePartakeOrderAggerate(aggregate)` → 一次事务完成 4 张表的更新。

**② UserAwardRecordAggregate（用户获奖记录聚合）**
```
UserAwardRecordAggregate
├── UserAwardRecordEntity    # 获奖记录（写 user_award_record 表）
└── TaskEntity               # MQ任务（写 task 表，同事务）
```

**③ TradeAggregate（积分交易聚合）**
```
TradeAggregate
├── CreditAccountEntity      # 积分账户（upsert user_credit_account）
├── CreditOrderEntity        # 积分流水单（写 user_credit_order）
└── TaskEntity               # MQ任务（写 task 表）
```

**④ BehaviorRebateAggregate（行为返利聚合）**
```
BehaviorRebateAggregate
├── BehaviorRebateOrderEntity # 返利订单
└── TaskEntity                # MQ任务
```

#### 聚合设计原则

- **一个聚合对应一个业务事务边界**：所有跨表操作通过聚合统一提交，保证原子性。
- **聚合内部用工厂方法构建**：如 `TradeAggregate.createCreditAccountEntity()` 封装创建逻辑。
- **聚合只暴露行为，不暴露内部状态**：外部通过仓储方法整体存储，不逐个操作实体。

### 3.4 仓储模式（Repository Pattern）

每个领域子模块都定义自己的仓储接口，放在 `domain/{module}/respository/` 下：

```java
// 领域层：只定义接口
public interface IActivityRespository {
    ActivityEntity queryRaffleActivityByActivityId(Long activityId);
    void saveCreatePartakeOrderAggerate(CreatePartakeOrderAggregate aggregate);
    boolean subtractionSkuStockCount(Long sku, String key, Date endDateTime);
    // ...
}

// 基础设施层：提供实现（ActivityRespositoryImpl）
@Repository
public class ActivityRespositoryImpl implements IActivityRespository {
    @Resource IRedisService redisService;
    @Resource IRaffleActivityDao raffleActivityDao;
    // 先查缓存，缓存miss再查DB，写入缓存
}
```

**缓存优先策略**（Cache-Aside Pattern）贯穿所有仓储实现：
1. 先从 Redis 查询
2. Miss 则从 MySQL 查询
3. 将结果写入 Redis（设置适当过期时间）
4. 返回结果


### 3.5 领域事件与可靠消息投递

本项目采用 **本地消息表（Outbox Pattern）** 保证 MQ 消息的可靠投递，彻底解决「消息丢失」问题。

#### 设计流程

```
业务写操作
   │
   ├─ 写业务表（如 user_award_record）  ┐
   └─ 写 task 表（state=create）        ┘ 同一个本地事务
   │
   ↓
异步 SendMessageTaskJob（定时扫描）
   ├─ 查询 task 表中 state=create/fail 的记录
   ├─ 发送 RocketMQ 消息
   └─ 更新 task.state = complete

MQ Consumer 消费成功后
   └─ 执行实际发奖/积分调整逻辑
```

#### task 表的状态机

| state | 含义 |
|---|---|
| `create` | 消息已创建，等待投递 |
| `complete` | 消息已成功发送 |
| `fail` | 发送失败，等待重试 |

**三类领域事件**：
- `SendAwardMessageEvent`：触发奖品发放
- `CreditAdjustSuccessMessageEvent`：积分调整成功通知
- `SendRebateMessageEvent`：返利消息通知

### 3.6 责任链模式（抽奖规则链）

抽奖前置规则使用**责任链（Chain of Responsibility）**模式，支持动态装配、顺序执行、短路退出。

#### 责任链节点

| 节点 Bean | 规则模型 | 作用 |
|---|---|---|
| `BlackListLogicChain` | `rule_blacklist` | 黑名单用户直接返回兜底奖品 |
| `RuleWeightLogicChain` | `rule_weight` | 积分达到阈值进入权重奖品池 |
| `DefaultLogicChain` | `default` | 默认随机抽奖（终止节点）|

#### 装配逻辑（DefaultLogicChainFactory）

```java
// 从 strategy 表的 rule_models 字段读取规则列表，按顺序装配责任链
String[] ruleModels = strategyEntity.ruleModels(); // e.g. ["rule_blacklist", "rule_weight"]
ILogicChain logicChain = logicChainGroup.get(ruleModels[0]);
for (int i = 1; i < ruleModels.length; i++) {
    currentchain = currentchain.appendnext(logicChainGroup.get(ruleModels[i]));
}
currentchain.appendnext(logicChainGroup.get("default")); // 末尾追加默认节点
```

#### 执行流程

```
BlackListLogicChain.logic()
   ├─ 命中黑名单 → 返回兜底奖品 awardId（短路，不再向下传递）
   └─ 未命中 → next().logic()（放行，传递给下一节点）
         │
   RuleWeightLogicChain.logic()
         ├─ 用户积分 >= 某阈值 → 从对应权重奖品池中随机（短路）
         └─ 不满足 → next().logic()
               │
         DefaultLogicChain.logic()
               └─ 全量随机抽奖，返回 awardId
```

### 3.7 决策树模式（奖品后置过滤）

抽奖后对奖品进行后置规则过滤，使用**决策树（Decision Tree）**模式，规则配置存在数据库中（`rule_tree`、`rule_tree_node`、`rule_tree_node_line` 三张表）。

#### 决策树节点类型

| 节点 Bean | rule_key | 作用 |
|---|---|---|
| `RuleLockLogicTreeNode` | `rule_lock` | 抽奖次数未达解锁阈值，降级处理 |
| `RuleStockLogicTreeNode` | `rule_stock` | 奖品库存不足，降级到兜底奖品 |
| `RuleLuckAwardLogicTreeNode` | `rule_luck_award` | 兜底奖品节点（终止） |

#### 执行引擎（DecisionTreeEngine）

```java
// 从树的根节点开始，按节点计算结果走对应的边，直到叶子节点
String nextNode = ruleTreeVO.getTreeRootRuleNode();
while (null != nextNode) {
    ILogicTreeNode logicTreeNode = logicTreeNodeGroup.get(ruleTreeNode.getRuleKey());
    TreeActionEntity result = logicTreeNode.logic(userId, strategyId, awardId, ruleValue, endDateTime);
    nextNode = nextNode(result.getRuleLogicCheckType().getCode(), ruleTreeNode.getTreeNodeLineVOList());
}
```

**边的跳转条件**存在 `rule_tree_node_line` 表中，支持 EQUAL/GT/LT/GE/LE 比较器。

### 3.8 模板方法模式

多个抽象基类定义了**标准流程骨架**，子类只需实现差异化步骤：

| 抽象类 | 固定流程（模板） | 子类实现 |
|---|---|---|
| `AbstractRaffleStrategy` | 校验参数 → 走责任链 → 走决策树 → 返回奖品 | `raffleLogicChain()`, `raffleLogicTree()` |
| `AbstractRaffleActivityPartake` | 查活动信息 → 查未消费订单 → 创建聚合 → 存库 | `doFilterAccount()`, `builderUserRaffleOrder()` |
| `AbstractRaffleActivityQuotaAccount` | 查SKU → 查活动 → 走活动规则链 → 构建订单聚合 → 交易策略 | `builderOrderAggerate()` |

### 3.9 策略模式

**交易策略（ITradePolicy）**：积分充值支持不同的交易类型（直接充值 vs 先创建订单再扣减），通过 `Map<String, ITradePolicy>` 注入，按 `OrderTradeTypeVO` 的 code 路由到对应策略。

**奖品分发策略（IDistributeAward）**：不同类型奖品有不同的发放实现（积分奖品、实物奖品等），通过 `Map<String, IDistributeAward>` 注入，按 `award_key` 路由。

---

## 4. 数据库设计思想

### 4.1 分库设计思路

项目采用 **1主库 + N分库** 的分库架构（使用 ShardingSphere/db-router 中间件）：

| 数据库 | 用途 | 分片策略 |
|---|---|---|
| `big_market` | 配置数据（策略、活动、奖品、规则树） | 单库，无分片 |
| `big_market_01` | 用户流水数据（分库1） | 按 user_id 哈希路由 |
| `big_market_02` | 用户流水数据（分库2） | 按 user_id 哈希路由 |

**分库原则**：
- **配置类数据**（不随用户增长）放主库，量小、读多写少，加 Redis 缓存即可。
- **用户流水类数据**（随用户规模线性增长）放分库，按 user_id 哈希均匀分散到各库。

// __CONTINUE_HERE__


