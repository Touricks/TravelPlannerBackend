1. 目标与范围确认
   现有行程服务的核心目标是通过 POST /api/itineraries 创建携带 AI 推荐 POI 的行程，保证对请求结构、天数与 POI 数的业务约束，并以 201 状态完成持久化流程。

新增 LightRAG 能力的目标，是在保持上述端到端流程的前提下，将用户偏好图谱检索与上下文富集注入推荐链路，使生成的 POI 更具个性化和可解释性。

2. 现有流程盘点
   ItineraryController 已暴露 /api/itineraries，直接委托 ItineraryService.createItinerary 处理验证、生成与错误日志。

ItineraryServiceImpl 负责校验请求、计算行程天数与推荐数、创建行程实体、调用 LLM 生成 POI，并写回 AI 元数据和持久化结果，是我们嵌入 LightRAG 的最佳编排点。

LangChain4jLLMService 对接 POIRecommendationService，内置多次重试、错误记录以及请求参数组装，输出结构化的 PlaceDTO 列表。

POIRecommendationService 定义了当前的系统消息、用户消息模版，以及带错误反馈的重试接口，为后续上下文增强提供插入点。

POIService 把 LLM 结果转换为 PlaceEntity 并与行程建立关联，负责生成数据的落库与日志。

ItineraryEntity 已包含 aiMetadata、seededRecommendations 等 JSONB 字段，可用于记录 LightRAG 检索到的上下文与生成结果摘要。

InterestServiceImpl 维护用户对行程地点的 pinned 状态，为构建偏好图谱提供直接的历史行为数据来源。

3. 核心设计议题
   知识图谱建模：围绕 User、Interest、Place、Category、Feature、Destination、TravelProfile 等节点以及兴趣、类别、特征、地理位置等关系构建偏好图谱，确保能够表达 request + 历史行为的融合上下文。

LightRAG 检索链路：设计“实体级 + 关系级”双层检索，先定位与请求匹配的节点，再通过多跳遍历补充相似地点、协同偏好与预算/出行模式模式，从而生成最终上下文包。

上下文富集与提示增强：将 request、图谱检索结果、历史模式等内容结构化注入系统消息与用户消息，使 LLM 生成更加贴合用户画像，并保留可解释字段以便前端展示。

图谱更新反馈：在行程生成与用户后续互动（例如 pinned）后，将新兴趣、行为和推荐结果增量写回图谱，持续改进下一次检索效果。

4. 服务与模块边界
   新增 LightRAGService（检索 + 更新）与 UserPreferenceContext 等模型层，用于封装图谱交互，对外暴露检索上下文与推荐扩展结果。

在 ItineraryServiceImpl.createItinerary 中插入对 LightRAGService 的调用：先获取上下文→增强 LLM 提示→生成 POI→在成功后回写图谱，保持原有事务和日志结构。

对 LangChain4jLLMService 或 POIRecommendationService 进行扩展，使其接收 LightRAG 提供的上下文参数，保持重试与转换逻辑不变。

保持 POIService 的持久化职责不变，同时为图谱更新提供所需的 place 元数据快照。

5. 数据契约与持久化
   请求侧继续依赖 CreateItineraryRequest 中的目的地、时间、预算、交通方式、日程约束，并可扩展附加偏好字段以丰富图谱检索输入。

行程实体的 aiMetadata、seededRecommendations 等 JSONB 字段可记录 LightRAG 输入、生成摘要、错误信息等，用于调试和前端展示。

需要定义 LightRAG 上下文到 POIRecommendationService 模版的映射规则，确保系统消息/用户消息能够安全插入新字段并兼容现有校验。

图谱更新流程应消费 POIService 保存后的 PlaceEntity 数据，带上 pinned 状态或推荐来源，以支持后续关系权重调整。

6. 横切关注点
   回退策略：当 LightRAG 检索失败或图谱不可用时，可落回现有 LangChain4jLLMService 多次重试机制，避免阻塞行程创建。

错误透明度：沿用 ItineraryServiceImpl 的日志与 aiMetadata 记录方式，详细记录 LightRAG 调用、上下文注入、生成失败等情况，便于排查与用户提示。

权限与数据隔离：依靠现有的用户验证和 pinned 校验逻辑，确保图谱操作只能针对当前登录用户的数据进行更新