# 后端配置说明

## 环境变量配置

后端服务需要配置以下环境变量才能正常运行。推荐使用 `.env` 文件进行配置。

### 创建 .env 文件

在 `backend` 目录下创建 `.env` 文件，内容如下：

```env
# 数据库配置
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=123456
MYSQL_DATABASE=dresscode1

# 豆包API配置
# 请从火山方舟平台获取以下配置信息
DOUBAO_API_KEY=your_api_key_here
DOUBAO_API_URL=https://ark.cn-beijing.volces.com/api/v3/chat/completions
DOUBAO_MODEL_ID=ep-xxxxx
```

### 重要配置说明

#### DOUBAO_MODEL_ID（必需）

**这是换装功能必需的配置！**

1. 登录 [火山方舟平台](https://console.volcengine.com/ark/)
2. 创建推理接入点
3. 获取接入点ID（格式：`ep-xxxxx`）
4. 将接入点ID填入 `.env` 文件中的 `DOUBAO_MODEL_ID`

**如果没有配置此变量，换装功能将无法使用！**

### 安装依赖

如果使用 `.env` 文件，需要安装 `python-dotenv`：

```bash
pip install python-dotenv
```

或者安装所有依赖：

```bash
pip install -r requirements.txt
```

### 其他配置方式

如果不使用 `.env` 文件，也可以通过系统环境变量设置：

**Windows PowerShell:**
```powershell
$env:DOUBAO_MODEL_ID="ep-xxxxx"
$env:DOUBAO_API_KEY="your_api_key"
```

**Windows CMD:**
```cmd
set DOUBAO_MODEL_ID=ep-xxxxx
set DOUBAO_API_KEY=your_api_key
```

**Linux/Mac:**
```bash
export DOUBAO_MODEL_ID=ep-xxxxx
export DOUBAO_API_KEY=your_api_key
```

