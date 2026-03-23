# Big-Market 营销抽奖系统 — 领域设计与数据库设计文档

> 文档日期：2026-03-23
> 项目路径：`D:/javacode/WebProj/ClaudeHelp/big-market/`
> 作者：小傅哥 (fuzhengwei / bugstack.cn)

---

## 一、项目总览

big-market 是一个基于 **DDD（领域驱动设计）** 的营销抽奖平台，支持大规模用户抽奖、积分充值、行为返利、奖品发放等完整业务闭环。项目采用标准六层架构：

```
big-market-api           对外接口定义（DTO、接口）
big-market-app           启动层、Spring 配置
big-market-domain        核心领域层（业务逻辑）
big-market-trigger       触发器层（HTTP、MQ 入口）
big-market-infrastructure 基础设施层（DB、Redis、事件）
big-market-types         公共枚举、异常、工具
```

---

## 二、领域划分

系统划分为六个领域，每个领域独立内聚：

| 领域 | 包路径 | 职责 |
|------|--------|------|
| strategy | `domain.strategy` | 抽奖策略装配与执行 |
| activity | `domain.activity` | 活动管理、配额控制、参与入口 |
| award | `domain.award` | 奖品发放记录 |
| rebate | `domain.rebate` | 用户行为返利（签到、下单等） |
| credit | `domain.credit` | 用户积分账户管理 |
| task | `domain.task` | MQ 消息任务可靠投递保障 |

---

## 三、Strategy 领域 — 抽奖策略设计

### 3.1 核心思想：概率表 + Redis 随机分发

**文件**：`StrategyArmoryDispathchImpl.java`

抽奖不使用实时概率计算，而是在系统启动（或活动装配时）将所有奖品按概率展开为一张**概率映射表**存入 Redis，抽奖时只需从表中随机取一个索引，O(1) 完成。

装配流程：
```
1. 查询策略下所有奖品及其概率（strategy_award 表）
2. 将概率转换为最小公倍数份额，展开为 [awardId, awardId, ...] 数组
3. 打乱数组（SecureRandom），存入 Redis Hash（key=strategyId, field=随机下标, value=awardId）
4. 同时将每个奖品剩余库存写入 Redis（STRATEGY_AWARD_COUNT_KEY）
5. 若策略存在 rule_weight，按积分阈值额外装配对应权重子表
```

抽奖时（`IStrategyDisPatch.getRandomAwardId`）：
- 生成 [0, 概率表长度) 内随机数
- 直接查 Redis 得到 awardId
- 无需访问 DB，延迟极低

### 3.2 两层过滤架构：责任链 + 决策树

抽奖结果经过两个独立阶段的过滤，形成「前置规则链 → 抽奖 → 后置决策树」的三段式结构：

```
用户发起抽奖
     ↓
[阶段一] 责任链（AbstractRaffleStrategy.raffleLogicChain）
     │  作用：在抽奖之前决定是否走特殊路径（黑名单/权重），或放行走默认随机
     │  节点：BlackListLogicChain → RuleWeightLogicChain → DefaultLogicChain
     ↓
 随机抽奖（从Redis概率表取awardId）
     ↓
[阶段二] 决策树（AbstractRaffleStrategy.raffleLogicTree）
     │  作用：对抽到的奖品做后处理（库存扣减、次数限制、兜底奖品）
     │  节点：RuleLockLogicTreeNode → RuleStockLogicTreeNode → RuleLuckAwardLogicTreeNode
     ↓
最终奖品
```

### 3.3 责任链节点详解（DefaultLogicChainFactory）

工厂从数据库读取策略的 `ruleModels` 字段，按顺序动态组装责任链，末尾自动附加 `default` 节点。

| 节点 Bean 名 | 规则 | 逻辑 |
|-------------|------|------|
| `rule_blacklist` | BlackListLogicChain | 检查 userId 是否在黑名单中，命中则直接返回固定兜底奖品（如积分奖品），不进行随机抽奖 |
| `rule_weight` | RuleWeightLogicChain | 读取用户当前积分，匹配最接近的积分阈值（如4000/5000/6000），从对应权重子表中抽奖；积分不足则放行 |
| `default` | DefaultLogicChain | 走全量概率表随机抽奖 |

**黑名单规则数据格式**（存于 `strategy_rule.ruleValue`）：
```
100:user001,user002,user003
# 100=兜底奖品awardId，后面是黑名单用户列表
```

**权重规则数据格式**：
```
4000:102,103,104,105 5000:102,103,104,105,106,107 6000:102,103,104,105,106,107,108
# 积分阈值:可抽奖品列表（空格分隔多档）
```

### 3.4 决策树节点详解（DefaultTreeFactory）

