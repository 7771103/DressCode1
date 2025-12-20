# 和风天气API集成说明

## ✅ 快速开始（已完成配置）

**API Key已获取：** `45b4b6ab84dc478fb32c6e0f7989d16c`

### 立即使用步骤：

1. **在 `backend` 目录下创建 `.env` 文件**，内容如下：
   ```
   QWEATHER_API_KEY=45b4b6ab84dc478fb32c6e0f7989d16c
   ```

2. **安装依赖**（如果还没有安装）：
   ```bash
   cd backend
   pip install -r requirements.txt
   ```

3. **启动后端服务**，天气API将自动使用配置的API Key。

4. **前端已配置完成**：
   - 首页天气定位页面会自动使用定位到的城市查询天气
   - 天气信息会实时显示温度和天气状况

---

## 需要准备的东西

### 1. 和风天气API Key

1. **注册账号**
   - 访问和风天气开发者平台：https://dev.qweather.com/
   - 注册并登录账号

2. **创建项目**
   - 登录后，在控制台创建新项目
   - 选择"Web API"服务类型
   - 选择"免费订阅"计划（每天有免费调用次数）

3. **获取API Key**
   - **重要提示：** 项目ID（如：4EE26FX775）**不是** API Key
   
   - **方法一：在项目详情页查看（推荐）**
     1. 在"项目管理"页面，点击你的项目名称进入项目详情页
     2. 在项目详情页中查找"API Key"、"密钥"、"Key"或"开发密钥"字段
     3. API Key 通常会自动生成，直接显示在项目信息中
     4. 如果看不到，尝试点击"显示"、"查看"或眼睛图标来显示密钥
   
   - **方法二：通过创建凭据获取（如果方法一找不到）**
     1. 在项目设置页面，点击"+ 创建凭据"按钮
     2. 在"创建凭据"页面中，**选择"API KEY"选项**（不要选择JWT）
     3. 填写凭据名称（可以随意填写，如：我的API密钥）
     4. 点击"保存"按钮
     5. 创建成功后，会显示生成的API Key，复制并保存
   
   - **API Key 的特征：**
     - 通常是一串32位的字符串（字母和数字组合）
     - 格式类似：`a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6`
     - 与项目ID不同（项目ID通常更短，格式也不同）
   
   - **注意：** 复制并保存好这个Key（注意保密，不要泄露）

### 2. 配置环境变量

**方法一：使用 `.env` 文件（推荐）**

在 `backend` 目录下创建 `.env` 文件：
```
QWEATHER_API_KEY=45b4b6ab84dc478fb32c6e0f7989d16c
```

**注意：** `.env` 文件已经在 `.gitignore` 中，不会被提交到代码仓库。

**方法二：直接设置环境变量**

**Windows (PowerShell):**
```powershell
$env:QWEATHER_API_KEY="45b4b6ab84dc478fb32c6e0f7989d16c"
```

**Windows (CMD):**
```cmd
set QWEATHER_API_KEY=45b4b6ab84dc478fb32c6e0f7989d16c
```

**Linux/Mac:**
```bash
export QWEATHER_API_KEY="45b4b6ab84dc478fb32c6e0f7989d16c"
```

**注意：** 如果使用方法二，每次启动后端服务前都需要重新设置环境变量。

### 3. 安装依赖

确保已安装 `requests` 库：
```bash
cd backend
pip install -r requirements.txt
```

## API使用说明

### 接口地址
```
GET /api/weather
```

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| city | string | 否 | 城市名称，默认为"北京" |
| location | string | 否 | 经纬度，格式：经度,纬度（如：116.41,39.92） |

### 请求示例

```bash
# 通过城市名称查询
curl "http://localhost:5000/api/weather?city=上海"

# 通过经纬度查询
curl "http://localhost:5000/api/weather?location=121.47,31.23"
```

### 返回数据格式

**成功响应：**
```json
{
  "ok": true,
  "data": {
    "city": "上海",
    "locationId": "101020100",
    "temperature": "25",
    "feelsLike": "27",
    "condition": "多云",
    "icon": "101",
    "humidity": "65",
    "windSpeed": "15",
    "windDir": "东北风",
    "pressure": "1013",
    "vis": "16",
    "updateTime": "2024-01-01T12:00+08:00",
    "forecast": [
      {
        "date": "2024-01-02",
        "tempMax": "28",
        "tempMin": "20",
        "textDay": "晴",
        "textNight": "多云",
        "iconDay": "100"
      }
    ]
  }
}
```

**失败响应：**
```json
{
  "ok": false,
  "msg": "错误信息"
}
```

## 注意事项

1. **免费额度限制**
   - 和风天气免费版每天有调用次数限制
   - 建议合理使用，避免超出限制

2. **API Key安全**
   - 不要将API Key提交到代码仓库
   - 使用环境变量或配置文件管理
   - 如果使用Git，确保 `.env` 文件在 `.gitignore` 中

3. **错误处理**
   - 如果未配置API Key，接口会返回模拟数据
   - 网络错误或API错误会返回相应的错误信息

4. **城市名称**
   - 支持中文城市名称（如：北京、上海）
   - 也支持英文城市名称
   - 如果城市名称不准确，可能返回404错误

## 测试

启动后端服务后，可以通过以下方式测试：

```bash
# 测试北京天气
curl "http://localhost:5000/api/weather?city=北京"

# 测试上海天气
curl "http://localhost:5000/api/weather?city=上海"
```

## 更多功能

和风天气API还提供以下功能（可根据需要扩展）：
- 24小时天气预报
- 7天天气预报
- 生活指数（穿衣、运动、洗车等）
- 空气质量
- 灾害预警

如需添加这些功能，可以参考和风天气官方文档：https://dev.qweather.com/docs/api/

