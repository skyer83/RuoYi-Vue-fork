package com.lulala.pay.service.impl;

import com.lulala.pay.config.AlipayConfig;
import com.lulala.pay.config.WechatPayConfig;
import com.lulala.pay.domain.PayRecord;
import com.lulala.pay.domain.PaySubscription;
import com.lulala.pay.domain.dto.DeductRequest;
import com.lulala.pay.domain.dto.SubscriptionCreateRequest;
import com.lulala.pay.domain.dto.SubscriptionSignResponse;
import com.lulala.pay.domain.enums.DeductPeriod;
import com.lulala.pay.domain.enums.PayMethod;
import com.lulala.pay.domain.enums.PayStatus;
import com.lulala.pay.domain.enums.SubscriptionStatus;
import com.lulala.pay.service.IAlipayAutoDeductService;
import com.lulala.pay.service.IPayRecordService;
import com.lulala.pay.service.IPaySubscriptionService;
import com.lulala.pay.service.IWechatAutoDeductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订阅服务实现
 * 注意：此为示例实现，实际应使用数据库存储
 *
 * @author lulala
 */
@Slf4j
@Service
public class PaySubscriptionServiceImpl implements IPaySubscriptionService {

    @Resource
    private IAlipayAutoDeductService alipayAutoDeductService;

    @Resource
    private IWechatAutoDeductService wechatAutoDeductService;

    @Resource
    private IPayRecordService payRecordService;

    @Resource
    private AlipayConfig alipayConfig;

    @Resource
    private WechatPayConfig wechatPayConfig;

    // 模拟数据库存储
    private final Map<Long, PaySubscription> subscriptionDB = new ConcurrentHashMap<>();
    private Long idGenerator = 1L;

    @Override
    public PaySubscription createSubscription(SubscriptionCreateRequest request) {
        PaySubscription subscription = new PaySubscription();
        subscription.setSubscriptionId(idGenerator++);
        subscription.setSubscriptionNo(generateSubscriptionNo());
        subscription.setUserId(request.getUserId());
        subscription.setPayMethod(request.getPayMethod());
        subscription.setProductName(request.getProductName());
        subscription.setProductCode(request.getProductCode());
        subscription.setAmount(request.getAmount());
        subscription.setPeriod(request.getPeriod());
        subscription.setPeriodCount(request.getPeriodCount());
        subscription.setMaxDeductCount(request.getMaxDeductCount());
        subscription.setStatus(SubscriptionStatus.PENDING.getCode());
        subscription.setDeductCount(0);
        subscription.setCreateTime(new Date());
        subscription.setUpdateTime(new Date());
        subscription.setRemark(request.getRemark());
        
        // 设置订阅开始时间和下次扣款时间
        subscription.setStartTime(new Date());
        subscription.setNextDeductTime(calculateNextDeductTime(new Date(), request.getPeriod(), request.getPeriodCount()));
        
        subscriptionDB.put(subscription.getSubscriptionId(), subscription);
        return subscription;
    }

    @Override
    public SubscriptionSignResponse sign(Long subscriptionId) {
        PaySubscription subscription = getSubscription(subscriptionId);
        if (subscription == null) {
            throw new RuntimeException("订阅不存在");
        }
        
        if (subscription.getStatus() != SubscriptionStatus.PENDING.getCode()) {
            throw new RuntimeException("订阅状态不正确，无法签约");
        }

        SubscriptionSignResponse response;
        if (PayMethod.ALIPAY.getCode().equals(subscription.getPayMethod())) {
            response = alipayAutoDeductService.generateSignUrl(subscription);
        } else if (PayMethod.WECHAT.getCode().equals(subscription.getPayMethod())) {
            response = wechatAutoDeductService.generateSignUrl(subscription);
        } else {
            throw new RuntimeException("不支持的支付方式");
        }
        
        return response;
    }

    @Override
    public void handleSignSuccess(Long subscriptionId, String agreementNo) {
        PaySubscription subscription = getSubscription(subscriptionId);
        if (subscription == null) {
            throw new RuntimeException("订阅不存在");
        }
        
        subscription.setAgreementNo(agreementNo);
        subscription.setStatus(SubscriptionStatus.SIGNED.getCode());
        subscription.setSignedTime(new Date());
        subscription.setUpdateTime(new Date());
        
        subscriptionDB.put(subscription.getSubscriptionId(), subscription);
        log.info("订阅签约成功, subscriptionId: {}, agreementNo: {}", subscriptionId, agreementNo);
    }

    @Override
    public void cancel(Long subscriptionId) {
        PaySubscription subscription = getSubscription(subscriptionId);
        if (subscription == null) {
            throw new RuntimeException("订阅不存在");
        }
        
        if (subscription.getStatus() != SubscriptionStatus.SIGNED.getCode()) {
            throw new RuntimeException("订阅状态不正确，无法解约");
        }

        boolean success;
        if (PayMethod.ALIPAY.getCode().equals(subscription.getPayMethod())) {
            success = alipayAutoDeductService.cancel(subscription.getAgreementNo());
        } else if (PayMethod.WECHAT.getCode().equals(subscription.getPayMethod())) {
            success = wechatAutoDeductService.cancel(subscription.getAgreementNo());
        } else {
            throw new RuntimeException("不支持的支付方式");
        }
        
        if (success) {
            subscription.setStatus(SubscriptionStatus.CANCELLED.getCode());
            subscription.setCancelledTime(new Date());
            subscription.setUpdateTime(new Date());
            subscriptionDB.put(subscription.getSubscriptionId(), subscription);
            log.info("订阅解约成功, subscriptionId: {}", subscriptionId);
        } else {
            throw new RuntimeException("解约失败");
        }
    }

