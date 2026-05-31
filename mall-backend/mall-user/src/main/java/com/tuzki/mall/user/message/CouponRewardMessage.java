package com.tuzki.mall.user.message;

import java.io.Serial;
import java.io.Serializable;
import java.time.YearMonth;

/**
 * 发放优惠券消息体
 *
 * @author lanshu
 * @date 2026-05-31
 */
public class CouponRewardMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1;

    private Long userId;

    private int continuousSignedDays;

    private YearMonth rewardMonth;

    /**
     * 签到奖励所需天数
     */
    private Integer requiredDays;

    /**
     * 消费券ID
     */
    private Long templateId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getContinuousSignedDays() {
        return continuousSignedDays;
    }

    public void setContinuousSignedDays(int continuousSignedDays) {
        this.continuousSignedDays = continuousSignedDays;
    }

    public YearMonth getRewardMonth() {
        return rewardMonth;
    }

    public void setRewardMonth(YearMonth rewardMonth) {
        this.rewardMonth = rewardMonth;
    }

    public Integer getRequiredDays() {
        return requiredDays;
    }

    public void setRequiredDays(Integer requiredDays) {
        this.requiredDays = requiredDays;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }
}
