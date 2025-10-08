# Planning 模块技术文档

## 概述

Planning 模块是旅行规划系统的核心功能之一，负责将用户的行程信息和感兴趣的地点通过 AI 模型（MCP）生成优化的旅行计划。

## 架构设计

### 模块结构

```
org.laioffer.planner.planning/
├── PlanningController.java           # API 控制器
├── PlanningService.java              # 服务接口
├── PlanningServiceImpl.java          # 服务实现
├── exception/
│   └── ItineraryNotFoundException.java
└── ai/
    ├── McpAiClient.java              # AI 客户端接口
    ├── McpAiClientImpl.java          # AI 客户端实现
    └── model/
        ├── AiPlanRequest.java        # AI 请求 DTO
        ├── AiPlanResponse.java       # AI 响应 DTO
        ├── AiPlaceInfo.java          # 地点信息 DTO
        ├── AiPlannedDay.java         # 每日计划 DTO
        └── AiPlannedStop.java        # 站点详情 DTO
```

---

## 完整工作流程

### 1. API 入口

**端点**: `POST /v1/itineraries/{itineraryId}/plan`

**请求参数**:
- **路径参数**: `itineraryId` (UUID) - 行程的唯一标识符
- **请求体**: `PlanItineraryRequest`
  ```json
  {
    "interestPlaceIds": ["uuid-1", "uuid-2", "uuid-3"]  // 可选
  }
  ```

**响应**: `PlanItineraryResponse` - 包含按天组织的详细旅行计划

---

### 2. 业务逻辑流程

#### 步骤 1: 获取行程信息

```java
ItineraryEntity itinerary = itineraryRepository.findById(itineraryId)
    .orElseThrow(() -> new ItineraryNotFoundException(...));
```

**获取的行程信息包括**:
- 目的地城市 (`destinationCity`)
- 旅行起止日期 (`startDate`, `endDate`)
- 旅行模式 (`travelMode`: WALKING, DRIVING, TRANSIT 等)
- 预算 (`budgetInCents`: 以分为单位)
- 每日活动时间 (`dailyStart`, `dailyEnd`: 如 09:00-20:00)

**异常处理**:
- 如果行程不存在，抛出 `ItineraryNotFoundException`

---

#### 步骤 2: 获取用户感兴趣的地点

```java
List<ItineraryPlaceEntity> interestedPlaces;
if (CollectionUtils.isEmpty(request.getInterestPlaceIds())) {
    // 模式 A: 获取该行程下所有的兴趣点
    interestedPlaces = itineraryPlaceRepository.findAllByItineraryId(itineraryId);
} else {
    // 模式 B: 只获取指定的地点
    List<UUID> placeIds = request.getInterestPlaceIds().stream()
        .map(UUID::fromString).toList();
    interestedPlaces = itineraryPlaceRepository.findAllByItineraryIdAndIdIn(itineraryId, placeIds);
}
```

**支持两种模式**:
- **全部兴趣点模式**: 使用用户之前添加的所有感兴趣地点
- **指定地点模式**: 只使用本次请求中指定的地点列表

---

#### 步骤 3: 构建 AI 请求

**方法**: `buildAiRequest(itinerary, interestedPlaces)`

##### 3.1 转换行程约束条件

将 `ItineraryEntity` 的信息映射到 `AiPlanRequest`:

| 行程字段 | AI 请求字段 | 说明 |
|---------|------------|------|
| destinationCity | destinationCity | 目的地城市 |
| startDate | startDate | 旅行开始日期 |
| endDate | endDate | 旅行结束日期 |
| travelMode | travelMode | 旅行模式（转为字符串） |
| budgetInCents | budgetInCents | 预算（分） |
| dailyStart | dailyStart | 每日开始时间 |
| dailyEnd | dailyEnd | 每日结束时间 |

##### 3.2 转换地点信息

**方法**: `convertToAiPlaceInfo(itineraryPlace)`

对每个 `ItineraryPlaceEntity`，提取并转换为 `AiPlaceInfo`:

