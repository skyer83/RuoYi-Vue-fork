package com.lulala.pay.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订阅实体
 *
 * @author lulala
 */
@Data
public class PaySubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订阅ID
     */
    private Long subscriptionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订阅编号
     */
    private String subscriptionNo;

    /**
     * 支付方式：alipay-支付宝，wechat-微信
     */
    private String payMethod;

    /**
     * 协议号（支付平台返回的签约协议ID）
     */
    private String agreementNo;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品编码
     */
    private String productCode;

    /**
     * 订阅金额
     */
    private BigDecimal amount;

    /**
     * 币种
     */
    private String currency = "CNY";

    /**
     * 扣款周期：daily-每天，weekly-每周，monthly-每月，yearly-每年
     */
    private String period;

    /**
     * 扣款周期数（与period配合使用，如每2周扣款）
     */
    private Integer periodCount = 1;

    /**
     * 订阅状态：0-待签约，1-已签约，2-已解约，3-已过期
     */
    private Integer status;

    /**
     * 签约时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signedTime;

    /**
     * 解约时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cancelledTime;

    /**
     * 下次扣款时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextDeductTime;

    /**
     * 最后扣款时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastDeductTime;

    /**
     * 累计扣款次数
     */
    private Integer deductCount = 0;

    /**
     * 最大扣款次数（0表示不限次数）
     */
    private Integer maxDeductCount = 0;

    /**
     * 订阅开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 订阅结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

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

    /**
     * 备注
     */
    private String remark;
}
