package com.lulala.pay.domain.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 创建订阅请求
 *
 * @author lulala
 */
@Data
public class SubscriptionCreateRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 支付方式：alipay-支付宝，wechat-微信
     */
    @NotBlank(message = "支付方式不能为空")
    private String payMethod;

    /**
     * 产品名称
     */
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称不能超过100个字符")
    private String productName;

    /**
     * 产品编码
     */
    @Size(max = 50, message = "产品编码不能超过50个字符")
    private String productCode;

    /**
     * 订阅金额
     */
    @NotNull(message = "订阅金额不能为空")
    @DecimalMin(value = "0.01", message = "订阅金额必须大于0")
    private BigDecimal amount;

    /**
     * 扣款周期：daily-每天，weekly-每周，monthly-每月，yearly-每年
     */
    @NotBlank(message = "扣款周期不能为空")
    private String period;

    /**
     * 扣款周期数（与period配合使用，如每2周扣款）
     */
    @Min(value = 1, message = "扣款周期数必须大于0")
    private Integer periodCount = 1;

    /**
     * 最大扣款次数（0表示不限次数）
     */
    @Min(value = 0, message = "最大扣款次数不能为负数")
    private Integer maxDeductCount = 0;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
