package com.tuzki.mall.admin.seckill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 后台秒杀活动请求对象，用于管理员创建或编辑秒杀活动基础信息。
 */
public class AdminSeckillActivityRequest {

    @NotBlank(message = "name must not be blank")
    private String name;

    @NotNull(message = "startTime must not be null")
    private LocalDateTime startTime;

    @NotNull(message = "endTime must not be null")
    private LocalDateTime endTime;

    @NotNull(message = "status must not be null")
    private Integer status;

    private String remark;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
