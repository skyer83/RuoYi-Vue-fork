package com.lulala.pay.domain.enums;

/**
 * 支付方式枚举
 *
 * @author lulala
 */
public enum PayMethod {

    /**
     * 支付宝
     */
    ALIPAY("alipay", "支付宝"),

    /**
     * 微信支付
     */
    WECHAT("wechat", "微信支付");

    private final String code;
    private final String name;

    PayMethod(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static PayMethod getByCode(String code) {
        for (PayMethod method : values()) {
            if (method.getCode().equals(code)) {
                return method;
            }
        }
        return null;
    }
}
