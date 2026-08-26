package com.tuzki.mall.admin.product.search;

/**
 * 商品搜索索引服务接口，负责维护 Elasticsearch 中的商品索引和商品文档。
 */
public interface ProductSearchIndexService {

    /**
     * 创建商品搜索索引；如果索引已存在则直接返回。
     */
    void createIndexIfAbsent();

    /**
     * 同步单个商品文档到 Elasticsearch。
     *
     * @param productId 商品 ID，用于从 MySQL 读取商品、分类、SKU 和库存数据并写入 ES
     */
    void syncProduct(Long productId);

    /**
     * 从 Elasticsearch 删除单个商品文档。
     *
     * @param productId 商品 ID，用作 ES 文档 ID
     */
    void deleteProduct(Long productId);

    /**
     * 重建全部商品搜索索引，将 MySQL 中未删除商品批量写入 Elasticsearch。
     */
    void rebuildAll();
}
