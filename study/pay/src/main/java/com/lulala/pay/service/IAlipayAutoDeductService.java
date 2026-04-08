package com.lulala.pay.service;

import com.lulala.pay.domain.PaySubscription;
import com.lulala.pay.domain.dto.SubscriptionSignResponse;

import java.math.BigDecimal;

/**
 * 支付宝自动续费服务接口
 *
 * @author lulala
 */
public interface IAlipayAutoDeductService {

    /**
     * 生成签约页面URL
     *
     * @param subscription 订阅信息
     * @return 签约响应
     */
    SubscriptionSignResponse generateSignUrl(PaySubscription subscription);

    /**
     * 验证签约回调签名
     *
     * @param params 回调参数
     * @return 验证结果
     */
    boolean verifySignCallback(java.util.Map<String, String> params);

    /**
     * 解析签约回调，获取协议号
     *
     * @param params 回调参数
     * @return 协议号
     */
    String parseAgreementNo(java.util.Map<String, String> params);

    /**
     * 解约
     *
     * @param agreementNo 协议号
     * @return 是否成功
     */
    boolean cancel(String agreementNo);

    /**
     * 发起扣款
     *
     * @param subscription 订阅信息
     * @param outTradeNo   商户订单号
     * @param amount       扣款金额
     * @param subject      订单标题
     * @return 是否成功
     */
    boolean deduct(PaySubscription subscription, String outTradeNo, BigDecimal amount, String subject);

    /**
     * 查询扣款结果
     *
     * @param outTradeNo 商户订单号
     * @return 扣款状态
     */
    String queryDeductResult(String outTradeNo);
}