决策树结构存储在数据库（`rule_tree`/`rule_tree_node`/`rule_tree_node_line` 三表），运行时动态加载，形成有向图。`DecisionTreeEngine` 从根节点出发递归执行。

| 节点 | 作用 |
|------|------|
| `RuleLockLogicTreeNode` | 校验用户抽奖次数是否达到解锁门槛，未达到则走兜底节点 |
| `RuleStockLogicTreeNode` | Redis 原子扣减奖品库存（`substractAwardCount`），库存不足走兜底 |
| `RuleLuckAwardLogicTreeNode` | 兜底节点，返回保底奖品（通常是积分） |

---

## 四、Activity 领域 — 活动与配额设计

### 4.1 三层账户配额体系

活动领域最核心的设计是用户抽奖配额的**三级镜像账户**结构，将总配额、月配额、日配额分开存储：

```
raffle_activity_account        （总账户：用户终身总次数）
raffle_activity_account_month  （月账户：当月剩余次数，每月重置）
raffle_activity_account_day    （日账户：当日剩余次数，每日重置）
```

**设计原因**：
- 分表后每张表只记录当前周期数据，避免全量扫描历史记录
- 月/日账户在对应周期首次使用时按总账户上限动态创建（镜像插入），不存在则说明当期未使用过
- 扣减时三张表同时原子扣减，任一失败则事务回滚

参与流程（`RaffleActivityPartakeService.doFilterAccount`）：
```
1. 查 raffle_activity_account：总次数为0 → 抛出 ACTIVITY_QUOTA_ERROR
2. 查 raffle_activity_account_month：当月次数为0 → 抛出 ACTIVITY_MONTH_QUOTA_ERROR
3. 查 raffle_activity_account_day：当日次数为0 → 抛出 ACTIVITY_DAY_QUOTA_ERROR
4. 三项均通过 → 创建 user_raffle_order（抽奖参与单）
5. 事务内原子扣减三级账户
```

### 4.2 SKU 商品与积分充值

用户通过消耗积分购买 `raffle_activity_sku`（SKU 商品）来获得抽奖次数：

```
用户积分 → 购买 SKU → 生成 raffle_activity_order → 增加 raffle_activity_account 次数
```

- SKU 绑定 `activityId` + `activityCountId`（次数配置），一个活动可以有多种 SKU（如10次包、50次包）
- SKU 库存通过 Redis 预热，下单时先扣 Redis 再异步落库（`updateActivitySkuStock`）
- 订单状态机：`create → completed`，支持幂等校验

### 4.3 活动责任链（DefaultActivityChainFactory）

活动参与前也有责任链校验：活动是否存在、是否在有效期、SKU 库存是否充足等，与 strategy 领域责任链架构一致。

---

## 五、Rebate 领域 — 行为返利设计

**核心思想**：用户完成特定行为（签到、下单等）时，系统自动发放积分/抽奖次数奖励。

流程（`BehaviorRebateService.createOrder`）：
```
1. 查 daily_behavior_rebate 配置表（按 behaviorType 查返利规则）
2. 构建 BehaviorRebateOrderEntity（幂等 bizId = userId_rebateType_outBusinessNo）
3. 同时构建 TaskEntity（MQ 消息任务）
4. 事务内同时写入 user_behavior_rebate_order + task 表
5. 事务提交后发 MQ，消费方增加用户积分/抽奖次数
```

**幂等保障**：`bizId` 由 `userId + 行为类型 + 外部业务号` 拼装，数据库唯一索引防重，相同行为不会重复发放。

---

## 六、Credit 领域 — 积分账户设计

**核心思想**：积分是系统内的虚拟货币，用于购买 SKU（换取抽奖次数）。

账户结构（`user_credit_account`）：
- `totalAmount`：历史累计总积分（只增不减，用于展示）
- `availableAmount`：当前可用积分（每次消费扣减）
- `accountStatus`：open/close（冻结机制）

积分变更流程（`CreditAdjustService.createOrder`）：
```
1. 构建 CreditAccountEntity（upsert 账户）
2. 构建 CreditOrderEntity（积分变更流水，携带 outBusinessNo 幂等）
3. 构建 TaskEntity（MQ 任务，积分变更成功后通知下游）
4. 三者打包为 TradeAggregate，事务内一次写入
```

积分变更来源：行为返利发放、积分消耗（购买 SKU）等，每次变更都有对应的 `user_credit_order` 流水记录。

---

## 七、Task 领域 — 可靠消息投递

**核心思想**：所有涉及跨服务/跨域的状态变更，都通过「本地消息表 + 定时扫描重投」模式保证 MQ 消息不丢失。

```
业务操作 + task 写入（同一事务）
        ↓
事务提交后立即尝试发 MQ
        ↓（若发送失败）
定时任务扫描 state=create/fail 的 task 记录 → 重新发送
        ↓
发送成功 → 更新 task.state = completed
```

