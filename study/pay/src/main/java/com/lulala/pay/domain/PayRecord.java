package com.lulala.pay.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付记录实体
 *
 * @author lulala
 */
@Data
public class PayRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private Long recordId;

    /**
     * 订阅ID
     */
    private Long subscriptionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 支付平台交易号
     */
    private String tradeNo;

    /**
     * 支付方式：alipay-支付宝，wechat-微信
     */
    private String payMethod;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 实际支付金额
     */
    private BigDecimal payAmount;

    /**
     * 币种
     */
    private String currency = "CNY";

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 订单描述
     */
    private String body;

    /**
     * 支付状态：0-待支付，1-支付成功，2-支付失败，3-已退款
     */
    private Integer status;

    /**
     * 支付时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    /**
     * 关闭时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closeTime;

    /**
     * 是否自动扣款：0-否，1-是
     */
    private Integer autoDeduct;

    /**
     * 扣款失败原因
     */
    private String failReason;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