```java
AiPlaceInfo {
    placeId: UUID           // 地点唯一ID
    name: String            // 地点名称
    address: String         // 详细地址
    latitude: BigDecimal    // 纬度
    longitude: BigDecimal   // 经度
    description: String     // 地点描述
    pinned: boolean         // 是否标记为必去
    note: String            // 用户备注
}
```

**数据来源**:
- 基础信息来自 `ItineraryPlaceEntity`
- 详细信息（地址、坐标、描述）来自关联的 `PlaceEntity`

---

#### 步骤 4: 调用 AI 模型

```java
AiPlanResponse aiResponse = mcpAiClient.generatePlan(aiRequest);
```

**McpAiClient 实现**:
- 使用 Spring 的 `RestTemplate` 发起 HTTP POST 请求
- 目标 URL: 通过配置项 `ai.mcp.service.url` 指定
  - 默认值: `http://localhost:8080/ai/plan`
- 请求体: `AiPlanRequest` (JSON 序列化)
- 响应: `AiPlanResponse` (JSON 反序列化)

**AI 响应示例**:

```json
{
  "summary": "为期3天的北京文化之旅",
  "days": [
    {
      "date": "2025-10-15",
      "summary": "探索市中心历史文化区",
      "stops": [
        {
          "placeId": "550e8400-e29b-41d4-a716-446655440000",
          "placeName": "故宫博物院",
          "arrivalTime": "09:00",
          "departureTime": "12:00",
          "durationMinutes": 180,
          "activity": "参观古建筑和文物展览",
          "transportMode": "步行",
          "transportDurationMinutes": 15
        },
        {
          "placeId": "550e8400-e29b-41d4-a716-446655440001",
          "placeName": "景山公园",
          "arrivalTime": "12:30",
          "departureTime": "14:00",
          "durationMinutes": 90,
          "activity": "登高俯瞰紫禁城全景",
          "transportMode": "步行",
          "transportDurationMinutes": 10
        }
      ]
    }
  ]
}
```

---

#### 步骤 5: 处理 AI 响应

**方法**: `buildPlanResponse(aiResponse)`

##### 5.1 转换每日计划

**方法**: `convertToPlannedDay(aiDay)`

转换逻辑:

```java
PlannedDay {
    date: String              // LocalDate → String (如 "2025-10-15")
    summary: String           // 每日概要（可选）
    stops: List<PlannedStop>  // 该天的站点列表
}
```

##### 5.2 转换站点信息

**方法**: `convertToPlannedStop(aiStop)`

字段映射:

| AI 字段 | API 字段 | 转换逻辑 |
|---------|---------|---------|
| arrivalTime | arrivalLocal | LocalTime → String |
| departureTime | departLocal | LocalTime → String |
| durationMinutes | stayMinutes | 直接映射 |
| activity | note | 活动描述作为备注 |

> **注意**: 当前实现中未完整填充 `PlaceDTO` 对象，后续可能需要从数据库查询完整地点信息。

---

#### 步骤 6: 返回最终计划

```java
return buildPlanResponse(aiResponse);
```

- `PlanningServiceImpl` 返回 `PlanItineraryResponse`
- `PlanningController` 将其包装在 HTTP 200 响应中
- 前端接收完整的旅行计划

---

## 数据流转图

