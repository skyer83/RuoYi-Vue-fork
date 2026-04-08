package com.lulala.pay.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lulala.pay.domain.PayRecord;
import com.lulala.pay.domain.enums.PayStatus;
import com.lulala.pay.service.IPayRecordService;
import com.lulala.pay.service.IPaySubscriptionService;
import com.lulala.pay.service.IWechatAutoDeductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.math.BigDecimal;

/**
 * 微信支付回调控制器
 *
 * @author lulala
 */
@Slf4j
@Api(tags = "微信支付回调")
@RestController
@RequestMapping("/wechat")
public class WechatCallbackController {

    @Resource
    private IWechatAutoDeductService wechatAutoDeductService;

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
        log.info("收到微信签约回调通知");
        
        try {
            String notifyData = readNotifyData(request);
            String signature = request.getHeader("Wechatpay-Signature");
            
            // 验证签名
            if (!wechatAutoDeductService.verifySignCallback(notifyData, signature)) {
                log.error("微信签约回调验签失败");
                return buildFailResponse();
            }
            
            // 解析签约结果
            JSONObject json = JSON.parseObject(notifyData);
            JSONObject resource = json.getJSONObject("resource");
            String contractId = resource.getString("contract_id");
            String outContractCode = resource.getString("out_contract_code");
            
            log.info("微信签约成功, contractId: {}, outContractCode: {}", contractId, outContractCode);
            
            // 更新订阅状态
            Long subscriptionId = extractSubscriptionId(outContractCode);
            if (subscriptionId != null) {
                subscriptionService.handleSignSuccess(subscriptionId, contractId);
            }
            
            return buildSuccessResponse();
        } catch (Exception e) {
            log.error("处理微信签约回调异常", e);
            return buildFailResponse();
        }
    }

    /**
     * 支付回调通知
     */
    @ApiOperation("支付回调通知")
    @PostMapping("/pay/notify")
    public String payNotify(HttpServletRequest request) {
        log.info("收到微信支付回调通知");
        
        try {
            String notifyData = readNotifyData(request);
            String signature = request.getHeader("Wechatpay-Signature");
            
            // 验证签名
            if (!wechatAutoDeductService.verifySignCallback(notifyData, signature)) {
                log.error("微信支付回调验签失败");
                return buildFailResponse();
            }
            
            JSONObject json = JSON.parseObject(notifyData);
            JSONObject resource = json.getJSONObject("resource");
            
            String outTradeNo = resource.getString("out_trade_no");
            String transactionId = resource.getString("transaction_id");
            String tradeState = resource.getString("trade_state");
            Long total = resource.getLong("amount");
            
            log.info("微信支付回调, outTradeNo: {}, transactionId: {}, tradeState: {}",
                    outTradeNo, transactionId, tradeState);
            
            // 更新支付记录状态
            PayRecord record = recordService.getByOutTradeNo(outTradeNo);
            if (record != null) {
                if ("SUCCESS".equals(tradeState)) {
                    record.setStatus(PayStatus.SUCCESS.getCode());
                    record.setTradeNo(transactionId);
                    record.setPayTime(new java.util.Date());
                    if (total != null) {
                        record.setPayAmount(new BigDecimal(total).divide(new BigDecimal("100")));
                    }
                } else {
                    record.setStatus(PayStatus.FAILED.getCode());
                    record.setFailReason(tradeState);
                }
                recordService.updateRecord(record);
            }
            
            return buildSuccessResponse();
        } catch (Exception e) {
            log.error("处理微信支付回调异常", e);
            return buildFailResponse();
        }
    }

    /**
     * 解约回调通知
     */
    @ApiOperation("解约回调通知")
    @PostMapping("/unsign/notify")
    public String unsignNotify(HttpServletRequest request) {
        log.info("收到微信解约回调通知");
        
        try {
            String notifyData = readNotifyData(request);
            String signature = request.getHeader("Wechatpay-Signature");
            
            // 验证签名
            if (!wechatAutoDeductService.verifySignCallback(notifyData, signature)) {
                log.error("微信解约回调验签失败");
                return buildFailResponse();
            }
            
            JSONObject json = JSON.parseObject(notifyData);
            JSONObject resource = json.getJSONObject("resource");
            String contractId = resource.getString("contract_id");
            
            log.info("微信解约成功, contractId: {}", contractId);
            
            return buildSuccessResponse();
        } catch (Exception e) {
            log.error("处理微信解约回调异常", e);
            return buildFailResponse();
        }
    }

    /**
     * 读取回调数据
     */
    private String readNotifyData(HttpServletRequest request) {
        try {
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("读取回调数据失败", e);
            return "";
        }
    }

    /**
     * 构建成功响应
     */
    private String buildSuccessResponse() {
        JSONObject json = new JSONObject();
        json.put("code", "SUCCESS");
        json.put("message", "成功");
        return json.toJSONString();
    }

    /**
     * 构建失败响应
     */
    private String buildFailResponse() {
        JSONObject json = new JSONObject();
        json.put("code", "FAIL");
        json.put("message", "失败");
        return json.toJSONString();
    }

    /**
     * 从商户订单号中提取订阅ID
     */
    private Long extractSubscriptionId(String outContractCode) {
        try {
            if (outContractCode != null && outContractCode.startsWith("SUB")) {
                return Long.parseLong(outContractCode.substring(3, 16));
            }
        } catch (Exception e) {
            log.warn("解析订阅ID失败, outContractCode: {}", outContractCode);
        }
        return null;
    }
}
