package com.lulala.pay.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.lulala.pay.config.AlipayConfig;
import com.lulala.pay.domain.PaySubscription;
import com.lulala.pay.domain.dto.SubscriptionSignResponse;
import com.lulala.pay.service.IAlipayAutoDeductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 支付宝自动续费服务实现
 * 基于支付宝周期扣款能力
 *
 * @author lulala
 */
@Slf4j
@Service
public class AlipayAutoDeductServiceImpl implements IAlipayAutoDeductService {

    @Resource
    private AlipayConfig alipayConfig;

    private AlipayClient alipayClient;

    @PostConstruct
    public void init() {
        alipayClient = new DefaultAlipayClient(
                alipayConfig.getActualGatewayUrl(),
                alipayConfig.getAppId(),
                alipayConfig.getPrivateKey(),
                "json",
                alipayConfig.getCharset(),
                alipayConfig.getAlipayPublicKey(),
                alipayConfig.getSignType()
        );
    }

    @Override
    public SubscriptionSignResponse generateSignUrl(PaySubscription subscription) {
        try {
            // 构建签约URL（使用支付宝周期扣款产品）
            // 实际项目中需要调用支付宝签约API
            // 这里构建一个简化的签约链接
            JSONObject bizContent = new JSONObject();
            bizContent.put("personal_product_code", "CYCLE_PAY_AUTH_P");
            bizContent.put("sign_scene", "INDUSTRY|DIGITAL_MEDIA");
            bizContent.put("external_agreement_no", subscription.getSubscriptionNo());
            bizContent.put("period_params", buildPeriodParams(subscription));
            bizContent.put("amount_limit", subscription.getAmount().setScale(2, RoundingMode.HALF_UP).toString());

            String signUrl = buildSignPageUrl(bizContent.toJSONString(), subscription);

            SubscriptionSignResponse signResponse = new SubscriptionSignResponse();
            signResponse.setSubscriptionId(subscription.getSubscriptionId());
            signResponse.setSubscriptionNo(subscription.getSubscriptionNo());
            signResponse.setSignUrl(signUrl);

            return signResponse;
        } catch (Exception e) {
            log.error("生成支付宝签约URL失败", e);
            throw new RuntimeException("生成签约URL失败: " + e.getMessage());
        }
    }

    /**
     * 构建签约页面URL
     */
    private String buildSignPageUrl(String bizContent, PaySubscription subscription) throws Exception {
        Map<String, String> params = new java.util.TreeMap<>();
        params.put("app_id", alipayConfig.getAppId());
        params.put("method", "alipay.user.agreement.page.sign");
        params.put("charset", alipayConfig.getCharset());
        params.put("sign_type", alipayConfig.getSignType());
        params.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        params.put("version", "1.0");
        params.put("return_url", alipayConfig.getReturnUrl());
        params.put("notify_url", alipayConfig.getNotifyUrl() + "/alipay/sign/notify");
        params.put("biz_content", bizContent);

        String sign = AlipaySignature.rsaSign(params, alipayConfig.getPrivateKey(), alipayConfig.getCharset());
        params.put("sign", sign);

        StringBuilder urlBuilder = new StringBuilder(alipayConfig.getActualGatewayUrl());
        urlBuilder.append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            urlBuilder.append(entry.getKey()).append("=")
                    .append(java.net.URLEncoder.encode(entry.getValue(), "UTF-8"))
                    .append("&");
        }
        return urlBuilder.substring(0, urlBuilder.length() - 1);
    }

    /**
     * 构建周期参数
     */
    private JSONObject buildPeriodParams(PaySubscription subscription) {
        JSONObject params = new JSONObject();
        params.put("period_type", convertPeriodType(subscription.getPeriod()));
        params.put("period", subscription.getPeriodCount());
        params.put("single_amount", subscription.getAmount().setScale(2, RoundingMode.HALF_UP).toString());
        return params;
    }

    @Override
    public boolean verifySignCallback(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );
        } catch (AlipayApiException e) {
            log.error("验证支付宝签名失败", e);
            return false;
        }
    }

    @Override
    public String parseAgreementNo(Map<String, String> params) {
        return params.get("agreement_no");
    }

    @Override
    public boolean cancel(String agreementNo) {
        try {
            // 解约需要调用支付宝解约API
            // 这里简化处理
            log.info("支付宝解约, agreementNo: {}", agreementNo);
            return true;
        } catch (Exception e) {
            log.error("支付宝解约失败", e);
            return false;
        }
    }

    @Override
    public boolean deduct(PaySubscription subscription, String outTradeNo, BigDecimal amount, String subject) {
        try {
            AlipayTradePayRequest request = new AlipayTradePayRequest();

            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", outTradeNo);
            bizContent.put("total_amount", amount.setScale(2, RoundingMode.HALF_UP).toString());
            bizContent.put("subject", subject);
            bizContent.put("product_code", "CYCLE_PAY_AUTH");

            // 设置协议参数，用于周期扣款
            JSONObject agreementParams = new JSONObject();
            agreementParams.put("agreement_no", subscription.getAgreementNo());
            bizContent.put("agreement_params", agreementParams);

            request.setBizContent(bizContent.toJSONString());
            request.setNotifyUrl(alipayConfig.getNotifyUrl() + "/alipay/pay/notify");

            AlipayTradePayResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("支付宝扣款成功, outTradeNo: {}, tradeNo: {}", outTradeNo, response.getTradeNo());
                return true;
            } else {
                log.error("支付宝扣款失败, outTradeNo: {}, code: {}, msg: {}",
                        outTradeNo, response.getCode(), response.getMsg());
                return false;
            }
        } catch (AlipayApiException e) {
            log.error("支付宝扣款异常", e);
            return false;
        }
    }

    @Override
    public String queryDeductResult(String outTradeNo) {
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", outTradeNo);
            request.setBizContent(bizContent.toJSONString());

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                return response.getTradeStatus();
            }
            return null;
        } catch (AlipayApiException e) {
            log.error("查询支付宝扣款结果失败", e);
            return null;
        }
    }

    /**
     * 转换周期类型
     */
    private String convertPeriodType(String period) {
        switch (period) {
            case "daily":
                return "DAY";
            case "weekly":
                return "WEEK";
            case "monthly":
                return "MONTH";
            case "yearly":
                return "YEAR";
            default:
                return "MONTH";
        }
    }
}