    @Override
    public boolean deduct(DeductRequest request) {
        PaySubscription subscription = getSubscription(request.getSubscriptionId());
        if (subscription == null) {
            throw new RuntimeException("订阅不存在");
        }
        
        if (subscription.getStatus() != SubscriptionStatus.SIGNED.getCode()) {
            throw new RuntimeException("订阅状态不正确，无法扣款");
        }

        String outTradeNo = request.getOutTradeNo() != null ? request.getOutTradeNo() : generateOutTradeNo();
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : subscription.getAmount();
        String subject = request.getSubject() != null ? request.getSubject() : subscription.getProductName();

        // 创建支付记录
        PayRecord record = new PayRecord();
        record.setSubscriptionId(subscription.getSubscriptionId());
        record.setUserId(subscription.getUserId());
        record.setOutTradeNo(outTradeNo);
        record.setPayMethod(subscription.getPayMethod());
        record.setAmount(amount);
        record.setSubject(subject);
        record.setStatus(PayStatus.PENDING.getCode());
        record.setAutoDeduct(1);
        record.setCreateTime(new Date());
        payRecordService.createRecord(record);

        boolean success;
        if (PayMethod.ALIPAY.getCode().equals(subscription.getPayMethod())) {
            success = alipayAutoDeductService.deduct(subscription, outTradeNo, amount, subject);
        } else if (PayMethod.WECHAT.getCode().equals(subscription.getPayMethod())) {
            success = wechatAutoDeductService.deduct(subscription, outTradeNo, amount, subject);
        } else {
            throw new RuntimeException("不支持的支付方式");
        }

        if (success) {
            record.setStatus(PayStatus.SUCCESS.getCode());
            record.setPayTime(new Date());
            payRecordService.updateRecord(record);

            // 更新订阅信息
            subscription.setDeductCount(subscription.getDeductCount() + 1);
            subscription.setLastDeductTime(new Date());
            subscription.setNextDeductTime(calculateNextDeductTime(new Date(), subscription.getPeriod(), subscription.getPeriodCount()));
            subscription.setUpdateTime(new Date());
            subscriptionDB.put(subscription.getSubscriptionId(), subscription);
            
            log.info("扣款成功, subscriptionId: {}, outTradeNo: {}", subscription.getSubscriptionId(), outTradeNo);
        } else {
            record.setStatus(PayStatus.FAILED.getCode());
            record.setFailReason("扣款失败");
            payRecordService.updateRecord(record);
            log.error("扣款失败, subscriptionId: {}, outTradeNo: {}", subscription.getSubscriptionId(), outTradeNo);
        }

        return success;
    }

    @Override
    public void scheduledDeduct() {
        log.info("开始执行定时扣款任务...");
        List<PaySubscription> subscriptions = listActiveSubscriptions();
        Date now = new Date();
        
        for (PaySubscription subscription : subscriptions) {
            try {
                // 检查是否需要扣款
                if (subscription.getNextDeductTime() != null 
                        && !subscription.getNextDeductTime().after(now)) {
                    // 检查是否达到最大扣款次数
                    if (subscription.getMaxDeductCount() > 0 
                            && subscription.getDeductCount() >= subscription.getMaxDeductCount()) {
                        subscription.setStatus(SubscriptionStatus.EXPIRED.getCode());
                        subscription.setUpdateTime(new Date());
                        subscriptionDB.put(subscription.getSubscriptionId(), subscription);
                        continue;
                    }
                    
                    DeductRequest request = new DeductRequest();
                    request.setSubscriptionId(subscription.getSubscriptionId());
                    deduct(request);
                }
            } catch (Exception e) {
                log.error("定时扣款异常, subscriptionId: {}", subscription.getSubscriptionId(), e);
            }
        }
        log.info("定时扣款任务执行完成");
    }

    @Override
    public PaySubscription getSubscription(Long subscriptionId) {
        return subscriptionDB.get(subscriptionId);
    }

    @Override
    public List<PaySubscription> listByUserId(Long userId) {
        List<PaySubscription> result = new ArrayList<>();
        for (PaySubscription subscription : subscriptionDB.values()) {
            if (userId.equals(subscription.getUserId())) {
                result.add(subscription);
            }
        }
        result.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return result;
    }

    @Override
    public List<PaySubscription> listActiveSubscriptions() {
        List<PaySubscription> result = new ArrayList<>();
        for (PaySubscription subscription : subscriptionDB.values()) {
            if (subscription.getStatus() == SubscriptionStatus.SIGNED.getCode()) {
                result.add(subscription);
            }
        }
        return result;
    }

    /**
     * 生成订阅编号
     */
    private String generateSubscriptionNo() {
        return "SUB" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }

    /**
     * 生成商户订单号
     */
    private String generateOutTradeNo() {
        return "PAY" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }

    /**
     * 计算下次扣款时间
     */
    private Date calculateNextDeductTime(Date baseTime, String period, Integer periodCount) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(baseTime.toInstant(), ZoneId.systemDefault());
        ChronoUnit unit;
        
        switch (period) {
            case "daily":
                unit = ChronoUnit.DAYS;
                break;
            case "weekly":
                unit = ChronoUnit.WEEKS;
                break;
            case "monthly":
                unit = ChronoUnit.MONTHS;
                break;
            case "yearly":
                unit = ChronoUnit.YEARS;
                break;
            default:
                unit = ChronoUnit.MONTHS;
        }
        
        LocalDateTime nextTime = dateTime.plus(periodCount, unit);
        return Date.from(nextTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
