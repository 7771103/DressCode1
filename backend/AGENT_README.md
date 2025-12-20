# 智能体系统使用说明

## 概述

本系统实现了基于 **MCP + RAG + LLM** 的智能穿搭推荐智能体，能够：
- 自动查询天气信息（MCP工具）
- 从数据库检索相关帖子（RAG检索）
- 理解用户意图并生成个性化回复（LLM）

## 架构说明

### 1. MCP (Model Context Protocol) 工具
- **WeatherTool**: 封装天气API调用
  - 自动获取用户所在城市的天气
  - 支持根据城市名称或经纬度查询
  - 返回温度、天气状况、预报等信息

### 2. RAG (Retrieval-Augmented Generation) 检索
- **RAGRetriever**: 从数据库检索相关帖子
  - `retrieve_by_weather()`: 根据天气条件检索（温度、天气状况）
  - `retrieve_by_keywords()`: 根据关键词检索
  - `retrieve_by_tags()`: 根据标签检索

### 3. LLM (Large Language Model)
- 使用阿里云 DashScope Qwen 模型
- 意图分析：理解用户查询意图
- 回复生成：生成个性化、自然的回复

## 工作流程

```
用户查询："最近有什么穿搭推荐？"
    ↓
[LLM] 分析意图 → 识别需要天气信息
    ↓
[MCP] 调用天气工具 → 获取当前城市天气（如：北京，25°C，晴朗）
    ↓
[RAG] 根据天气检索 → 从数据库检索适合的帖子（标签包含"夏"、"晴朗"等）
    ↓
[LLM] 生成回复 → 结合天气和帖子信息，生成个性化推荐
    ↓
返回：回复文本 + 推荐帖子列表 + 天气信息
```

## 环境配置

### 1. 安装依赖

```bash
cd backend
pip install -r requirements.txt
```

### 2. 配置环境变量

在 `backend` 目录下创建 `.env` 文件：

```env
# 和风天气API Key（用于MCP工具）
QWEATHER_API_KEY=your_qweather_api_key

# 阿里云DashScope API Key（用于LLM）
DASHSCOPE_API_KEY=your_dashscope_api_key

# MySQL数据库配置（如果不在.env中）
MYSQL_USER=root
MYSQL_PASSWORD=123456
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=dresscode1
```

### 3. 获取API Key

#### 和风天气API Key
1. 访问 https://dev.qweather.com/
2. 注册并创建项目
3. 获取API Key（参考 `WEATHER_API_SETUP.md`）

#### DashScope API Key
1. 访问 https://dashscope.aliyun.com/
2. 注册并开通服务
3. 在控制台获取API Key

## API接口

### POST /api/chat

智能对话接口，支持天气查询和穿搭推荐。

#### 请求示例

```json
{
  "userId": 1,
  "message": "最近有什么穿搭推荐？",
  "history": []
}
```

#### 响应示例

```json
{
  "ok": true,
  "data": {
    "role": "assistant",
    "content": "根据北京的天气情况（25°C，晴朗），我为你找到了5条相关穿搭推荐。这些搭配都很适合当前的天气和场合...",
    "weather": {
      "city": "北京",
      "temperature": 25,
      "condition": "晴朗",
      "forecast": [...]
    },
    "posts": [
      {
        "id": 1,
        "userId": 1,
        "userNickname": "用户1",
        "imageUrl": "/uploads/image1.jpg",
        "content": "今天穿这套，很适合夏天！",
        "tags": ["夏", "晴朗", "休闲"],
        "link": "/api/posts/1",
        ...
      },
      ...
    ]
  }
}
```

## 使用示例

### 示例1：根据天气推荐

**用户查询：** "最近有什么穿搭推荐？"

**智能体处理：**
1. 分析意图 → 需要天气信息
2. 获取用户城市天气（如：北京，25°C，晴朗）
3. 检索适合的帖子（标签包含"夏"、"晴朗"）
4. 生成推荐回复

### 示例2：特定场景推荐

**用户查询：** "我要去约会，有什么推荐？"

**智能体处理：**
1. 分析意图 → 关键词：约会
2. 检索相关帖子（标签包含"约会"或内容包含"约会"）
3. 生成推荐回复

### 示例3：城市特定推荐

**用户查询：** "上海最近有什么穿搭推荐？"

**智能体处理：**
1. 分析意图 → 城市：上海，需要天气信息
2. 获取上海天气
3. 检索上海的帖子，结合天气条件
4. 生成推荐回复

## 数据库要求

确保帖子表（`posts`）中的 `tags` 字段包含以下信息：
- 季节标签：`["春", "夏", "秋", "冬"]`
- 天气标签：`["晴朗", "雨天", "阴天", "雪天", "多云"]`
- 风格标签：`["休闲", "正式", "运动", "甜美"]`
- 场景标签：`["通勤", "约会", "旅行"]`

示例：
```json
{
  "tags": ["夏", "晴朗", "休闲", "约会"]
}
```

## 故障排除

### 1. LLM API调用失败
- 检查 `DASHSCOPE_API_KEY` 是否正确配置
- 检查网络连接
- 系统会自动降级到简单的关键词匹配

### 2. 天气API调用失败
- 检查 `QWEATHER_API_KEY` 是否正确配置
- 检查API调用次数是否超限
- 系统会返回模拟数据

### 3. 检索不到帖子
- 检查数据库中是否有帖子数据
- 检查帖子的 `tags` 字段格式是否正确
- 系统会自动返回最近的帖子作为fallback

## 扩展功能

### 1. 添加新的MCP工具
在 `mcp_tools.py` 中添加新的工具类，然后在 `agent.py` 中集成。

### 2. 改进RAG检索
- 可以添加向量数据库（如：Chroma、FAISS）进行语义检索
- 可以添加embedding模型进行更精确的语义匹配

### 3. 优化LLM提示词
在 `agent.py` 中的 `_analyze_intent()` 和 `_generate_response()` 方法中优化提示词。

## 注意事项

1. **API调用成本**：LLM和天气API都有调用成本，注意控制调用频率
2. **数据质量**：帖子的标签质量直接影响检索效果，建议使用高质量标签
3. **性能优化**：如果帖子数量很大，建议添加缓存机制
4. **错误处理**：系统已实现多层fallback机制，确保即使部分功能失败也能正常工作