```
┌─────────────────────────────────────────────────────────────────┐
│                          前端应用                                 │
└────────────────────┬────────────────────────────────────────────┘
                     │ POST /v1/itineraries/{id}/plan
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│                    PlanningController                            │
│  - 接收 itineraryId + PlanItineraryRequest                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│                   PlanningServiceImpl                            │
├─────────────────────────────────────────────────────────────────┤
│  Step 1: 从数据库获取 ItineraryEntity                            │
│          ↓                                                       │
│  Step 2: 从数据库获取 List<ItineraryPlaceEntity>                 │
│          ↓                                                       │
│  Step 3: 构建 AiPlanRequest                                      │
│          - 行程约束 (日期、预算、时间等)                           │
│          - 地点列表 (名称、坐标、描述等)                           │
│          ↓                                                       │
│  Step 4: 调用 McpAiClient.generatePlan()                         │
└────────────────────┬────────────────────────────────────────────┘
                     │ HTTP POST
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│                      MCP AI 服务                                 │
│  - 分析行程约束                                                   │
│  - 优化地点顺序                                                   │
│  - 计算路线和时间                                                 │
│  - 生成每日计划                                                   │
└────────────────────┬────────────────────────────────────────────┘
                     │ AiPlanResponse
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│                   PlanningServiceImpl                            │
│  Step 5: 转换 AI 响应为 API 格式                                  │
│          - AiPlannedDay → PlannedDay                             │
│          - AiPlannedStop → PlannedStop                           │
│          ↓                                                       │
│  Step 6: 返回 PlanItineraryResponse                              │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│                          前端应用                                 │
│  显示优化后的旅行计划                                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## 核心功能特性

### ✅ 智能路线优化
AI 模型会综合考虑：
- 地点间的地理距离
- 地点的开放时间
- 推荐的游玩时长
- 最优的交通方式

### ✅ 时间约束管理
- 遵守用户设定的每日活动时间（如 9:00-20:00）
- 自动分配每个地点的到达和离开时间
- 确保行程不会过于紧凑或松散

### ✅ 预算感知
- AI 可根据预算调整推荐的活动类型
- 优化交通方式选择（如预算紧张时推荐公共交通）

### ✅ 灵活的地点选择
- **全量模式**: 使用所有用户感兴趣的地点
- **精选模式**: 仅使用请求中指定的地点子集

### ✅ 优先级处理
- `pinned` 标记的地点会被优先安排
- 确保必去景点不会被遗漏

### ✅ 异常处理
- 行程不存在: 返回 `404 Not Found` + `ItineraryNotFoundException`
- AI 服务不可用: 抛出相应异常（需配置错误处理）

---

## 配置说明

### 必需配置

在 `application.yml` 或 `application.properties` 中添加：

```yaml
ai:
  mcp:
    service:
      url: http://your-ai-service-domain/api/plan
```

或

```properties
ai.mcp.service.url=http://your-ai-service-domain/api/plan
```

### 默认值

如果未配置，默认使用：`http://localhost:8080/ai/plan`

---

## 依赖关系

### Spring Bean 依赖

```
PlanningController
    ↓
PlanningServiceImpl
    ├─→ ItineraryRepository
    ├─→ ItineraryPlaceRepository
    └─→ McpAiClient
            └─→ RestTemplate (配置在 AppConfig)
```

### 实体关系

```
ItineraryEntity (行程)
    ↓ 1:N
ItineraryPlaceEntity (行程-地点关联)
    ↓ N:1
PlaceEntity (地点详情)
```

---

## API 使用示例

### 请求示例 1: 使用所有兴趣点

```bash
POST /v1/itineraries/550e8400-e29b-41d4-a716-446655440000/plan
Content-Type: application/json

{
  "interestPlaceIds": []
}
```

### 请求示例 2: 使用指定地点

```bash
POST /v1/itineraries/550e8400-e29b-41d4-a716-446655440000/plan
Content-Type: application/json

{
  "interestPlaceIds": [
    "550e8400-e29b-41d4-a716-446655440001",
    "550e8400-e29b-41d4-a716-446655440002",
    "550e8400-e29b-41d4-a716-446655440003"
  ]
}
```

### 响应示例

```json
{
  "days": [
    {
      "date": "2025-10-15",
      "stops": [
        {
          "order": 1,
          "place": { /* PlaceDTO 对象 */ },
          "arrivalLocal": "09:00",
          "departLocal": "12:00",
          "stayMinutes": 180,
          "note": "参观古建筑和文物展览"
        }
      ]
    }
  ]
}
```

---

## 技术栈

- **框架**: Spring Boot
- **持久层**: Spring Data JPA (Hibernate)
- **数据库**: PostgreSQL (with PostGIS extension)
- **HTTP 客户端**: RestTemplate
- **JSON 序列化**: Jackson
- **依赖注入**: Spring IoC

