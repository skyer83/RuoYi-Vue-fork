package com.lulala.pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付配置
 *
 * @author lulala
 */
@Data
@Component
@ConfigurationProperties(prefix = "pay.alipay")
public class AlipayConfig {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 应用私钥
     */
    private String privateKey;

    /**
     * 支付宝公钥
     */
    private String alipayPublicKey;

    /**
     * 服务器异步通知页面路径
     */
    private String notifyUrl;

    /**
     * 页面跳转同步通知页面路径
     */
    private String returnUrl;

    /**
     * 签名方式
     */
    private String signType = "RSA2";

    /**
     * 字符编码格式
     */
    private String charset = "UTF-8";

    /**
     * 支付宝网关
     */
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";

    /**
     * 支付宝网关（沙箱环境）
     */
    private String gatewayUrlSandbox = "https://openapi.alipaydev.com/gateway.do";

    /**
     * 是否沙箱环境
     */
    private boolean sandbox = false;

    /**
     * 获取实际使用的网关地址
     */
    public String getActualGatewayUrl() {
        return sandbox ? gatewayUrlSandbox : gatewayUrl;
    }
}
