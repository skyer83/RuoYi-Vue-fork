package com.lulala.pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置
 *
 * @author lulala
 */
@Data
@Component
@ConfigurationProperties(prefix = "pay.wechat")
public class WechatPayConfig {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 商户号
     */
    private String mchId;

    /**
     * 商户API私钥路径
     */
    private String privateKeyPath;

    /**
     * 商户证书序列号
     */
    private String merchantSerialNumber;

    /**
     * API V3密钥
     */
    private String apiV3Key;

    /**
     * 异步通知地址
     */
    private String notifyUrl;

    /**
     * 回调通知地址（签约）
     */
    private String contractNotifyUrl;

    /**
     * 是否沙箱环境
     */
    private boolean sandbox = false;
}
