package com.lulala.pay.service;

import com.lulala.pay.domain.PayRecord;

import java.util.List;

/**
 * 支付记录服务接口
 *
 * @author lulala
 */
public interface IPayRecordService {

    /**
     * 创建支付记录
     *
     * @param record 支付记录
     * @return 创建后的记录
     */
    PayRecord createRecord(PayRecord record);

    /**
     * 更新支付记录
     *
     * @param record 支付记录
     * @return 更新后的记录
     */
    PayRecord updateRecord(PayRecord record);

    /**
     * 根据ID查询支付记录
     *
     * @param recordId 记录ID
     * @return 支付记录
     */
    PayRecord getRecord(Long recordId);

    /**
     * 根据商户订单号查询
     *
     * @param outTradeNo 商户订单号
     * @return 支付记录
     */
    PayRecord getByOutTradeNo(String outTradeNo);

    /**
     * 根据订阅ID查询支付记录列表
     *
     * @param subscriptionId 订阅ID
     * @return 支付记录列表
     */
    List<PayRecord> listBySubscriptionId(Long subscriptionId);

    /**
     * 根据用户ID查询支付记录列表
     *
     * @param userId 用户ID
     * @return 支付记录列表
     */
    List<PayRecord> listByUserId(Long userId);
}
