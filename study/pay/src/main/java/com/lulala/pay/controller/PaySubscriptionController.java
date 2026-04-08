package com.lulala.pay.controller;

import com.lulala.pay.domain.PayRecord;
import com.lulala.pay.domain.PaySubscription;
import com.lulala.pay.domain.dto.DeductRequest;
import com.lulala.pay.domain.dto.SubscriptionCreateRequest;
import com.lulala.pay.domain.dto.SubscriptionSignResponse;
import com.lulala.pay.service.IPayRecordService;
import com.lulala.pay.service.IPaySubscriptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订阅管理控制器
 * 提供支付宝、微信自动续费接口
 *
 * @author lulala
 */
@Api(tags = "订阅管理")
@RestController
@RequestMapping("/subscription")
public class PaySubscriptionController {

    @Resource
    private IPaySubscriptionService subscriptionService;

    @Resource
    private IPayRecordService recordService;

    /**
     * 创建订阅
     */
    @ApiOperation("创建订阅")
    @PostMapping("/create")
    public Map<String, Object> create(@Valid @RequestBody SubscriptionCreateRequest request) {
        Map<String, Object> result = new HashMap<>();
        PaySubscription subscription = subscriptionService.createSubscription(request);
        result.put("code", 200);
        result.put("msg", "创建成功");
        result.put("data", subscription);
        return result;
    }

    /**
     * 发起签约
     * 返回签约URL，前端跳转到支付平台完成签约
     */
    @ApiOperation("发起签约")
    @PostMapping("/sign/{subscriptionId}")
    public Map<String, Object> sign(
            @ApiParam("订阅ID") @PathVariable Long subscriptionId) {
        Map<String, Object> result = new HashMap<>();
        SubscriptionSignResponse response = subscriptionService.sign(subscriptionId);
        result.put("code", 200);
        result.put("msg", "获取签约链接成功");
        result.put("data", response);
        return result;
    }

    /**
     * 解约
     */
    @ApiOperation("解约")
    @PostMapping("/cancel/{subscriptionId}")
    public Map<String, Object> cancel(
            @ApiParam("订阅ID") @PathVariable Long subscriptionId) {
        Map<String, Object> result = new HashMap<>();
        subscriptionService.cancel(subscriptionId);
        result.put("code", 200);
        result.put("msg", "解约成功");
        return result;
    }

    /**
     * 手动触发扣款
     */
    @ApiOperation("手动扣款")
    @PostMapping("/deduct")
    public Map<String, Object> deduct(@Valid @RequestBody DeductRequest request) {
        Map<String, Object> result = new HashMap<>();
        boolean success = subscriptionService.deduct(request);
        if (success) {
            result.put("code", 200);
            result.put("msg", "扣款成功");
        } else {
            result.put("code", 500);
            result.put("msg", "扣款失败");
        }
        return result;
    }

    /**
     * 查询订阅详情
     */
    @ApiOperation("查询订阅详情")
    @GetMapping("/{subscriptionId}")
    public Map<String, Object> getSubscription(
            @ApiParam("订阅ID") @PathVariable Long subscriptionId) {
        Map<String, Object> result = new HashMap<>();
        PaySubscription subscription = subscriptionService.getSubscription(subscriptionId);
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", subscription);
        return result;
    }

    /**
     * 查询用户订阅列表
     */
    @ApiOperation("查询用户订阅列表")
    @GetMapping("/user/{userId}")
    public Map<String, Object> listByUserId(
            @ApiParam("用户ID") @PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<PaySubscription> subscriptions = subscriptionService.listByUserId(userId);
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", subscriptions);
        return result;
    }

    /**
     * 查询订阅的支付记录
     */
    @ApiOperation("查询订阅支付记录")
    @GetMapping("/{subscriptionId}/records")
    public Map<String, Object> listRecords(
            @ApiParam("订阅ID") @PathVariable Long subscriptionId) {
        Map<String, Object> result = new HashMap<>();
        List<PayRecord> records = recordService.listBySubscriptionId(subscriptionId);
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", records);
        return result;
    }

    /**
     * 查询用户支付记录
     */
    @ApiOperation("查询用户支付记录")
    @GetMapping("/user/{userId}/records")
    public Map<String, Object> listUserRecords(
            @ApiParam("用户ID") @PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<PayRecord> records = recordService.listByUserId(userId);
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", records);
        return result;
    }
}
