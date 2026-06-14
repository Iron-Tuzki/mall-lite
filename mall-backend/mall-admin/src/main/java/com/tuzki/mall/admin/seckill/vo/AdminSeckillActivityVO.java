package com.tuzki.mall.admin.seckill.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台秒杀活动视图对象，用于返回活动基础信息和活动商品明细。
 */
public class AdminSeckillActivityVO {

    private Long id;

    private String name;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private String remark;

    private List<AdminSeckillSkuVO> skus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<AdminSeckillSkuVO> getSkus() {
        return skus;
    }

    public void setSkus(List<AdminSeckillSkuVO> skus) {
        this.skus = skus;
    }
}
