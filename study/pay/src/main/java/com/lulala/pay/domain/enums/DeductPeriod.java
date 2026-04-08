package com.lulala.pay.domain.enums;

/**
 * 扣款周期枚举
 *
 * @author lulala
 */
public enum DeductPeriod {

    /**
     * 每天
     */
    DAILY("daily", "每天"),

    /**
     * 每周
     */
    WEEKLY("weekly", "每周"),

    /**
     * 每月
     */
    MONTHLY("monthly", "每月"),

    /**
     * 每年
     */
    YEARLY("yearly", "每年");

    private final String code;
    private final String name;

    DeductPeriod(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static DeductPeriod getByCode(String code) {
        for (DeductPeriod period : values()) {
            if (period.getCode().equals(code)) {
                return period;
            }
        }
        return null;
    }
}
