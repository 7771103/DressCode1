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

# 通义千问API配置（用于物品类型识别，chat/completions API）
# 获取方式：访问 https://dashscope.console.aliyun.com/
# 登录后进入「API-KEY管理」页面，创建或查看您的API Key
QWEN_API_KEY=your_qwen_api_key_here
QWEN_MODEL=qwen-vl-max
QWEN_API_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions

# 豆包API配置（用于换装功能，images/generations API）
# ⚠️ 注意：豆包API只支持images/generations，不支持chat/completions
# 获取API Key：访问 https://console.volcengine.com/ark/
# 登录后进入「API Key 管理」页面，创建或查看您的API Key
DOUBAO_API_KEY=your_doubao_api_key_here

# 获取接入点ID：访问 https://console.volcengine.com/ark/
# 登录后进入「推理接入点」页面，创建推理接入点后复制接入点ID（格式：ep-xxxxx）
DOUBAO_MODEL_ID=ep-xxxxx

# API端点配置（用于换装功能，图像生成API）
DOUBAO_API_URL=https://ark.cn-beijing.volces.com/api/v3/images/generations
```

### 重要配置说明

#### DOUBAO_MODEL_ID（必需）

**这是换装功能必需的配置！**

1. 登录 [火山方舟平台](https://console.volcengine.com/ark/)
2. 创建推理接入点
3. 获取接入点ID（格式：`ep-xxxxx`）
4. 将接入点ID填入 `.env` 文件中的 `DOUBAO_MODEL_ID`

**如果没有配置此变量，换装功能将无法使用！**

#### QWEN_API_KEY（推荐）

**用于物品类型识别（配饰 vs 服装）**

1. 访问 [阿里云DashScope控制台](https://dashscope.console.aliyun.com/)
2. 登录后进入「API-KEY管理」页面
3. 创建或查看您的API Key
4. 将API Key填入 `.env` 文件中的 `QWEN_API_KEY`

**如果没有配置此变量，物品类型识别功能将无法使用，所有物品将按服装处理。**

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
$env:DOUBAO_API_KEY="your_doubao_api_key"
$env:QWEN_API_KEY="your_qwen_api_key"
$env:QWEN_MODEL="qwen-vl-max"
```

**Windows CMD:**
```cmd
set DOUBAO_MODEL_ID=ep-xxxxx
set DOUBAO_API_KEY=your_doubao_api_key
set QWEN_API_KEY=your_qwen_api_key
set QWEN_MODEL=qwen-vl-max
```

**Linux/Mac:**
```bash
export DOUBAO_MODEL_ID=ep-xxxxx
export DOUBAO_API_KEY=your_doubao_api_key
export QWEN_API_KEY=your_qwen_api_key
export QWEN_MODEL=qwen-vl-max
```

### API用途说明

- **豆包API（DOUBAO_*）**：仅用于换装功能，使用 `images/generations` API端点
- **通义千问API（QWEN_*）**：用于物品类型识别，使用 `chat/completions` API端点

