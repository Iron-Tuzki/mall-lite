package com.tuzki.mall.seckill.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀活动视图对象，向前台返回当前可参与活动及其活动商品列表。
 */
public class SeckillActivityVO {

    private Long id;

    private String name;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private List<SeckillSkuVO> skus;

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

    public List<SeckillSkuVO> getSkus() {
        return skus;
    }

    public void setSkus(List<SeckillSkuVO> skus) {
        this.skus = skus;
    }
}
