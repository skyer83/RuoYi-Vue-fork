package com.lulala.pay.task;

import com.lulala.pay.service.IPaySubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 订阅定时任务
 * 用于自动扣款
 *
 * @author lulala
 */
@Slf4j
@Component
public class SubscriptionScheduledTask {

    @Resource
    private IPaySubscriptionService subscriptionService;

    /**
     * 每小时执行一次自动扣款
     * cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void autoDeduct() {
        log.info("开始执行订阅自动扣款任务...");
        try {
            subscriptionService.scheduledDeduct();
        } catch (Exception e) {
            log.error("订阅自动扣款任务执行异常", e);
        }
    }

    /**
     * 每天凌晨1点执行订阅状态检查
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkSubscriptionStatus() {
        log.info("开始执行订阅状态检查任务...");
        // 可以添加订阅过期检查等逻辑
        log.info("订阅状态检查任务执行完成");
    }
}
