package com.tuzki.mall.product.search.vo;

/**
 * 商品搜索聚合项视图对象，用于返回品牌、分类或价格区间的聚合名称和命中数量。
 */
public class ProductSearchAggVO {

    private String key;

    private Long count;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
