package com.tuzki.mall.common.api;

import java.util.List;

/**
 * 分页查询结果对象，用于统一返回当前页码、每页数量、总记录数和当前页数据。
 *
 * @param <T> 分页列表中的数据类型
 */
public class PageResult<T> {

    private final long pageNo;

    private final long pageSize;

    private final long total;

    private final List<T> records;

    public PageResult(long pageNo, long pageSize, long total, List<T> records) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
        this.records = records;
    }

    public long getPageNo() {
        return pageNo;
    }

    public long getPageSize() {
        return pageSize;
    }

    public long getTotal() {
        return total;
    }

    public List<T> getRecords() {
        return records;
    }
}
