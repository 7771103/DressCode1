# 快速开始指南

## 5分钟快速上手

### 步骤1：安装依赖

```bash
cd backend
pip install -r requirements.txt
```

### 步骤2：配置环境变量

在 `backend` 目录下创建 `.env` 文件：

```env
# 必需：阿里云DashScope API Key（用于LLM）
DASHSCOPE_API_KEY=sk-your-key-here

# 必需：和风天气API Key（用于天气查询）
QWEATHER_API_KEY=your-qweather-key-here
```

**获取API Key：**
- DashScope: https://dashscope.aliyun.com/
- 和风天气: https://dev.qweather.com/ (参考 `WEATHER_API_SETUP.md`)

### 步骤3：启动服务

```bash
python app.py
```

服务将在 `http://localhost:5000` 启动。

### 步骤4：测试智能体

#### 方法1：使用curl

```bash
curl -X POST http://localhost:5000/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "message": "最近有什么穿搭推荐？"
  }'
```

#### 方法2：使用Python测试脚本

```bash
python test_agent.py
```

### 步骤5：查看响应

成功响应示例：

```json
{
  "ok": true,
  "data": {
    "role": "assistant",
    "content": "根据北京的天气情况（25°C，晴朗），我为你找到了5条相关穿搭推荐...",
    "weather": {
      "city": "北京",
      "temperature": 25,
      "condition": "晴朗"
    },
    "posts": [
      {
        "id": 1,
        "link": "/api/posts/1",
        "tags": ["夏", "晴朗", "休闲"]
      }
    ]
  }
}
```

## 常见问题

### Q: LLM API调用失败怎么办？
A: 检查 `DASHSCOPE_API_KEY` 是否正确，系统会自动降级到关键词匹配。

### Q: 天气API调用失败怎么办？
A: 检查 `QWEATHER_API_KEY` 是否正确，系统会返回模拟数据。

### Q: 检索不到帖子怎么办？
A: 确保数据库中有帖子数据，且 `tags` 字段格式正确（JSON数组）。

### Q: 如何查看日志？
A: 日志会输出到控制台，包含详细的处理流程信息。

## 下一步

- 阅读 `AGENT_README.md` 了解详细功能
- 阅读 `IMPLEMENTATION_SUMMARY.md` 了解实现细节
- 根据需求扩展功能（见 `AGENT_README.md` 的"扩展功能"部分）

## 示例查询

试试这些查询：

1. **天气相关推荐**
   - "最近有什么穿搭推荐？"
   - "今天天气怎么样？有什么穿搭建议？"
   - "北京最近有什么穿搭推荐？"

2. **场景推荐**
   - "我要去约会，有什么推荐？"
   - "工作场合穿什么？"
   - "旅行穿搭推荐"

3. **风格推荐**
   - "休闲风格有什么推荐？"
   - "正式场合的穿搭"


