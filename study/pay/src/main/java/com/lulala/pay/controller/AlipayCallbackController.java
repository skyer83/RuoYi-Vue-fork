package com.lulala.pay.controller;

import com.lulala.pay.domain.PayRecord;
import com.lulala.pay.domain.PaySubscription;
import com.lulala.pay.domain.enums.PayStatus;
import com.lulala.pay.service.IAlipayAutoDeductService;
import com.lulala.pay.service.IPayRecordService;
import com.lulala.pay.service.IPaySubscriptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝回调控制器
 *
 * @author lulala
 */
@Slf4j
@Api(tags = "支付宝回调")
@RestController
@RequestMapping("/alipay")
public class AlipayCallbackController {

    @Resource
    private IAlipayAutoDeductService alipayAutoDeductService;

    @Resource
    private IPaySubscriptionService subscriptionService;

    @Resource
    private IPayRecordService recordService;

    /**
     * 签约回调通知
     */
    @ApiOperation("签约回调通知")
    @PostMapping("/sign/notify")
    public String signNotify(HttpServletRequest request) {
        log.info("收到支付宝签约回调通知");
        
        Map<String, String> params = getRequestParams(request);
        
        // 验证签名
        if (!alipayAutoDeductService.verifySignCallback(params)) {
            log.error("支付宝签约回调验签失败");
            return "failure";
        }
        
        // 解析签约结果
        String agreementNo = alipayAutoDeductService.parseAgreementNo(params);
        String outTradeNo = params.get("out_trade_no");
        
        log.info("支付宝签约成功, agreementNo: {}, outTradeNo: {}", agreementNo, outTradeNo);
        
        // 更新订阅状态
        // 需要根据out_trade_no找到对应的订阅
        // 这里简化处理，实际需要根据业务逻辑关联
        try {
            // 假设out_trade_no就是订阅编号的前缀
            Long subscriptionId = extractSubscriptionId(outTradeNo);
            if (subscriptionId != null) {
                subscriptionService.handleSignSuccess(subscriptionId, agreementNo);
            }
        } catch (Exception e) {
            log.error("处理签约回调异常", e);
        }
        
        return "success";
    }

    /**
     * 支付回调通知
     */
    @ApiOperation("支付回调通知")
    @PostMapping("/pay/notify")
    public String payNotify(HttpServletRequest request) {
        log.info("收到支付宝支付回调通知");
        
        Map<String, String> params = getRequestParams(request);
        
        // 验证签名
        if (!alipayAutoDeductService.verifySignCallback(params)) {
            log.error("支付宝支付回调验签失败");
            return "failure";
        }
        
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        String totalAmount = params.get("total_amount");
        
        log.info("支付宝支付回调, outTradeNo: {}, tradeNo: {}, status: {}", 
                outTradeNo, tradeNo, tradeStatus);
        
        // 更新支付记录状态
        try {
            PayRecord record = recordService.getByOutTradeNo(outTradeNo);
            if (record != null) {
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    record.setStatus(PayStatus.SUCCESS.getCode());
                    record.setTradeNo(tradeNo);
                    record.setPayTime(new java.util.Date());
                    record.setPayAmount(new java.math.BigDecimal(totalAmount));
                } else {
                    record.setStatus(PayStatus.FAILED.getCode());
                    record.setFailReason(tradeStatus);
                }
                recordService.updateRecord(record);
            }
        } catch (Exception e) {
            log.error("处理支付回调异常", e);
        }
        
        return "success";
    }

    /**
     * 解约回调通知
     */
    @ApiOperation("解约回调通知")
    @PostMapping("/unsure/notify")
    public String unsignNotify(HttpServletRequest request) {
        log.info("收到支付宝解约回调通知");
        
        Map<String, String> params = getRequestParams(request);
        
        // 验证签名
        if (!alipayAutoDeductService.verifySignCallback(params)) {
            log.error("支付宝解约回调验签失败");
            return "failure";
        }
        
        String agreementNo = params.get("agreement_no");
        log.info("支付宝解约成功, agreementNo: {}", agreementNo);
        
        return "success";
    }

    /**
     * 获取请求参数
     */
    private Map<String, String> getRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            params.put(name, request.getParameter(name));
        }
        return params;
    }

    /**
     * 从商户订单号中提取订阅ID
     */
    private Long extractSubscriptionId(String outTradeNo) {
        // 实际业务中需要根据订单号规则解析
        // 这里简化处理
        try {
            if (outTradeNo != null && outTradeNo.startsWith("SUB")) {
                return Long.parseLong(outTradeNo.substring(3, 16));
            }
        } catch (Exception e) {
            log.warn("解析订阅ID失败, outTradeNo: {}", outTradeNo);
        }
        return null;
    }
}
