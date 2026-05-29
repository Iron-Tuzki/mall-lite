package com.tuzki.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.product.entity.ProductFavorite;
import com.tuzki.mall.product.vo.ProductFavoriteVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品收藏数据访问接口，负责收藏关系写入、取消以及收藏商品列表查询。
 */
public interface ProductFavoriteMapper extends BaseMapper<ProductFavorite> {

    /**
     * 取消指定用户对指定商品的收藏。
     *
     * @param userId 用户 ID
     * @param productId 商品 ID
     * @return 影响行数，1 表示取消成功，0 表示原本未收藏
     */
    @Update("""
            UPDATE pms_product_favorite
            SET deleted = 1,
                update_time = NOW()
            WHERE user_id = #{userId}
              AND product_id = #{productId}
              AND deleted = 0
            """)
    int cancelFavorite(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 查询用户最近收藏的商品。
     *
     * @param userId 用户 ID
     * @param limit 查询条数
     * @return 收藏商品摘要列表
     */
    @Select("""
            SELECT p.id AS product_id,
                   p.name,
                   p.subtitle,
                   p.main_image_url,
                   sku_price.min_price AS min_price,
                   f.create_time AS favorite_time
            FROM pms_product_favorite f
            INNER JOIN pms_product p ON p.id = f.product_id
            LEFT JOIN (
                SELECT product_id,
                       MIN(price) AS min_price
                FROM pms_sku
                WHERE status = 1
                  AND deleted = 0
                GROUP BY product_id
            ) sku_price ON sku_price.product_id = p.id
            WHERE f.user_id = #{userId}
              AND f.deleted = 0
              AND p.status = 1
              AND p.deleted = 0
            ORDER BY f.create_time DESC, f.id DESC
            LIMIT #{limit}
            """)
    List<ProductFavoriteVO> listFavorites(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询用户所有未删除收藏的商品 ID，用于重建 Redis 收藏集合缓存。
     *
     * @param userId 用户 ID
     * @return 收藏商品 ID 列表
     */
    @Select("""
            SELECT product_id
            FROM pms_product_favorite
            WHERE user_id = #{userId}
              AND deleted = 0
            """)
    List<Long> selectFavoriteProductIdsByUser(@Param("userId") Long userId);

    /**
     * 查询用户已收藏的商品 ID。
     *
     * @param userId 用户 ID
     * @param productIds 待判断商品 ID 列表
     * @return 已收藏商品 ID 列表
     */
    @Select("""
            <script>
            SELECT product_id
            FROM pms_product_favorite
            WHERE user_id = #{userId}
              AND deleted = 0
              AND product_id IN
              <foreach collection="productIds" item="productId" open="(" separator="," close=")">
                #{productId}
              </foreach>
            </script>
            """)
    List<Long> selectFavoritedProductIds(@Param("userId") Long userId, @Param("productIds") List<Long> productIds);
}
