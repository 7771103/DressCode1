# 快速配置指南

## 问题：豆包接入点ID未配置

如果看到错误信息："豆包接入点ID未配置"，请按以下步骤操作：

### 步骤 1：安装依赖

```bash
cd backend
pip install python-dotenv
```

或者安装所有依赖：

```bash
pip install -r requirements.txt
```

### 步骤 2：创建 .env 文件

在 `backend` 目录下创建 `.env` 文件（如果不存在），内容如下：

```env
# 数据库配置
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=123456
MYSQL_DATABASE=dresscode1

# 豆包API配置
DOUBAO_API_KEY=e5ede485-932d-4de2-8334-57d8955b61b8
DOUBAO_API_URL=https://ark.cn-beijing.volces.com/api/v3/chat/completions
DOUBAO_MODEL_ID=ep-xxxxx
```

**重要：** 请将 `DOUBAO_MODEL_ID=ep-xxxxx` 中的 `ep-xxxxx` 替换为您从火山方舟平台获取的实际接入点ID。

### 步骤 3：获取接入点ID

1. 访问 [火山方舟控制台](https://console.volcengine.com/ark/)
2. 登录您的账号
3. 创建推理接入点
4. 复制接入点ID（格式：`ep-20241220102330-xxxxx`）
5. 将ID填入 `.env` 文件的 `DOUBAO_MODEL_ID` 字段

### 步骤 4：重启后端服务

配置完成后，重启后端服务：

```bash
python app.py
```

### 验证配置

如果配置正确，换装功能应该可以正常使用。如果仍有问题，请检查：

1. `.env` 文件是否在 `backend` 目录下
2. `DOUBAO_MODEL_ID` 的值是否正确（必须以 `ep-` 开头）
3. 是否已安装 `python-dotenv` 包
4. 后端服务是否已重启

### 更多信息

详细配置说明请参考 `backend/CONFIG.md` 文件。