Task 表字段：`userId`、`topic`（MQ Topic）、`messageId`（幂等）、`message`（JSON）、`state`（create/completed/fail）

这一模式在 rebate、credit、award 三个领域均有应用，确保最终一致性。

---

## 八、数据库表设计详解

### 8.1 策略相关表

#### strategy（抽奖策略表）
| 字段 | 说明 |
|------|------|
| strategyId | 策略ID，与活动关联 |
| strategyDesc | 策略描述 |
| ruleModels | 前置责任链规则列表，逗号分隔，如 `rule_blacklist,rule_weight`，决定责任链节点顺序 |

#### strategy_award（策略奖品表）
| 字段 | 说明 |
|------|------|
| strategyId | 所属策略 |
| awardId | 奖品ID |
| awardCount | 奖品总库存 |
| awardCountSurplus | 奖品剩余库存（DB 兜底，Redis 为主） |
| awardRate | 奖品概率（装配时展开为概率表） |
| ruleModels | 该奖品绑定的后置决策树规则，如 `rule_lock,rule_lucky_award` |
| sort | 排序权重 |

#### strategy_rule（策略规则表）
| 字段 | 说明 |
|------|------|
| strategyId | 所属策略 |
| awardId | 为 null 时表示策略级规则，非 null 时表示奖品级规则 |
| ruleType | 规则类型（策略/奖品） |
| ruleModel | 规则标识，如 `rule_blacklist`、`rule_weight`、`rule_lock` |
| ruleValue | 规则值，格式由各规则自定义（见第三章） |

**设计亮点**：策略规则与奖品解耦，通过 `ruleModel` 字符串标识动态路由到对应责任链/决策树节点，新增规则无需修改表结构，只需新增 Bean 和数据行。

#### rule_tree / rule_tree_node / rule_tree_node_line（决策树三表）

三表共同描述一棵有向决策树：

| 表 | 核心字段 | 作用 |
|----|----------|------|
| rule_tree | treeId, treeRootRuleKey | 树的定义，指定根节点 |
| rule_tree_node | treeId, ruleKey, ruleValue | 每个节点的规则标识和规则值 |
| rule_tree_node_line | treeId, ruleNodeFrom, ruleNodeTo, ruleLimitType, ruleLimitValue | 节点间的有向边，定义跳转条件 |

`ruleLimitType` 支持：`=`、`>`、`<`、`>=`、`<=`、`enum`（枚举范围），描述从 From 节点跳转到 To 节点的条件。

**设计亮点**：决策树完全由数据驱动，在数据库中配置即可改变抽奖后处理逻辑，无需重新部署代码。

### 8.2 活动相关表

#### raffle_activity（活动表）
| 字段 | 说明 |
|------|------|
| activityId | 活动唯一ID |
| strategyId | 绑定的抽奖策略ID（活动→策略 1:1） |
| beginDateTime / endDateTime | 活动有效期 |
| state | 活动状态（open/close/draft） |

#### raffle_activity_sku（活动SKU表）
| 字段 | 说明 |
|------|------|
| sku | 商品唯一编号 |
| activityId | 所属活动 |
| activityCountId | 绑定的次数配置ID |
| stockCount / stockCountSurplus | 商品总库存/剩余库存 |
| productAmount | 商品价格（积分值） |

#### raffle_activity_count（次数配置表）
记录各 SKU 对应的抽奖次数规格：totalCount（总次数）、dayCount（日限制）、monthCount（月限制）。

#### raffle_activity_account（总账户表）
| 字段 | 说明 |
|------|------|
| userId + activityId | 联合主键（用户+活动唯一） |
| totalCount | 用户持有总次数 |
| totalCountSurplus | 剩余总次数 |
| dayCount / dayCountSurplus | 日限额/剩余（冗余在总账户，供快速判断） |
| monthCount / monthCountSurplus | 月限额/剩余（冗余在总账户） |

#### raffle_activity_account_day / raffle_activity_account_month（日/月镜像账户）
| 字段 | 说明 |
|------|------|
| userId + activityId + day/month | 联合主键 |
| dayCountSurplus / monthCountSurplus | 当期剩余次数 |

**设计亮点**：镜像账户在当期首次使用时按当期上限创建，天然隔离不同周期的配额，无需定时重置任务。

#### raffle_activity_order（充值订单表）
记录用户购买 SKU 的订单，包含 `orderId`（幂等）、`sku`、支付状态等。

#### user_raffle_order（抽奖参与单）
| 字段 | 说明 |
|------|------|
| userId, activityId, strategyId | 参与主体 |
| orderId | 抽奖单号（幂等） |
| orderState | create（已创建未抽奖） |
| endDateTime | 活动截止时间（冗余，方便超时处理） |

// __CONTINUE_HERE__