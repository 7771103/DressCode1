# MCP+RAG+LLM 智能体系统实现总结

## 实现概述

已成功实现基于 **MCP + RAG + LLM** 的智能穿搭推荐系统，能够根据用户查询自动获取天气信息，从数据库检索相关帖子，并生成个性化推荐。

## 核心组件

### 1. MCP工具模块 (`mcp_tools.py`)
- **WeatherTool**: 封装天气API调用
  - `get_weather()`: 获取指定城市的天气信息
  - `get_user_city()`: 获取用户所在城市
  - 支持城市名称和经纬度查询
  - 自动处理API错误和超时

### 2. RAG检索模块 (`rag_retriever.py`)
- **RAGRetriever**: 从数据库检索相关帖子
  - `retrieve_by_weather()`: 根据天气条件检索（温度、天气状况、季节）
  - `retrieve_by_keywords()`: 根据关键词在内容和标签中搜索
  - `retrieve_by_tags()`: 根据标签列表检索
  - 支持城市筛选和结果去重

### 3. 智能体主逻辑 (`agent.py`)
- **DressCodeAgent**: 整合MCP+RAG+LLM的核心类
  - `process_query()`: 主处理函数，协调整个流程
  - `_analyze_intent()`: 使用LLM分析用户意图
  - `_retrieve_posts()`: 根据意图和天气信息检索帖子
  - `_generate_response()`: 使用LLM生成个性化回复
  - 多层fallback机制，确保系统稳定性

### 4. API接口更新 (`app.py`)
- 更新 `/api/chat` 接口，集成智能体系统
- 返回格式包含：
  - 回复文本
  - 推荐帖子列表（带链接）
  - 天气信息（如果查询了天气）

## 工作流程

```
用户查询："最近有什么穿搭推荐？"
    ↓
[LLM意图分析]
  - 识别需要天气信息
  - 提取关键词和标签
    ↓
[MCP工具调用]
  - 获取用户城市（从用户表）
  - 调用天气API获取当前天气
    ↓
[RAG检索]
  - 根据温度判断季节（如：25°C → 夏季）
  - 根据天气状况匹配标签（如：晴朗 → "晴朗"标签）
  - 从数据库检索匹配的帖子
    ↓
[LLM生成回复]
  - 结合天气信息和帖子信息
  - 生成自然、友好的推荐回复
    ↓
返回结果
  - 回复文本
  - 推荐帖子列表（最多5条）
  - 天气信息
```

## 文件结构

```
backend/
├── agent.py              # 智能体主逻辑
├── mcp_tools.py          # MCP工具（天气API）
├── rag_retriever.py      # RAG检索模块
├── app.py                # Flask应用（已更新chat接口）
├── requirements.txt      # 依赖（已更新）
├── AGENT_README.md       # 使用说明
├── IMPLEMENTATION_SUMMARY.md  # 本文档
└── test_agent.py         # 测试脚本
```

## 依赖项

新增依赖：
- `dashscope==1.17.0`: 阿里云DashScope SDK（用于LLM）
- `openai==1.12.0`: OpenAI SDK（备用）
- `numpy==1.26.4`: 数值计算
- `sentence-transformers==2.3.1`: 文本嵌入（未来可扩展）

## 环境变量配置

需要在 `.env` 文件中配置：
```env
# 和风天气API Key
QWEATHER_API_KEY=your_key_here

# 阿里云DashScope API Key
DASHSCOPE_API_KEY=your_key_here
```

## 使用示例

### API调用示例

```bash
curl -X POST http://localhost:5000/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "message": "最近有什么穿搭推荐？",
    "history": []
  }'
```

### 响应示例

```json
{
  "ok": true,
  "data": {
    "role": "assistant",
    "content": "根据北京的天气情况（25°C，晴朗），我为你找到了5条相关穿搭推荐...",
    "weather": {
      "city": "北京",
      "temperature": 25,
      "condition": "晴朗",
      "forecast": [...]
    },
    "posts": [
      {
        "id": 1,
        "link": "/api/posts/1",
        "tags": ["夏", "晴朗", "休闲"],
        ...
      }
    ]
  }
}
```

## 关键特性

### 1. 智能意图识别
- 自动识别用户是否需要天气信息
- 提取关键词、标签、城市等信息
- 支持多种查询类型（推荐、搜索、问题）

### 2. 多条件检索
- 优先根据天气条件检索
- 如果结果不足，使用关键词/标签检索
- 支持城市筛选
- 自动去重和排序

### 3. 个性化回复
- 结合天气信息给出建议
- 自然地推荐相关帖子
- 说明推荐理由
- 友好的对话风格

### 4. 容错机制
- LLM调用失败时使用关键词匹配
- 天气API失败时返回模拟数据
- 检索失败时返回最近帖子
- 多层fallback确保系统稳定

## 数据库要求

确保 `posts` 表的 `tags` 字段包含结构化标签：
```json
{
  "tags": ["夏", "晴朗", "休闲", "约会"]
}
```

支持的标签类型：
- 季节：春、夏、秋、冬
- 天气：晴朗、雨天、阴天、雪天、多云
- 风格：休闲、正式、运动、甜美等
- 场景：通勤、约会、旅行等

## 测试

运行测试脚本：
```bash
cd backend
python test_agent.py
```

## 未来扩展

### 1. 向量数据库集成
- 使用Chroma或FAISS存储帖子向量
- 实现语义相似度检索
- 提高检索精度

### 2. 多模态支持
- 使用Qwen-VL分析图片内容
- 根据图片特征推荐相似穿搭

### 3. 用户画像
- 记录用户偏好
- 个性化推荐
- 推荐历史记录

### 4. 缓存优化
- 缓存天气信息（避免频繁调用）
- 缓存检索结果
- 提高响应速度

## 注意事项

1. **API成本**：注意控制LLM和天气API的调用频率
2. **数据质量**：标签质量直接影响检索效果
3. **性能**：大量数据时考虑添加索引和缓存
4. **安全性**：API Key不要提交到代码仓库

## 总结

成功实现了完整的MCP+RAG+LLM智能体系统，能够：
- ✅ 自动查询天气（MCP）
- ✅ 智能检索帖子（RAG）
- ✅ 生成个性化回复（LLM）
- ✅ 返回推荐帖子链接
- ✅ 多层容错机制

系统已集成到现有的 `/api/chat` 接口，可以直接使用。


