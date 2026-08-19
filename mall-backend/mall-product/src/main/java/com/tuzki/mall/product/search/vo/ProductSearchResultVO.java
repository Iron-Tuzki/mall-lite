package com.tuzki.mall.product.search.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品搜索结果视图对象，用于返回分页商品列表和搜索页筛选面板需要的聚合结果。
 */
public class ProductSearchResultVO {

    private long pageNo;

    private long pageSize;

    private long total;

    private List<ProductSearchItemVO> records = new ArrayList<>();

    private List<ProductSearchAggVO> brandAggs = new ArrayList<>();

    private List<ProductSearchAggVO> categoryAggs = new ArrayList<>();

    private List<ProductSearchAggVO> priceAggs = new ArrayList<>();

    public long getPageNo() {
        return pageNo;
    }

    public void setPageNo(long pageNo) {
        this.pageNo = pageNo;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<ProductSearchItemVO> getRecords() {
        return records;
    }

    public void setRecords(List<ProductSearchItemVO> records) {
        this.records = records;
    }

    public List<ProductSearchAggVO> getBrandAggs() {
        return brandAggs;
    }

    public void setBrandAggs(List<ProductSearchAggVO> brandAggs) {
        this.brandAggs = brandAggs;
    }

    public List<ProductSearchAggVO> getCategoryAggs() {
        return categoryAggs;
    }

    public void setCategoryAggs(List<ProductSearchAggVO> categoryAggs) {
        this.categoryAggs = categoryAggs;
    }

    public List<ProductSearchAggVO> getPriceAggs() {
        return priceAggs;
    }

    public void setPriceAggs(List<ProductSearchAggVO> priceAggs) {
        this.priceAggs = priceAggs;
    }
}