---

## 完整流程示例

为了更好地理解整个系统的工作流程，以下是一个真实场景的完整模拟。

### 场景设定

**用户**: 张三，计划在北京进行为期 3 天的文化之旅

**旅行信息**:
- 目的地：北京
- 日期：2025-10-15 至 2025-10-17
- 预算：¥1500（150000 分）
- 每日活动时间：09:00 - 20:00
- 旅行模式：公共交通 + 步行

**感兴趣的地点**（用户已提前选择）:
1. 故宫博物院
2. 天安门广场
3. 景山公园
4. 天坛公园
5. 颐和园
6. 圆明园
7. 南锣鼓巷
8. 什刹海

---

### Step 1: 前端发起请求

```http
POST /v1/itineraries/a1b2c3d4-e5f6-7890-abcd-ef1234567890/plan HTTP/1.1
Host: api.travelplanner.com
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "interestPlaceIds": []
}
```

> **说明**: `interestPlaceIds` 为空，表示使用该行程下的所有感兴趣地点

---

### Step 2: 服务层查询数据库

#### 2.1 查询行程信息

**数据库查询**: `SELECT * FROM itineraries WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'`

**查询结果** (`ItineraryEntity`):

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": 1001,
  "destinationCity": "北京",
  "startDate": "2025-10-15T00:00:00+08:00",
  "endDate": "2025-10-17T23:59:59+08:00",
  "travelMode": "TRANSIT",
  "budgetInCents": 150000,
  "dailyStart": "09:00:00",
  "dailyEnd": "20:00:00",
  "createdAt": "2025-10-08T14:30:00",
  "updatedAt": "2025-10-08T14:30:00"
}
```

#### 2.2 查询感兴趣的地点

**数据库查询**: `SELECT * FROM itinerary_places WHERE itinerary_id = '...'`

**查询结果** (`List<ItineraryPlaceEntity>` - 共 8 个地点):

```json
[
  {
    "id": "place-link-001",
    "itineraryId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "placeId": "place-001",
    "name": "故宫博物院",
    "description": "中国明清两代的皇家宫殿，世界文化遗产",
    "pinned": true,
    "note": "必去景点，提前预约门票",
    "addedAt": "2025-10-08T15:00:00",
    "place": {
      "id": "place-001",
      "name": "故宫博物院",
      "address": "北京市东城区景山前街4号",
      "latitude": 39.916345,
      "longitude": 116.397026,
      "description": "世界上现存规模最大、保存最为完整的木质结构古建筑之一"
    }
  },
  {
    "id": "place-link-002",
    "itineraryId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "placeId": "place-002",
    "name": "天安门广场",
    "description": "世界上最大的城市广场之一",
    "pinned": true,
    "note": null,
    "addedAt": "2025-10-08T15:01:00",
    "place": {
      "id": "place-002",
      "name": "天安门广场",
      "address": "北京市东城区长安街",
      "latitude": 39.903828,
      "longitude": 116.391136,
      "description": "位于北京市中心，是中国的象征"
    }
  },
  {
    "id": "place-link-003",
    "placeId": "place-003",
    "name": "景山公园",
    "pinned": false,
    "place": {
      "latitude": 39.921629,
      "longitude": 116.395592
    }
  },
  {
    "id": "place-link-004",
    "placeId": "place-004",
    "name": "天坛公园",
    "pinned": false,
    "place": {
      "latitude": 39.882217,
      "longitude": 116.407395
    }
  },
  {
    "id": "place-link-005",
    "placeId": "place-005",
    "name": "颐和园",
    "pinned": true,
    "note": "想看昆明湖和长廊",
    "place": {
      "latitude": 39.999439,
      "longitude": 116.275352
    }
  },
  {
    "id": "place-link-006",
    "placeId": "place-006",
    "name": "圆明园",
    "pinned": false,
    "place": {
      "latitude": 40.007554,
      "longitude": 116.297662
    }
  },
  {
    "id": "place-link-007",
    "placeId": "place-007",
    "name": "南锣鼓巷",
    "pinned": false,
    "place": {
      "latitude": 39.937674,
      "longitude": 116.402893
    }
  },
  {
    "id": "place-link-008",
    "placeId": "place-008",
    "name": "什刹海",
    "pinned": false,
    "place": {
      "latitude": 39.939083,
      "longitude": 116.389441
    }
  }
]
```

---

### Step 3: 构建 AI 请求

**处理逻辑**: `buildAiRequest(itinerary, interestedPlaces)`

**生成的 AI 请求** (`AiPlanRequest`):

```json
{
  "destinationCity": "北京",
  "startDate": "2025-10-15T00:00:00+08:00",
  "endDate": "2025-10-17T23:59:59+08:00",
  "travelMode": "TRANSIT",
  "budgetInCents": 150000,
  "dailyStart": "09:00:00",
  "dailyEnd": "20:00:00",
  "interestedPlaces": [
    {
      "placeId": "place-001",
      "name": "故宫博物院",
      "address": "北京市东城区景山前街4号",
      "latitude": 39.916345,
      "longitude": 116.397026,
      "description": "世界上现存规模最大、保存最为完整的木质结构古建筑之一",
      "pinned": true,
      "note": "必去景点，提前预约门票"
    },
    {
      "placeId": "place-002",
      "name": "天安门广场",
      "address": "北京市东城区长安街",
      "latitude": 39.903828,
      "longitude": 116.391136,
      "description": "位于北京市中心，是中国的象征",
      "pinned": true,
      "note": null
    },
    {
      "placeId": "place-003",
      "name": "景山公园",
      "address": "北京市西城区景山西街44号",
      "latitude": 39.921629,
      "longitude": 116.395592,
      "description": "可登高俯瞰紫禁城全景",
      "pinned": false,
      "note": null
    },
    {
      "placeId": "place-004",
      "name": "天坛公园",
      "address": "北京市东城区天坛路",
      "latitude": 39.882217,
      "longitude": 116.407395,
      "description": "明清两代皇帝祭天祈谷的场所",
      "pinned": false,
      "note": null
    },
    {
      "placeId": "place-005",
      "name": "颐和园",
      "address": "北京市海淀区新建宫门路19号",
      "latitude": 39.999439,
      "longitude": 116.275352,
      "description": "中国现存规模最大的皇家园林",
      "pinned": true,
      "note": "想看昆明湖和长廊"
    },
    {
      "placeId": "place-006",
      "name": "圆明园",
      "address": "北京市海淀区清华西路28号",
      "latitude": 40.007554,
      "longitude": 116.297662,
      "description": "清代皇家园林遗址",
      "pinned": false,
      "note": null
    },
    {
      "placeId": "place-007",
      "name": "南锣鼓巷",
      "address": "北京市东城区南锣鼓巷",
      "latitude": 39.937674,
      "longitude": 116.402893,
      "description": "北京最古老的街区之一，现为文艺小店聚集地",
      "pinned": false,
      "note": null
    },
    {
      "placeId": "place-008",
      "name": "什刹海",
      "address": "北京市西城区羊房胡同",
      "latitude": 39.939083,
      "longitude": 116.389441,
      "description": "北京内城唯一一处具有开阔水面的开放景区",
      "pinned": false,
      "note": null
    }
  ]
}
```

---

### Step 4: AI 模型处理

**HTTP 请求**:

```http
POST /api/plan HTTP/1.1
Host: ai.mcp-service.internal
Content-Type: application/json

