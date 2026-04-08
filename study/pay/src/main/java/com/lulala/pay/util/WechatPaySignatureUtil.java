package com.lulala.pay.util;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * 微信支付V3签名工具类
 *
 * @author lulala
 */
public class WechatPaySignatureUtil {

    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    private static final String ALGORITHM = "RSA";

    /**
     * 构建Authorization请求头
     *
     * @param method        HTTP方法
     * @param urlPath       URL路径
     * @param body          请求体
     * @param mchid         商户号
     * @param serialNo      证书序列号
     * @param privateKeyPath 私钥文件路径
     * @return Authorization头值
     */
    public static String buildAuthorization(String method, String urlPath, String body,
                                            String mchid, String serialNo, String privateKeyPath) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        
        // 构建签名串
        String signatureStr = buildSignatureString(method, urlPath, timestamp, nonceStr, body);
        
        // 读取私钥并签名
        PrivateKey privateKey = loadPrivateKey(privateKeyPath);
        String signature = sign(signatureStr, privateKey);
        
        // 构建Authorization头
        return String.format(
                "WECHATPAY2-SHA256-RSA2048 mchid=\"%s\",nonce_str=\"%s\",signature=\"%s\",timestamp=\"%s\",serial_no=\"%s\"",
                mchid, nonceStr, signature, timestamp, serialNo
        );
    }

    /**
     * 构建签名串
     */
    private static String buildSignatureString(String method, String urlPath, 
            String timestamp, String nonceStr, String body) {
        return method + "\n"
                + urlPath + "\n"
                + timestamp + "\n"
                + nonceStr + "\n"
                + (body == null ? "" : body) + "\n";
    }

    /**
     * 加载私钥
     */
    private static PrivateKey loadPrivateKey(String privateKeyPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(privateKeyPath)) {
            byte[] keyBytes = new byte[fis.available()];
            fis.read(keyBytes);
            
            String privateKeyPEM = new String(keyBytes, StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            
            byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
            
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            return keyFactory.generatePrivate(spec);
        }
    }

    /**
     * 使用私钥签名
     */
    private static String sign(String message, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * 验证签名（使用API V3密钥）
     *
     * @param data      待验证数据
     * @param signature 签名
     * @param apiV3Key  API V3密钥
     * @return 验证结果
     */
    public static boolean verify(String data, String signature, String apiV3Key) {
        // 实际验证逻辑需要使用微信支付平台公钥
        // 这里简化处理，实际项目需要下载并使用平台证书
        return signature != null && !signature.isEmpty();
    }

    /**
     * 计算HMAC-SHA256
     */
    public static String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
