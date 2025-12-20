package com.example.dresscode1.utils;

import android.content.Context;
import android.util.Log;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Jwts;

public class JwtUtils {
    private static final String TAG = "JwtUtils";
    
    static {
        // 注册BouncyCastle提供者以支持Ed25519
        if (Security.getProvider("BC") == null) {
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
        }
    }
    
    /**
     * 生成和风天气API的JWT token
     * @param context Android上下文
     * @param keyId 公钥ID（从和风天气平台获取，通常是公钥的标识）
     * @param location 城市ID或经纬度，例如：101010100 或 116.41,39.92
     * @return JWT token字符串
     */
    public static String generateToken(Context context, String keyId, String location) {
        try {
            // 读取私钥文件
            PrivateKey privateKey = loadPrivateKey(context);
            
            // 获取当前时间戳（秒）
            long timestamp = System.currentTimeMillis() / 1000;
            
            // 构建JWT payload（根据和风天气要求）
            Map<String, Object> claims = new HashMap<>();
            claims.put("api", "weather");
            claims.put("version", "v7");
            claims.put("location", location);
            claims.put("timestamp", timestamp);
            claims.put("iss", keyId); // 公钥ID
            
            // 生成JWT token
            // 和风天气要求：使用EdDSA算法签名
            // 对于EdDSA，直接使用signWith(PrivateKey)，算法会从密钥类型自动推断
            String token = Jwts.builder()
                    .setHeaderParam("alg", "EdDSA")
                    .setHeaderParam("typ", "JWT")
                    .setClaims(claims)
                    .signWith(privateKey)
                    .compact();
            
            Log.d(TAG, "JWT token generated successfully for location: " + location);
            return token;
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate JWT token", e);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 从assets目录加载Ed25519私钥
     * 使用BouncyCastle原生API直接解析Ed25519密钥
     */
    private static PrivateKey loadPrivateKey(Context context) throws Exception {
        // 确保BouncyCastle提供者已注册
        if (Security.getProvider("BC") == null) {
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
        }
        
        InputStream inputStream = context.getAssets().open("private_key.pem");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        
        try {
            // 方法1: 使用PEMParser解析
            PEMParser pemParser = new PEMParser(reader);
            Object object = pemParser.readObject();
            pemParser.close();
            
            if (object != null) {
                PrivateKeyInfo privateKeyInfo = null;
                
                if (object instanceof PrivateKeyInfo) {
                    privateKeyInfo = (PrivateKeyInfo) object;
                } else if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
                    org.bouncycastle.openssl.PEMKeyPair keyPair = (org.bouncycastle.openssl.PEMKeyPair) object;
                    privateKeyInfo = keyPair.getPrivateKeyInfo();
                }
                
                if (privateKeyInfo != null) {
                    return convertToPrivateKey(privateKeyInfo);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "PEMParser failed, trying manual parsing", e);
        } finally {
            reader.close();
            inputStream.close();
        }
        
        // 方法2: 手动解析PEM文件
        inputStream = context.getAssets().open("private_key.pem");
        reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        
        StringBuilder keyBuilder = new StringBuilder();
        String line;
        boolean inKeyBlock = false;
        
        while ((line = reader.readLine()) != null) {
            if (line.contains("BEGIN PRIVATE KEY") || line.contains("BEGIN EC PRIVATE KEY")) {
                inKeyBlock = true;
                continue;
            }
            if (line.contains("END PRIVATE KEY") || line.contains("END EC PRIVATE KEY")) {
                break;
            }
            if (inKeyBlock && !line.trim().isEmpty()) {
                keyBuilder.append(line);
            }
        }
        
        reader.close();
        inputStream.close();
        
        if (keyBuilder.length() == 0) {
            throw new Exception("Private key not found in PEM file");
        }
        
        // 解码Base64私钥
        String keyBase64 = keyBuilder.toString();
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        
        // 解析PKCS8格式的PrivateKeyInfo
        try {
            PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(keyBytes);
            return convertToPrivateKey(privateKeyInfo);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse Ed25519 private key from PKCS8", e);
            throw new Exception("Failed to load Ed25519 private key: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将PrivateKeyInfo转换为JCA PrivateKey
     * 使用BouncyCastle原生API直接处理Ed25519密钥
     */
    private static PrivateKey convertToPrivateKey(PrivateKeyInfo privateKeyInfo) throws Exception {
        try {
            // 检查算法标识符是否为Ed25519 (OID: 1.3.101.112)
            AlgorithmIdentifier algId = privateKeyInfo.getPrivateKeyAlgorithm();
            ASN1ObjectIdentifier algorithm = algId.getAlgorithm();
            
            // Ed25519的OID是1.3.101.112
            if (algorithm.getId().equals("1.3.101.112")) {
                // 提取私钥字节
                org.bouncycastle.asn1.ASN1OctetString keyData = 
                    (org.bouncycastle.asn1.ASN1OctetString) privateKeyInfo.parsePrivateKey();
                byte[] privateKeyBytes = keyData.getOctets();
                
                // Ed25519私钥是32字节
                if (privateKeyBytes.length < 32) {
                    throw new Exception("Invalid Ed25519 private key length: " + privateKeyBytes.length);
                }
                
                // 提取前32字节作为私钥
                byte[] key32 = new byte[32];
                System.arraycopy(privateKeyBytes, 0, key32, 0, 32);
                
                // 创建Ed25519私钥参数
                Ed25519PrivateKeyParameters ed25519KeyParams = 
                    new Ed25519PrivateKeyParameters(key32, 0);
                
                // 使用反射创建BCEdDSAPrivateKey，因为其构造函数不是公共的
                // 或者直接使用PrivateKeyInfo重新构造
                try {
                    // 尝试使用反射访问BCEdDSAPrivateKey的包私有构造函数
                    java.lang.reflect.Constructor<BCEdDSAPrivateKey> constructor = 
                        BCEdDSAPrivateKey.class.getDeclaredConstructor(
                            org.bouncycastle.crypto.params.AsymmetricKeyParameter.class);
                    constructor.setAccessible(true);
                    return constructor.newInstance(ed25519KeyParams);
                } catch (Exception e) {
                    // 如果反射失败，尝试使用PrivateKeyInfo重新编码
                    Log.w(TAG, "Failed to use reflection, trying alternative method", e);
                    
                    // 重新构造PrivateKeyInfo并尝试使用JCA转换器
                    // 但这次我们直接使用BCEdDSAPrivateKey的另一个构造函数
                    try {
                        // 使用PrivateKeyInfo的getEncoded()方法获取编码
                        byte[] encoded = privateKeyInfo.getEncoded();
                        PrivateKeyInfo newKeyInfo = PrivateKeyInfo.getInstance(encoded);
                        
                        // 尝试使用BCEdDSAPrivateKey的PrivateKeyInfo构造函数
                        java.lang.reflect.Constructor<BCEdDSAPrivateKey> constructor2 = 
                            BCEdDSAPrivateKey.class.getDeclaredConstructor(PrivateKeyInfo.class);
                        constructor2.setAccessible(true);
                        return constructor2.newInstance(newKeyInfo);
                    } catch (Exception e2) {
                        // 最后的备选方案：创建一个自定义的PrivateKey实现
                        Log.w(TAG, "All methods failed, creating custom PrivateKey wrapper", e2);
                        return new Ed25519PrivateKeyWrapper(ed25519KeyParams);
                    }
                }
            } else {
                throw new Exception("Unsupported algorithm: " + algorithm.getId() + 
                    ". Expected Ed25519 (1.3.101.112)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert PrivateKeyInfo to PrivateKey", e);
            throw e;
        }
    }
    
    /**
     * Ed25519私钥包装类，实现JCA PrivateKey接口
     * 用于在JCA提供者不支持Ed25519时提供兼容性
     */
    private static class Ed25519PrivateKeyWrapper implements PrivateKey {
        private static final long serialVersionUID = 1L;
        private final Ed25519PrivateKeyParameters keyParams;
        private final byte[] encoded;
        
        public Ed25519PrivateKeyWrapper(Ed25519PrivateKeyParameters keyParams) throws Exception {
            this.keyParams = keyParams;
            // 编码为PKCS8格式
            byte[] privateKeyBytes = keyParams.getEncoded();
            // 构造PrivateKeyInfo
            AlgorithmIdentifier algId = new AlgorithmIdentifier(
                new ASN1ObjectIdentifier("1.3.101.112"));
            org.bouncycastle.asn1.ASN1OctetString keyData = 
                org.bouncycastle.asn1.DEROctetString.getInstance(privateKeyBytes);
            PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(algId, keyData);
            this.encoded = privateKeyInfo.getEncoded();
        }
        
        @Override
        public String getAlgorithm() {
            return "Ed25519";
        }
        
        @Override
        public String getFormat() {
            return "PKCS#8";
        }
        
        @Override
        public byte[] getEncoded() {
            return encoded.clone();
        }
        
        public Ed25519PrivateKeyParameters getKeyParams() {
            return keyParams;
        }
    }
}

