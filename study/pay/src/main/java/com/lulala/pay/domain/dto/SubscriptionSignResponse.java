package com.lulala.pay.domain.dto;

import lombok.Data;

/**
 * 签约响应
 *
 * @author lulala
 */
@Data
public class SubscriptionSignResponse {

    /**
     * 订阅ID
     */
    private Long subscriptionId;

    /**
     * 订阅编号
     */
    private String subscriptionNo;

    /**
     * 签约页面URL（用于跳转到支付平台完成签约）
     */
    private String signUrl;

    /**
     * 签约参数（用于前端调起签约）
     */
    private String signData;
}