{请求体如上}
```

**AI 分析过程** (内部逻辑):

1. **日期分析**: 3 天行程（10月15日-17日）
2. **时间约束**: 每天 09:00-20:00，有效时间 11 小时
3. **地点分析**:
   - 8 个地点需要安排
   - 3 个必去地点（pinned）: 故宫、天安门、颐和园
   - 地点地理分布：市中心（故宫、天安门、景山、南锣鼓巷、什刹海）、西北郊（颐和园、圆明园）、南部（天坛）
4. **路线优化**:
   - Day 1: 市中心历史核心区（天安门 → 故宫 → 景山 → 南锣鼓巷 → 什刹海）
   - Day 2: 皇家园林（颐和园 → 圆明园）
   - Day 3: 天坛 + 机动时间
5. **交通规划**: 优先使用地铁，景点间步行
6. **时间分配**: 根据景点推荐游玩时长和开放时间

**AI 返回结果** (`AiPlanResponse`):

```json
{
  "summary": "三天两晚北京皇城文化深度游，探索明清皇家建筑与传统胡同文化，行程松弛有度，适合文化爱好者。",
  "days": [
    {
      "date": "2025-10-15",
      "summary": "Day 1: 探索天安门-故宫核心区，感受皇城威严与胡同风情",
      "stops": [
        {
          "placeId": "place-002",
          "placeName": "天安门广场",
          "arrivalTime": "09:00",
          "departureTime": "10:00",
          "durationMinutes": 60,
          "activity": "观看升旗仪式（如需要请提前到达），参观广场周边建筑，拍照留念",
          "transportMode": null,
          "transportDurationMinutes": 0
        },
        {
          "placeId": "place-001",
          "placeName": "故宫博物院",
          "arrivalTime": "10:15",
          "departureTime": "14:00",
          "durationMinutes": 225,
          "activity": "深度游览太和殿、中和殿、保和殿等三大殿，后宫区域和珍宝馆，建议按中轴线游览",
          "transportMode": "步行",
          "transportDurationMinutes": 15
        },
        {
          "placeId": "place-003",
          "placeName": "景山公园",
          "arrivalTime": "14:30",
          "departureTime": "16:00",
          "durationMinutes": 90,
          "activity": "登顶万春亭，360度俯瞰紫禁城全景，是拍摄故宫最佳机位",
          "transportMode": "步行",
          "transportDurationMinutes": 30
        },
        {
          "placeId": "place-007",
          "placeName": "南锣鼓巷",
          "arrivalTime": "16:45",
          "departureTime": "18:30",
          "durationMinutes": 105,
          "activity": "漫步胡同，探访文艺小店、咖啡馆和特色餐厅，品尝北京小吃",
          "transportMode": "地铁8号线",
          "transportDurationMinutes": 45
        },
        {
          "placeId": "place-008",
          "placeName": "什刹海",
          "arrivalTime": "19:00",
          "departureTime": "20:00",
          "durationMinutes": 60,
          "activity": "夜游什刹海，欣赏湖畔夜景，可选择在湖边酒吧小憩或乘船游览",
          "transportMode": "步行",
          "transportDurationMinutes": 30
        }
      ]
    },
    {
      "date": "2025-10-16",
      "summary": "Day 2: 西郊皇家园林巡礼，领略皇家造园艺术",
      "stops": [
        {
          "placeId": "place-005",
          "placeName": "颐和园",
          "arrivalTime": "09:00",
          "departureTime": "13:30",
          "durationMinutes": 270,
          "activity": "游览万寿山、佛香阁、长廊（728米彩画长廊）、昆明湖，建议租船游湖或沿湖漫步",
          "transportMode": "地铁4号线",
          "transportDurationMinutes": 60
        },
        {
          "placeId": "place-006",
          "placeName": "圆明园",
          "arrivalTime": "14:30",
          "departureTime": "17:30",
          "durationMinutes": 180,
          "activity": "参观大水法遗址等西洋楼景区，了解圆明园历史，感受皇家园林的沧桑变迁",
          "transportMode": "公交",
          "transportDurationMinutes": 60
        }
      ]
    },
    {
      "date": "2025-10-17",
      "summary": "Day 3: 天坛祈福，圆满结束北京文化之旅",
      "stops": [
        {
          "placeId": "place-004",
          "placeName": "天坛公园",
          "arrivalTime": "09:00",
          "departureTime": "12:00",
          "durationMinutes": 180,
          "activity": "游览祈年殿、回音壁、圜丘，观看老北京晨练文化，体验回音壁的奇妙声学效果",
          "transportMode": "地铁5号线",
          "transportDurationMinutes": 45
        }
      ]
    }
  ]
}
```

**AI 优化说明**:
- ✅ 必去景点全部安排（故宫、天安门、颐和园）
- ✅ 地理位置相近的景点安排在同一天（市中心 / 西郊分组）
- ✅ 遵守每日活动时间（09:00-20:00）
- ✅ 合理分配游览时长（故宫 3.75 小时，颐和园 4.5 小时）
- ✅ 优化交通路线（减少往返，主要使用地铁）
- ✅ 第 3 天安排较轻松，避免疲劳

---

### Step 5: 服务层转换响应

**处理逻辑**: `buildPlanResponse(aiResponse)`

**转换过程**:
1. 遍历 AI 返回的每一天 (`AiPlannedDay`)
2. 将日期从 `LocalDate` 转换为 `String`
3. 遍历每天的站点 (`AiPlannedStop`)
4. 将时间字段从 `LocalTime` 转换为 `String`
5. 将字段映射到 API 规范的 `PlannedStop` 格式

**最终 API 响应** (`PlanItineraryResponse`):

```json
{
  "days": [
    {
      "date": "2025-10-15",
      "stops": [
        {
          "order": 1,
          "place": null,
          "arrivalLocal": "09:00",
          "departLocal": "10:00",
          "stayMinutes": 60,
          "note": "观看升旗仪式（如需要请提前到达），参观广场周边建筑，拍照留念"
        },
        {
          "order": 2,
          "place": null,
          "arrivalLocal": "10:15",
          "departLocal": "14:00",
          "stayMinutes": 225,
          "note": "深度游览太和殿、中和殿、保和殿等三大殿，后宫区域和珍宝馆，建议按中轴线游览"
        },
        {
          "order": 3,
          "place": null,
          "arrivalLocal": "14:30",
          "departLocal": "16:00",
          "stayMinutes": 90,
          "note": "登顶万春亭，360度俯瞰紫禁城全景，是拍摄故宫最佳机位"
        },
        {
          "order": 4,
          "place": null,
          "arrivalLocal": "16:45",
          "departLocal": "18:30",
          "stayMinutes": 105,
          "note": "漫步胡同，探访文艺小店、咖啡馆和特色餐厅，品尝北京小吃"
        },
        {
          "order": 5,
          "place": null,
          "arrivalLocal": "19:00",
          "departLocal": "20:00",
          "stayMinutes": 60,
          "note": "夜游什刹海，欣赏湖畔夜景，可选择在湖边酒吧小憩或乘船游览"
        }
      ]
    },
    {
      "date": "2025-10-16",
      "stops": [
        {
          "order": 1,
          "place": null,
          "arrivalLocal": "09:00",
          "departLocal": "13:30",
          "stayMinutes": 270,
          "note": "游览万寿山、佛香阁、长廊（728米彩画长廊）、昆明湖，建议租船游湖或沿湖漫步"
        },
        {
          "order": 2,
          "place": null,
          "arrivalLocal": "14:30",
          "departLocal": "17:30",
          "stayMinutes": 180,
          "note": "参观大水法遗址等西洋楼景区，了解圆明园历史，感受皇家园林的沧桑变迁"
        }
      ]
    },
    {
      "date": "2025-10-17",
      "stops": [
        {
          "order": 1,
          "place": null,
          "arrivalLocal": "09:00",
          "departLocal": "12:00",
          "stayMinutes": 180,
          "note": "游览祈年殿、回音壁、圜丘，观看老北京晨练文化，体验回音壁的奇妙声学效果"
        }
      ]
    }
  ]
}
```

> **注意**: 当前实现中 `place` 字段为 `null`，后续优化将从数据库查询完整的 `PlaceDTO` 对象

---

### Step 6: 前端展示

前端接收到响应后，将按天展示行程：

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📅 Day 1 - 2025年10月15日 (周二)
探索天安门-故宫核心区，感受皇城威严与胡同风情
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🕘 09:00 - 10:00  |  📍 天安门广场  |  ⏱️ 1小时
└─ 观看升旗仪式（如需要请提前到达），参观广场周边建筑，拍照留念
    🚶 步行 15分钟 ↓

🕥 10:15 - 14:00  |  📍 故宫博物院  |  ⏱️ 3小时45分钟
└─ 深度游览太和殿、中和殿、保和殿等三大殿，后宫区域和珍宝馆
    🚶 步行 30分钟 ↓

🕞 14:30 - 16:00  |  📍 景山公园  |  ⏱️ 1小时30分钟
└─ 登顶万春亭，360度俯瞰紫禁城全景，是拍摄故宫最佳机位
    🚇 地铁8号线 45分钟 ↓

🕓 16:45 - 18:30  |  📍 南锣鼓巷  |  ⏱️ 1小时45分钟
└─ 漫步胡同，探访文艺小店、咖啡馆和特色餐厅，品尝北京小吃
    🚶 步行 30分钟 ↓

🕖 19:00 - 20:00  |  📍 什刹海  |  ⏱️ 1小时
└─ 夜游什刹海，欣赏湖畔夜景，可选择在湖边酒吧小憩或乘船游览

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📅 Day 2 - 2025年10月16日 (周三)
西郊皇家园林巡礼，领略皇家造园艺术
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🕘 09:00 - 13:30  |  📍 颐和园  |  ⏱️ 4小时30分钟
└─ 游览万寿山、佛香阁、长廊（728米彩画长廊）、昆明湖
    🚌 公交 60分钟 ↓

🕞 14:30 - 17:30  |  📍 圆明园  |  ⏱️ 3小时
└─ 参观大水法遗址等西洋楼景区，了解圆明园历史

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📅 Day 3 - 2025年10月17日 (周四)
天坛祈福，圆满结束北京文化之旅
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🕘 09:00 - 12:00  |  📍 天坛公园  |  ⏱️ 3小时
└─ 游览祈年殿、回音壁、圜丘，观看老北京晨练文化
```

