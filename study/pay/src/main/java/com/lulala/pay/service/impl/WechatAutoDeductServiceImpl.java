package com.lulala.pay.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lulala.pay.config.WechatPayConfig;
import com.lulala.pay.domain.PaySubscription;
import com.lulala.pay.domain.dto.SubscriptionSignResponse;
import com.lulala.pay.service.IWechatAutoDeductService;
import com.lulala.pay.util.WechatPaySignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * 微信自动续费服务实现
 * 基于微信支付委托代扣能力（纯HTTP客户端实现）
 *
 * @author lulala
 */
@Slf4j
@Service
public class WechatAutoDeductServiceImpl implements IWechatAutoDeductService {

    @Resource
    private WechatPayConfig wechatPayConfig;

    private static final String WECHAT_API_BASE = "https://api.mch.weixin.qq.com";

    @Override
    public SubscriptionSignResponse generateSignUrl(PaySubscription subscription) {
        try {
            String url = WECHAT_API_BASE + "/v3/papay/contracts/app";

            JSONObject reqBody = new JSONObject();
            reqBody.put("appid", wechatPayConfig.getAppId());
            reqBody.put("mchid", wechatPayConfig.getMchId());
            reqBody.put("plan_id", buildPlanId(subscription));
            reqBody.put("out_contract_code", subscription.getSubscriptionNo());
            reqBody.put("contract_notify_url", wechatPayConfig.getContractNotifyUrl());
            reqBody.put("contract_display_name", subscription.getProductName());

            String response = doPost(url, reqBody.toJSONString());

            JSONObject respJson = JSON.parseObject(response);

            SubscriptionSignResponse signResponse = new SubscriptionSignResponse();
            signResponse.setSubscriptionId(subscription.getSubscriptionId());
            signResponse.setSubscriptionNo(subscription.getSubscriptionNo());
            signResponse.setSignUrl(respJson.getString("h5_url"));
            signResponse.setSignData(respJson.getString("pre_contract_id"));

            return signResponse;
        } catch (Exception e) {
            log.error("生成微信签约URL失败", e);
            throw new RuntimeException("生成签约URL失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verifySignCallback(String notifyData, String signature) {
        // 微信支付V3签名验证
        try {
            return WechatPaySignatureUtil.verify(
                    notifyData, 
                    signature, 
                    wechatPayConfig.getApiV3Key()
            );
        } catch (Exception e) {
            log.error("微信签名验证失败", e);
            return false;
        }
    }

    @Override
    public String parseAgreementNo(String notifyData) {
        try {
            JSONObject json = JSON.parseObject(notifyData);
            JSONObject resource = json.getJSONObject("resource");
            if (resource != null) {
                return resource.getString("contract_id");
            }
            return json.getString("contract_id");
        } catch (Exception e) {
            log.error("解析微信签约回调失败", e);
            return null;
        }
    }

    @Override
    public boolean cancel(String agreementNo) {
        try {
            String url = WECHAT_API_BASE + "/v3/papay/contracts/" + agreementNo + "/terminate";

            JSONObject reqBody = new JSONObject();
            reqBody.put("mchid", wechatPayConfig.getMchId());
            reqBody.put("contract_termination_remark", "用户主动解约");

            String response = doPost(url, reqBody.toJSONString());
            return response != null;
        } catch (Exception e) {
            log.error("微信解约失败", e);
            return false;
        }
    }

    @Override
    public boolean deduct(PaySubscription subscription, String outTradeNo, BigDecimal amount, String subject) {
        try {
            String url = WECHAT_API_BASE + "/v3/pay/transactions/app";

            JSONObject reqBody = new JSONObject();
            reqBody.put("appid", wechatPayConfig.getAppId());
            reqBody.put("mchid", wechatPayConfig.getMchId());
            reqBody.put("description", subject);
            reqBody.put("out_trade_no", outTradeNo);
            reqBody.put("notify_url", wechatPayConfig.getNotifyUrl() + "/wechat/pay/notify");

            JSONObject amountObj = new JSONObject();
            amountObj.put("total", amount.multiply(new BigDecimal("100")).intValue());
            amountObj.put("currency", "CNY");
            reqBody.put("amount", amountObj);

            JSONObject attach = new JSONObject();
            attach.put("contract_id", subscription.getAgreementNo());
            reqBody.put("attach", attach.toJSONString());

            String response = doPost(url, reqBody.toJSONString());

            if (response != null) {
                JSONObject respJson = JSON.parseObject(response);
                String prepayId = respJson.getString("prepay_id");
                return prepayId != null;
            }
            return false;
        } catch (Exception e) {
            log.error("微信扣款异常", e);
            return false;
        }
    }

    @Override
    public String queryDeductResult(String outTradeNo) {
        try {
            String url = String.format(
                    WECHAT_API_BASE + "/v3/pay/transactions/out-trade-no/%s?mchid=%s",
                    outTradeNo, wechatPayConfig.getMchId());

            String response = doGet(url);
            if (response != null) {
                JSONObject respJson = JSON.parseObject(response);
                return respJson.getString("trade_state");
            }
            return null;
        } catch (Exception e) {
            log.error("查询微信扣款结果失败", e);
            return null;
        }
    }

    /**
     * 构建计划ID（需要在微信商户平台创建扣款模板）
     */
    private String buildPlanId(PaySubscription subscription) {
        // 实际应用中需要根据订阅类型返回对应的计划ID
        return "12345";
    }

    /**
     * 发送POST请求（带微信签名）
     */
    private String doPost(String url, String body) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("Accept", "application/json");
            
            // 添加微信支付V3签名
            String authorization = WechatPaySignatureUtil.buildAuthorization(
                    "POST", 
                    extractUrlPath(url),
                    body,
                    wechatPayConfig.getMchId(),
                    wechatPayConfig.getMerchantSerialNumber(),
                    wechatPayConfig.getPrivateKeyPath()
            );
            httpPost.addHeader("Authorization", authorization);
            httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                if (statusCode >= 200 && statusCode < 300) {
                    return result;
                } else {
                    log.error("微信支付API请求失败, statusCode: {}, response: {}", statusCode, result);
                    return null;
                }
            }
        }
    }

    /**
     * 发送GET请求（带微信签名）
     */
    private String doGet(String url) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Content-Type", "application/json");
            httpGet.addHeader("Accept", "application/json");
            
            // 添加微信支付V3签名
            String authorization = WechatPaySignatureUtil.buildAuthorization(
                    "GET",
                    extractUrlPath(url),
                    "",
                    wechatPayConfig.getMchId(),
                    wechatPayConfig.getMerchantSerialNumber(),
                    wechatPayConfig.getPrivateKeyPath()
            );
            httpGet.addHeader("Authorization", authorization);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                if (statusCode >= 200 && statusCode < 300) {
                    return result;
                } else {
                    log.error("微信支付API请求失败, statusCode: {}, response: {}", statusCode, result);
                    return null;
                }
            }
        }
    }

    /**
     * 提取URL路径
     */
    private String extractUrlPath(String url) {
        try {
            java.net.URL uri = new java.net.URL(url);
            String path = uri.getPath();
            if (uri.getQuery() != null) {
                path += "?" + uri.getQuery();
            }
            return path;
        } catch (Exception e) {
            return url;
        }
    }
}
