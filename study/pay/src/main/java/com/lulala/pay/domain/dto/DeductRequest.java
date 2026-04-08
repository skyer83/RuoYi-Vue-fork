package com.lulala.pay.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 扣款请求
 *
 * @author lulala
 */
@Data
public class DeductRequest {

    /**
     * 订阅ID
     */
    @NotNull(message = "订阅ID不能为空")
    private Long subscriptionId;

    /**
     * 商户订单号（可选，不传则自动生成）
     */
    private String outTradeNo;

    /**
     * 扣款金额（可选，不传则使用订阅金额）
     */
    private java.math.BigDecimal amount;

    /**
     * 订单标题（可选）
     */
    private String subject;
}
