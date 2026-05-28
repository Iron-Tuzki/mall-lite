package com.tuzki.mall.common.api;

import java.util.List;

/**
 * 游标分页结果对象，用于无限滚动场景返回当前批次数据、下一次查询游标和是否还有更多数据。
 *
 * @param <T> 分页列表中的数据类型
 */
public class CursorPageResult<T> {

    private final List<T> records;

    private final Integer nextSort;

    private final Long nextId;

    private final boolean hasMore;

    public CursorPageResult(List<T> records, Integer nextSort, Long nextId, boolean hasMore) {
        this.records = records;
        this.nextSort = nextSort;
        this.nextId = nextId;
        this.hasMore = hasMore;
    }

    public List<T> getRecords() {
        return records;
    }

    public Integer getNextSort() {
        return nextSort;
    }

    public Long getNextId() {
        return nextId;
    }

    public boolean isHasMore() {
        return hasMore;
    }
}
