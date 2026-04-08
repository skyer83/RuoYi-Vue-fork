package com.lulala.pay.domain.enums;

/**
 * 支付状态枚举
 *
 * @author lulala
 */
public enum PayStatus {

    /**
     * 待支付
     */
    PENDING(0, "待支付"),

    /**
     * 支付成功
     */
    SUCCESS(1, "支付成功"),

    /**
     * 支付失败
     */
    FAILED(2, "支付失败"),

    /**
     * 已退款
     */
    REFUNDED(3, "已退款");

    private final int code;
    private final String name;

    PayStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static PayStatus getByCode(int code) {
        for (PayStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
