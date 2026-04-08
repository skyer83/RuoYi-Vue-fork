package com.lulala.pay.service;

import com.lulala.pay.domain.PaySubscription;
import com.lulala.pay.domain.dto.DeductRequest;
import com.lulala.pay.domain.dto.SubscriptionCreateRequest;
import com.lulala.pay.domain.dto.SubscriptionSignResponse;

import java.util.List;

/**
 * 订阅服务接口
 *
 * @author lulala
 */
public interface IPaySubscriptionService {

    /**
     * 创建订阅
     *
     * @param request 订阅创建请求
     * @return 订阅实体
     */
    PaySubscription createSubscription(SubscriptionCreateRequest request);

    /**
     * 发起签约
     *
     * @param subscriptionId 订阅ID
     * @return 签约响应
     */
    SubscriptionSignResponse sign(Long subscriptionId);

    /**
     * 处理签约成功回调
     *
     * @param subscriptionId 订阅ID
     * @param agreementNo    协议号
     */
    void handleSignSuccess(Long subscriptionId, String agreementNo);

    /**
     * 解约
     *
     * @param subscriptionId 订阅ID
     */
    void cancel(Long subscriptionId);

    /**
     * 发起扣款
     *
     * @param request 扣款请求
     * @return 扣款结果
     */
    boolean deduct(DeductRequest request);

    /**
     * 定时自动扣款（扫描所有待扣款的订阅）
     */
    void scheduledDeduct();

    /**
     * 查询订阅
     *
     * @param subscriptionId 订阅ID
     * @return 订阅信息
     */
    PaySubscription getSubscription(Long subscriptionId);

    /**
     * 根据用户ID查询订阅列表
     *
     * @param userId 用户ID
     * @return 订阅列表
     */
    List<PaySubscription> listByUserId(Long userId);

    /**
     * 查询所有有效订阅
     *
     * @return 订阅列表
     */
    List<PaySubscription> listActiveSubscriptions();
}
