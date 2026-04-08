package com.lulala.pay.service;

import com.lulala.pay.domain.PaySubscription;
import com.lulala.pay.domain.dto.SubscriptionSignResponse;

import java.math.BigDecimal;

/**
 * 微信自动续费服务接口
 *
 * @author lulala
 */
public interface IWechatAutoDeductService {

    /**
     * 生成签约预支付参数
     *
     * @param subscription 订阅信息
     * @return 签约响应
     */
    SubscriptionSignResponse generateSignUrl(PaySubscription subscription);

    /**
     * 验证签约回调签名
     *
     * @param notifyData 回调数据
     * @param signature  签名
     * @return 验证结果
     */
    boolean verifySignCallback(String notifyData, String signature);

    /**
     * 解析签约回调，获取协议号
     *
     * @param notifyData 回调数据
     * @return 协议号
     */
    String parseAgreementNo(String notifyData);

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
