# 和风天气API配置说明

## 概述

本项目已配置为使用和风天气的专属API Host和JWT认证方式。

## 配置信息

- **专属Host**: `pn3qqqyqfa.re.qweatherapi.com`
- **认证方式**: JWT (使用Ed25519私钥签名)
- **私钥位置**: `app/src/main/assets/private_key.pem`

## 需要配置的参数

### 1. 公钥ID (KEY_ID)

在 `app/src/main/java/com/example/dresscode1/network/WeatherApiClient.java` 文件中，需要设置您的公钥ID：

```java
private static final String KEY_ID = "YOUR_KEY_ID"; // 请替换为您的公钥ID
```

**如何获取公钥ID：**
1. 登录和风天气控制台：https://console.qweather.com
2. 进入「设置」->「API密钥」
3. 找到您上传的Ed25519公钥
4. 复制公钥ID（通常是公钥的标识符或哈希值）

## JWT Token生成规则

根据和风天气的要求，JWT token包含以下信息：

**Header:**
```json
{
  "alg": "EdDSA",
  "typ": "JWT"
}
```

**Payload:**
```json
{
  "api": "weather",
  "version": "v7",
  "location": "101010100",  // 城市ID或经纬度
  "timestamp": 1703071618,  // Unix时间戳（秒）
  "iss": "YOUR_KEY_ID"      // 公钥ID
}
```

**Signature:**
使用Ed25519私钥对Header和Payload进行签名。

## 使用方式

1. **初始化**（在Application或Activity的onCreate中）：
   ```java
   WeatherApiClient.init(context);
   ```

2. **调用API**：
   ```java
   WeatherApiService service = WeatherApiClient.getService();
   service.getNowWeather("101010100")  // 城市ID或经纬度
       .enqueue(new Callback<WeatherResponse>() {
           // 处理响应
       });
   ```

## 注意事项

1. **私钥安全**: 私钥文件已放置在 `app/src/main/assets/` 目录中，请确保不要将私钥提交到公开的代码仓库。

2. **公钥上传**: 确保您已将Ed25519公钥上传到和风天气平台。

3. **公钥格式**: 公钥格式应为：
   ```
   -----BEGIN PUBLIC KEY-----
   MCowBQYDK2VwAyEA...
   -----END PUBLIC KEY-----
   ```

4. **Token有效期**: JWT token每次请求时动态生成，包含当前时间戳，确保请求的时效性。

## 故障排查

### 问题1: JWT token生成失败
- 检查私钥文件是否存在：`app/src/main/assets/private_key.pem`
- 检查私钥格式是否正确（Ed25519格式）
- 查看Logcat中的错误日志

### 问题2: API返回403错误
- 确认已使用专属Host：`pn3qqqyqfa.re.qweatherapi.com`
- 确认公钥ID配置正确
- 确认公钥已上传到和风天气平台

### 问题3: API返回401错误
- 检查JWT token是否正确生成
- 检查私钥是否与上传的公钥匹配
- 检查JWT payload中的参数是否正确

## 相关文件

- `app/src/main/java/com/example/dresscode1/network/WeatherApiClient.java` - API客户端配置
- `app/src/main/java/com/example/dresscode1/utils/JwtUtils.java` - JWT工具类
- `app/src/main/assets/private_key.pem` - Ed25519私钥文件


