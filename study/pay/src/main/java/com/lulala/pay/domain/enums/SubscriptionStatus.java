package com.lulala.pay.domain.enums;

/**
 * 订阅状态枚举
 *
 * @author lulala
 */
public enum SubscriptionStatus {

    /**
     * 待签约
     */
    PENDING(0, "待签约"),

    /**
     * 已签约
     */
    SIGNED(1, "已签约"),

    /**
     * 已解约
     */
    CANCELLED(2, "已解约"),

    /**
     * 已过期
     */
    EXPIRED(3, "已过期");

    private final int code;
    private final String name;

    SubscriptionStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static SubscriptionStatus getByCode(int code) {
        for (SubscriptionStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
