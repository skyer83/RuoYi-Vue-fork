package com.lulala.pay.service.impl;

import com.lulala.pay.domain.PayRecord;
import com.lulala.pay.service.IPayRecordService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付记录服务实现
 * 注意：此为示例实现，实际应使用数据库存储
 *
 * @author lulala
 */
@Service
public class PayRecordServiceImpl implements IPayRecordService {

    // 模拟数据库存储
    private final Map<Long, PayRecord> recordDB = new ConcurrentHashMap<>();
    private final Map<String, PayRecord> outTradeNoIndex = new ConcurrentHashMap<>();
    private Long idGenerator = 1L;

    @Override
    public PayRecord createRecord(PayRecord record) {
        record.setRecordId(idGenerator++);
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        recordDB.put(record.getRecordId(), record);
        outTradeNoIndex.put(record.getOutTradeNo(), record);
        return record;
    }

    @Override
    public PayRecord updateRecord(PayRecord record) {
        record.setUpdateTime(new Date());
        recordDB.put(record.getRecordId(), record);
        outTradeNoIndex.put(record.getOutTradeNo(), record);
        return record;
    }

    @Override
    public PayRecord getRecord(Long recordId) {
        return recordDB.get(recordId);
    }

    @Override
    public PayRecord getByOutTradeNo(String outTradeNo) {
        return outTradeNoIndex.get(outTradeNo);
    }

    @Override
    public List<PayRecord> listBySubscriptionId(Long subscriptionId) {
        List<PayRecord> result = new ArrayList<>();
        for (PayRecord record : recordDB.values()) {
            if (subscriptionId.equals(record.getSubscriptionId())) {
                result.add(record);
            }
        }
        result.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return result;
    }

    @Override
    public List<PayRecord> listByUserId(Long userId) {
        List<PayRecord> result = new ArrayList<>();
        for (PayRecord record : recordDB.values()) {
            if (userId.equals(record.getUserId())) {
                result.add(record);
            }
        }
        result.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return result;
    }
}
