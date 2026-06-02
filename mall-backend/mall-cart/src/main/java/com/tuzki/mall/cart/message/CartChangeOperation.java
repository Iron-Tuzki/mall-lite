package com.tuzki.mall.cart.message;

/**
 * 购物车变更操作类型，区分更新数量和写入删除墓碑。
 */
public enum CartChangeOperation {
    UPSERT,
    DELETE
}
