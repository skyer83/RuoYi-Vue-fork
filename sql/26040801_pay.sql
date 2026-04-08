-- =============================================
-- 支付模块 - 支付宝、微信自动续费功能
-- 创建日期: 2026-04-08
-- =============================================

-- ----------------------------
-- 1. 订阅表
-- ----------------------------
DROP TABLE IF EXISTS `pay_subscription`;
CREATE TABLE `pay_subscription` (
    `subscription_id`   BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '订阅ID',
    `user_id`           BIGINT(20)      NOT NULL COMMENT '用户ID',
    `subscription_no`   VARCHAR(64)     NOT NULL COMMENT '订阅编号',
    `pay_method`        VARCHAR(20)     NOT NULL COMMENT '支付方式：alipay-支付宝，wechat-微信',
    `agreement_no`      VARCHAR(128)    DEFAULT NULL COMMENT '协议号（支付平台返回的签约协议ID）',
    `product_name`      VARCHAR(100)    NOT NULL COMMENT '产品名称',
    `product_code`      VARCHAR(50)     DEFAULT NULL COMMENT '产品编码',
    `amount`            DECIMAL(10,2)   NOT NULL COMMENT '订阅金额',
    `currency`          VARCHAR(10)     DEFAULT 'CNY' COMMENT '币种',
    `period`            VARCHAR(20)     NOT NULL COMMENT '扣款周期：daily-每天，weekly-每周，monthly-每月，yearly-每年',
    `period_count`      INT(11)         DEFAULT 1 COMMENT '扣款周期数（与period配合使用，如每2周扣款）',
    `status`            TINYINT(4)      DEFAULT 0 COMMENT '订阅状态：0-待签约，1-已签约，2-已解约，3-已过期',
    `signed_time`       DATETIME        DEFAULT NULL COMMENT '签约时间',
    `cancelled_time`    DATETIME        DEFAULT NULL COMMENT '解约时间',
    `next_deduct_time`  DATETIME        DEFAULT NULL COMMENT '下次扣款时间',
    `last_deduct_time`  DATETIME        DEFAULT NULL COMMENT '最后扣款时间',
    `deduct_count`      INT(11)         DEFAULT 0 COMMENT '累计扣款次数',
    `max_deduct_count`  INT(11)         DEFAULT 0 COMMENT '最大扣款次数（0表示不限次数）',
    `start_time`        DATETIME        DEFAULT NULL COMMENT '订阅开始时间',
    `end_time`          DATETIME        DEFAULT NULL COMMENT '订阅结束时间',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`            VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`subscription_id`),
    UNIQUE KEY `uk_subscription_no` (`subscription_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_agreement_no` (`agreement_no`),
    KEY `idx_status` (`status`),
    KEY `idx_next_deduct_time` (`next_deduct_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='订阅表';

-- ----------------------------
-- 2. 支付记录表
-- ----------------------------
DROP TABLE IF EXISTS `pay_record`;
CREATE TABLE `pay_record` (
    `record_id`         BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `subscription_id`   BIGINT(20)      NOT NULL COMMENT '订阅ID',
    `user_id`           BIGINT(20)      NOT NULL COMMENT '用户ID',
    `out_trade_no`      VARCHAR(64)     NOT NULL COMMENT '商户订单号',
    `trade_no`          VARCHAR(128)    DEFAULT NULL COMMENT '支付平台交易号',
    `pay_method`        VARCHAR(20)     NOT NULL COMMENT '支付方式：alipay-支付宝，wechat-微信',
    `amount`            DECIMAL(10,2)   NOT NULL COMMENT '订单金额',
    `pay_amount`        DECIMAL(10,2)   DEFAULT NULL COMMENT '实际支付金额',
    `currency`          VARCHAR(10)     DEFAULT 'CNY' COMMENT '币种',
    `subject`           VARCHAR(200)    DEFAULT NULL COMMENT '订单标题',
    `body`              VARCHAR(500)    DEFAULT NULL COMMENT '订单描述',
    `status`            TINYINT(4)      DEFAULT 0 COMMENT '支付状态：0-待支付，1-支付成功，2-支付失败，3-已退款',
    `pay_time`          DATETIME        DEFAULT NULL COMMENT '支付时间',
    `close_time`        DATETIME        DEFAULT NULL COMMENT '关闭时间',
    `auto_deduct`       TINYINT(4)      DEFAULT 0 COMMENT '是否自动扣款：0-否，1-是',
    `fail_reason`       VARCHAR(500)    DEFAULT NULL COMMENT '扣款失败原因',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`record_id`),
    UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
    KEY `idx_subscription_id` (`subscription_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_trade_no` (`trade_no`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- ----------------------------
-- 3. 字典数据 - 支付方式
-- ----------------------------
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`) 
VALUES ('支付方式', 'pay_method', '0', 'admin', NOW(), '支付方式字典');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) 
VALUES 
(1, '支付宝', 'alipay', 'pay_method', '', 'primary', 'Y', '0', 'admin', NOW(), '支付宝支付'),
(2, '微信支付', 'wechat', 'pay_method', '', 'success', 'N', '0', 'admin', NOW(), '微信支付');

-- ----------------------------
-- 4. 字典数据 - 订阅状态
-- ----------------------------
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`) 
VALUES ('订阅状态', 'subscription_status', '0', 'admin', NOW(), '订阅状态字典');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) 
VALUES 
(1, '待签约', '0', 'subscription_status', '', 'info', 'Y', '0', 'admin', NOW(), '待签约'),
(2, '已签约', '1', 'subscription_status', '', 'success', 'N', '0', 'admin', NOW(), '已签约'),
(3, '已解约', '2', 'subscription_status', '', 'warning', 'N', '0', 'admin', NOW(), '已解约'),
(4, '已过期', '3', 'subscription_status', '', 'danger', 'N', '0', 'admin', NOW(), '已过期');

-- ----------------------------
-- 5. 字典数据 - 支付状态
-- ----------------------------
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`) 
VALUES ('支付状态', 'pay_status', '0', 'admin', NOW(), '支付状态字典');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) 
VALUES 
(1, '待支付', '0', 'pay_status', '', 'info', 'Y', '0', 'admin', NOW(), '待支付'),
(2, '支付成功', '1', 'pay_status', '', 'success', 'N', '0', 'admin', NOW(), '支付成功'),
(3, '支付失败', '2', 'pay_status', '', 'danger', 'N', '0', 'admin', NOW(), '支付失败'),
(4, '已退款', '3', 'pay_status', '', 'warning', 'N', '0', 'admin', NOW(), '已退款');

-- ----------------------------
-- 6. 字典数据 - 扣款周期
-- ----------------------------
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`) 
VALUES ('扣款周期', 'deduct_period', '0', 'admin', NOW(), '扣款周期字典');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) 
VALUES 
(1, '每天', 'daily', 'deduct_period', '', '', 'N', '0', 'admin', NOW(), '每天扣款'),
(2, '每周', 'weekly', 'deduct_period', '', '', 'N', '0', 'admin', NOW(), '每周扣款'),
(3, '每月', 'monthly', 'deduct_period', '', '', 'Y', '0', 'admin', NOW(), '每月扣款'),
(4, '每年', 'yearly', 'deduct_period', '', '', 'N', '0', 'admin', NOW(), '每年扣款');
