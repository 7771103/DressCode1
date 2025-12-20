# 豆包API配置详细指南

## 问题诊断

如果API调用失败，请按以下步骤检查配置：

### 1. 检查API Key配置

**问题：** API Key未配置或使用了示例值

**解决步骤：**

1. 登录 [火山方舟控制台](https://console.volcengine.com/ark/)
2. 进入 **「API Key 管理」** 页面
3. 找到您的API Key（如：`dresscode换装`）
4. 点击 **「查看」** 或 **「复制」** 按钮获取API Key的值
5. **重要：** API Key的值通常是一串很长的字符串，**不是** `e5ede485-932d-4de2-8334-57d8955b61b8` 这样的示例值
6. 在 `backend/.env` 文件中设置：
   ```env
   DOUBAO_API_KEY=您的真实API Key值
   ```

**常见错误：**
- ❌ 使用示例值：`DOUBAO_API_KEY=e5ede485-932d-4de2-8334-57d8955b61b8`
- ✅ 使用真实值：`DOUBAO_API_KEY=ak-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### 2. 检查接入点ID配置

**问题：** 接入点ID未配置或格式错误

**解决步骤：**

1. 登录 [火山方舟控制台](https://console.volcengine.com/ark/)
2. 进入 **「推理接入点」** 页面
3. 找到您的接入点（如：`dresscode换装`）
4. 复制 **「接入点 ID」**（格式：`ep-20251220184236-nr5vt`）
5. 在 `backend/.env` 文件中设置：
   ```env
   DOUBAO_MODEL_ID=ep-20251220184236-nr5vt
   ```

**重要提示：**
- ✅ 接入点ID必须以 `ep-` 开头
- ✅ 格式示例：`ep-20251220184236-nr5vt`
- ❌ 不要使用模型名称（如：`doubao-seedream-4-5`）
- ❌ 不要使用示例值（如：`ep-xxxxx`）

### 3. 检查API Key权限

**问题：** API Key没有权限访问接入点

**解决步骤：**

1. 在 **「API Key 管理」** 页面，找到您的API Key
2. 点击 **「编辑权限」**
3. 在 **「访问权限范围」** 中，确保：
   - ✅ 您的接入点（如：`dresscode换装`）已被勾选
   - ✅ 或者勾选 **「默认全选（包括后续新增接入点）」**
4. 点击 **「提交」** 保存

### 4. 完整的 .env 文件示例

```env
# 数据库配置
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=123456
MYSQL_DATABASE=dresscode1

# 豆包API配置
# ⚠️ 请替换为您的真实值
DOUBAO_API_KEY=ak-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx  # 从API Key管理页面获取
DOUBAO_API_URL=https://ark.cn-beijing.volces.com/api/v3/images/generations  # 图像生成API端点
DOUBAO_MODEL_ID=ep-20251220184236-nr5vt  # 从推理接入点页面获取
```

### 5. 验证配置

配置完成后，重启后端服务：

```bash
cd backend
python app.py
```

如果配置正确，您应该能看到：
```
[DEBUG] API配置检查通过
[DEBUG] API URL: https://ark.cn-beijing.volces.com/api/v3/images/generations
[DEBUG] 接入点ID: ep-20251220184236-nr5vt
[DEBUG] API Key: ak-xxxxx...xxxxx
```

### 6. 常见错误及解决方案

#### 错误1：API调用失败 (状态码: 401)
**原因：** API Key无效或未配置
**解决：** 检查 `DOUBAO_API_KEY` 是否为真实值

#### 错误2：API调用失败 (状态码: 403)
**原因：** API Key没有权限访问此接入点
**解决：** 在API Key管理页面，编辑权限，确保接入点已被勾选

#### 错误3：模型不支持此API（错误代码：InvalidParameter）
**错误信息示例：**
```
The parameter `model` specified in the request are not valid: 
the requested model doubao-seedream-4-5-251128 does not support this api.
```

**可能的原因：**
1. 使用了错误的API端点（如chat/completions，但Seedream模型需要使用images/generations）
2. 接入点配置的模型类型与API调用方式不匹配

**解决步骤：**
1. **确认API端点：**
   - Seedream虚拟试衣模型应使用图像生成API：`/api/v3/images/generations`
   - 确保 `backend/.env` 中的 `DOUBAO_API_URL` 设置为：
     ```
     DOUBAO_API_URL=https://ark.cn-beijing.volces.com/api/v3/images/generations
     ```

2. **检查接入点配置：**
   - 登录 [火山方舟控制台](https://console.volcengine.com/ark/)
   - 进入「推理接入点」页面
   - 找到您的接入点，查看「模型类型」和「支持的API类型」
   - 确认该接入点是否支持 `images/generations` API

3. **验证配置：**
   - 确保使用的是接入点ID（格式：`ep-xxxxx`），而不是模型名称
   - 确保API URL指向 `images/generations` 端点
   - 重启后端服务，确保新配置生效

**注意：** Seedream模型用于图像编辑/换装，应使用图像生成API端点，而不是对话API端点。

#### 错误4：接入点ID格式错误
**原因：** 接入点ID不以 `ep-` 开头
**解决：** 从推理接入点页面复制完整的接入点ID

### 7. 获取帮助

如果仍然遇到问题，请检查后端日志中的详细错误信息：
- 查看控制台输出的 `[ERROR]` 和 `[DEBUG]` 信息
- 错误信息会包含具体的失败原因和解决建议

