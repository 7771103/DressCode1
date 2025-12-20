# 虚拟试衣功能使用说明

## 功能概述

已实现完整的虚拟试衣功能，包括：
1. 从帖子详情页跳转到衣橱页面
2. 用户照片上传
3. 调用豆包API进行换装合成
4. 显示合成结果
5. 试装历史记录（数据库已支持，前端可扩展）

## 数据库设置

1. 执行数据库 schema 更新：
```bash
mysql -u root -p < backend/schema.sql
```

这将创建 `try_ons` 表用于存储试装记录。

## 后端配置

### 1. 安装依赖

```bash
cd backend
pip install -r requirements.txt
```

### 2. 配置豆包API

豆包API ID: `e5ede485-932d-4de2-8334-57d8955b61b8`

**重要：** 需要根据豆包API的实际文档调整 `backend/app.py` 中的 `call_doubao_tryon_api` 函数。

当前实现使用了类似OpenAI的API格式，如果豆包API使用不同的格式，需要修改：

1. **API URL**: 通过环境变量 `DOUBAO_API_URL` 设置，默认：`https://ark.cn-beijing.volces.com/api/v3/chat/completions`
2. **模型ID**: 通过环境变量 `DOUBAO_MODEL_ID` 设置，默认：`ep-20241220102330-xxxxx`
3. **请求格式**: 根据实际API文档调整 `payload` 结构
4. **响应解析**: 根据实际API响应格式调整解析逻辑

### 3. 启动后端

```bash
cd backend
python app.py
```

后端会运行在 `http://0.0.0.0:5000`

## API 接口

### 虚拟试衣

```
POST /api/try-on
Content-Type: application/json

{
  "user_id": 1,
  "user_image_path": "/static/posts/post_xxx.jpg",
  "clothing_image_path": "/static/posts/post_yyy.jpg",
  "post_id": 123  // 可选，关联的帖子ID
}
```

响应：
```json
{
  "ok": true,
  "msg": "换装成功",
  "data": {
    "id": 1,
    "resultImagePath": "/static/try-on-results/tryon_1_xxx.jpg",
    "status": "success"
  }
}
```

### 获取试装历史

```
GET /api/try-on/history?user_id=1&page=1&page_size=20
```

响应：
```json
{
  "ok": true,
  "data": [
    {
      "id": 1,
      "userImagePath": "/static/posts/post_xxx.jpg",
      "clothingImagePath": "/static/posts/post_yyy.jpg",
      "resultImagePath": "/static/try-on-results/tryon_1_xxx.jpg",
      "postId": 123,
      "postImagePath": "/static/posts/post_yyy.jpg",
      "status": "success",
      "errorMessage": null,
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1
}
```

## Android 应用配置

1. 更新 `app/src/main/java/com/example/dresscode1/network/ApiClient.java` 中的 `BASE_URL`，改为你的服务器 IP 地址

2. 同步 Gradle 项目以获取新的依赖

## 功能流程

1. **从帖子详情页跳转**
   - 用户在帖子详情页点击"我来试试"按钮
   - 自动跳转到衣橱页面，显示帖子中的衣服图片

2. **选择用户照片**
   - 用户点击"选择照片"按钮
   - 从相册选择照片
   - 照片显示在用户照片区域

3. **确认合成**
   - 用户点击"确认合成"按钮
   - 系统先上传用户照片到服务器
   - 调用换装API进行合成
   - 显示合成结果

4. **查看结果**
   - 合成成功后，结果图片显示在结果区域
   - 用户可以保存结果（功能可扩展）

## 注意事项

### 豆包API配置

由于豆包API的具体格式可能不同，需要：

1. **查看豆包API文档**，确认：
   - API端点URL
   - 请求格式（JSON结构）
   - 认证方式（Bearer Token或其他）
   - 响应格式

2. **修改 `backend/app.py` 中的 `call_doubao_tryon_api` 函数**：
   - 调整 `api_url` 和 `model_id`
   - 修改 `payload` 结构以匹配API要求
   - 调整响应解析逻辑

3. **测试API调用**：
   - 可以先使用Postman或curl测试API
   - 确认返回格式后再修改代码

### 图片处理

- 用户照片和衣服图片都会先上传到服务器
- 合成结果保存在 `try_on_results` 目录
- 所有图片通过静态文件服务提供访问

### 错误处理

- API调用失败时会记录错误信息到数据库
- 前端会显示友好的错误提示
- 可以通过试装历史查看失败记录

## 扩展功能

### 试装历史记录

数据库已支持试装历史记录，可以：

1. 在衣橱页面添加历史记录列表
2. 显示用户的所有试装记录
3. 支持重新查看和保存历史结果

### 保存到相册

可以在 `HomeActivity.saveResult()` 方法中实现保存功能：

```java
private void saveResult() {
    // 实现保存合成结果到相册的功能
    // 使用 MediaStore API 保存图片
}
```

## 故障排查

1. **API调用失败**
   - 检查豆包API配置是否正确
   - 查看后端日志确认错误信息
   - 验证API ID和模型ID是否正确

2. **图片上传失败**
   - 检查服务器存储目录权限
   - 确认图片格式是否支持
   - 查看后端日志

3. **合成结果为空**
   - 检查豆包API响应格式
   - 确认图片base64编码是否正确
   - 查看数据库中的错误信息

