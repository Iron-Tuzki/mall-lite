package com.tuzki.mall.product.hot;

/**
 * 商品热点行为类型，定义首页热门商品统计中各类用户行为的默认热度分值。
 */
public enum ProductHotAction {

    VIEW(1),
    FAVORITE(4),
    CART_ADD(6),
    PAY_SUCCESS(10);

    private final int score;

    ProductHotAction(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