---

### 流程总结

#### 数据转换链路

```
数据库实体 (Entity)
    ↓ 查询
ItineraryEntity + List<ItineraryPlaceEntity>
    ↓ buildAiRequest()
AiPlanRequest (AI 请求格式)
    ↓ HTTP POST
AI 模型处理
    ↓ HTTP Response
AiPlanResponse (AI 响应格式)
    ↓ buildPlanResponse()
PlanItineraryResponse (API 响应格式)
    ↓ JSON
前端展示
```

#### 关键性能指标（本次示例）

| 指标 | 数值 |
|-----|------|
| 行程天数 | 3 天 |
| 地点总数 | 8 个 |
| 必去地点 | 3 个 |
| 总游览时间 | 约 23.5 小时 |
| Day 1 站点数 | 5 个 |
| Day 2 站点数 | 2 个 |
| Day 3 站点数 | 1 个 |
| 平均每天游览时间 | 约 7.8 小时 |
| AI 处理时间 | < 2 秒（预估）|
| 数据库查询次数 | 2 次 |
| HTTP 请求次数 | 1 次（AI 调用）|

#### 用户价值

1. **节省规划时间**: 用户无需手动安排路线和时间，AI 自动生成优化方案
2. **专业路线建议**: 基于地理位置、开放时间等因素的智能优化
3. **灵活可调整**: 用户可以基于生成的计划进行微调
4. **详细活动指导**: 每个站点都有具体的活动建议
5. **交通指引**: 明确的交通方式和预估时长

---

## 后续优化建议

1. **完善 PlaceDTO 填充**: 在转换 `PlannedStop` 时，从数据库查询完整的地点信息
2. **缓存优化**: 对频繁访问的行程和地点信息添加缓存
3. **异步处理**: 对于复杂行程，考虑使用异步生成计划
4. **重试机制**: 为 AI 服务调用添加重试和降级策略
5. **日志增强**: 添加详细的业务日志，便于追踪和调试
6. **性能监控**: 记录 AI 调用的响应时间和成功率
7. **用户反馈循环**: 收集用户对计划的评价，优化 AI 模型

---

