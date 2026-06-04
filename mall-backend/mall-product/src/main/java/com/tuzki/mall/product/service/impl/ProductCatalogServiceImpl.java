package com.tuzki.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.tuzki.mall.common.api.CursorPageResult;
import com.tuzki.mall.common.api.PageResult;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.entity.Category;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.CategoryMapper;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.product.sentinel.ProductHotSentinelResources;
import com.tuzki.mall.product.service.ProductCatalogService;
import com.tuzki.mall.product.service.ProductDetailCacheService;
import com.tuzki.mall.product.service.ProductHotDetailCacheService;
import com.tuzki.mall.product.vo.CategoryVO;
import com.tuzki.mall.product.vo.ProductDetailVO;
import com.tuzki.mall.product.vo.ProductSummaryVO;
import com.tuzki.mall.product.vo.SkuVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品目录查询服务默认实现，负责分类、商品列表、推荐商品、商品详情和 SKU 查询。
 */
@Service
public class ProductCatalogServiceImpl implements ProductCatalogService {


    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCatalogServiceImpl.class);

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private static final int DEFAULT_PAGE_NO = 1;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 50;

    private final CategoryMapper categoryMapper;

    private final ProductMapper productMapper;

    private final SkuMapper skuMapper;

    private final ProductDetailCacheService productDetailCacheService;

    private final ProductHotDetailCacheService productHotDetailCacheService;

    public ProductCatalogServiceImpl(CategoryMapper categoryMapper, ProductMapper productMapper, SkuMapper skuMapper,
                                     ProductDetailCacheService productDetailCacheService,
                                     ProductHotDetailCacheService productHotDetailCacheService) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.productDetailCacheService = productDetailCacheService;
        this.productHotDetailCacheService = productHotDetailCacheService;
    }

    @Override
    public List<CategoryVO> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, ACTIVE_STATUS)
                        .eq(Category::getDeleted, NOT_DELETED)
                        .orderByAsc(Category::getSort)
                        .orderByDesc(Category::getId))
                .stream()
                .map(this::toCategoryVO)
                .toList();
    }

    @Override
    public List<ProductSummaryVO> listProducts(Long categoryId) {
        LambdaQueryWrapper<Product> queryWrapper = activeProductQuery()
                .orderByAsc(Product::getSort)
                .orderByDesc(Product::getId);
        if (categoryId != null) {
            queryWrapper.eq(Product::getCategoryId, categoryId);
        }
        return productMapper.selectList(queryWrapper)
                .stream()
                .map(this::toProductSummaryVO)
                .toList();
    }

    @Override
    public PageResult<ProductSummaryVO> recommendProducts(Integer pageNo, Integer pageSize) {
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        long offset = (long) (safePageNo - 1) * safePageSize;
        long total = productMapper.selectCount(activeProductQuery());
        List<ProductSummaryVO> records = productMapper.selectList(activeProductQuery()
                        .orderByAsc(Product::getSort)
                        .orderByDesc(Product::getId)
                        .last("LIMIT " + safePageSize + " OFFSET " + offset))
                .stream()
                .map(this::toProductSummaryVO)
                .toList();
        return new PageResult<>(safePageNo, safePageSize, total, records);
    }

    @Override
    public CursorPageResult<ProductSummaryVO> scrollRecommendProducts(Integer pageSize, Integer lastSort, Long lastId) {
        int safePageSize = normalizePageSize(pageSize);
        List<Product> products = productMapper.selectList(applyRecommendCursor(activeProductQuery(), lastSort, lastId)
                .orderByAsc(Product::getSort)
                .orderByDesc(Product::getId)
                .last("LIMIT " + (safePageSize + 1)));

        boolean hasMore = products.size() > safePageSize;
        List<Product> visibleProducts = products.stream()
                .limit(safePageSize)
                .toList();
        List<ProductSummaryVO> records = visibleProducts.stream()
                .map(this::toProductSummaryVO)
                .toList();

        Product lastProduct = visibleProducts.isEmpty() ? null : visibleProducts.get(visibleProducts.size() - 1);
        Integer nextSort = lastProduct == null ? null : lastProduct.getSort();
        Long nextId = lastProduct == null ? null : lastProduct.getId();
        return new CursorPageResult<>(records, nextSort, nextId, hasMore);
    }

    @Override
    public ProductDetailVO getProductById(Long productId) {
        // 先从缓存中读取
        ProductDetailVO cachedProductDetailVO = productDetailCacheService.get(productId);
        if (cachedProductDetailVO != null) {
            return cachedProductDetailVO;
        }
        // 空值缓存，直接返回，防止缓存穿透
        if (productDetailCacheService.isNullValueCached(productId)) {
            throw new BusinessException(404, "product not found");
        }

        ProductDetailVO productDetailVO = loadProductDetailFromDatabase(productId);
        if (productDetailVO == null) {
            productDetailCacheService.putNullValue(productId); // 设置空值缓存
            throw new BusinessException(404, "product not found");
        }

        productDetailCacheService.put(productId, productDetailVO);
        return productDetailVO;
    }

    @Override
    @SentinelResource(
            value = ProductHotSentinelResources.HOT_PRODUCT_DETAIL,
            blockHandler = "handleHotProductDetailBlocked", // 触发限流、熔断降级都会走这个方法
            fallback = "handleHotProductDetailFallback", // 其他异常走这个方法，BusinessException走原有逻辑，比如抛出异常
            exceptionsToIgnore = BusinessException.class)
    public ProductDetailVO getHotProductById(Long productId) {
        ProductDetailVO productDetailVO = productHotDetailCacheService.getOrLoad(
                productId,
                () -> loadProductDetailFromDatabase(productId));
        if (productDetailVO == null) {
            throw new BusinessException(404, "product not found");
        }
        return productDetailVO;
    }

    public ProductDetailVO handleHotProductDetailBlocked(Long productId, BlockException exception) {
        LOGGER.info("商品[{}]触发blockHandler",productId);
        if (exception instanceof DegradeException) {
            throw new BusinessException(503, "hot product detail degraded");
        }
        throw new BusinessException(429, "hot product detail is busy");
    }

    public ProductDetailVO handleHotProductDetailFallback(Long productId, Throwable throwable) {
        throw new BusinessException(503, "hot product detail degraded");
    }

    @Override
    public SkuVO getSkuById(Long skuId) {
        Sku sku = skuMapper.selectOne(activeSkuQuery()
                .eq(Sku::getId, skuId));
        if (sku == null) {
            throw new BusinessException(404, "sku not found");
        }
        return toSkuVO(sku);
    }

    private LambdaQueryWrapper<Product> activeProductQuery() {
        return new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ACTIVE_STATUS)
                .eq(Product::getDeleted, NOT_DELETED);
    }

    private LambdaQueryWrapper<Sku> activeSkuQuery() {
        return new LambdaQueryWrapper<Sku>()
                .eq(Sku::getStatus, ACTIVE_STATUS)
                .eq(Sku::getDeleted, NOT_DELETED);
    }

    private ProductDetailVO loadProductDetailFromDatabase(Long productId) {
        Product product = productMapper.selectOne(activeProductQuery()
                .eq(Product::getId, productId));
        if (product == null) {
            return null;
        }
        ProductDetailVO productDetailVO = toProductDetailVO(product);
        productDetailVO.setSkus(skuMapper.selectList(activeSkuQuery()
                        .eq(Sku::getProductId, productId)
                        .orderByDesc(Sku::getId))
                .stream()
                .map(this::toSkuVO)
                .toList());
        return productDetailVO;
    }

    private LambdaQueryWrapper<Product> applyRecommendCursor(LambdaQueryWrapper<Product> queryWrapper,
                                                             Integer lastSort,
                                                             Long lastId) {
        if (lastSort == null || lastId == null) {
            return queryWrapper;
        }
        return queryWrapper.and(cursor -> cursor
                .gt(Product::getSort, lastSort)
                .or()
                .eq(Product::getSort, lastSort)
                .lt(Product::getId, lastId));
    }

    private CategoryVO toCategoryVO(Category category) {
        CategoryVO categoryVO = new CategoryVO();
        categoryVO.setId(category.getId());
        categoryVO.setParentId(category.getParentId());
        categoryVO.setName(category.getName());
        categoryVO.setLevel(category.getLevel());
        categoryVO.setSort(category.getSort());
        categoryVO.setIconUrl(category.getIconUrl());
        return categoryVO;
    }

    private ProductSummaryVO toProductSummaryVO(Product product) {
        ProductSummaryVO productSummaryVO = new ProductSummaryVO();
        fillProductSummaryVO(productSummaryVO, product);
        return productSummaryVO;
    }

    private ProductDetailVO toProductDetailVO(Product product) {
        ProductDetailVO productDetailVO = new ProductDetailVO();
        fillProductSummaryVO(productDetailVO, product);
        productDetailVO.setDescription(product.getDescription());
        return productDetailVO;
    }

    private void fillProductSummaryVO(ProductSummaryVO productSummaryVO, Product product) {
        productSummaryVO.setId(product.getId());
        productSummaryVO.setCategoryId(product.getCategoryId());
        productSummaryVO.setProductCode(product.getProductCode());
        productSummaryVO.setName(product.getName());
        productSummaryVO.setSubtitle(product.getSubtitle());
        productSummaryVO.setMainImageUrl(product.getMainImageUrl());
        productSummaryVO.setMinPrice(findMinSkuPrice(product.getId()));
    }

    private BigDecimal findMinSkuPrice(Long productId) {
        Sku sku = skuMapper.selectList(activeSkuQuery()
                        .eq(Sku::getProductId, productId)
                        .orderByAsc(Sku::getPrice)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
        return sku == null ? null : sku.getPrice();
    }

    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < DEFAULT_PAGE_NO) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private SkuVO toSkuVO(Sku sku) {
        SkuVO skuVO = new SkuVO();
        skuVO.setId(sku.getId());
        skuVO.setProductId(sku.getProductId());
        skuVO.setSkuCode(sku.getSkuCode());
        skuVO.setSkuName(sku.getSkuName());
        skuVO.setSpecData(sku.getSpecData());
        skuVO.setPrice(sku.getPrice());
        skuVO.setOriginalPrice(sku.getOriginalPrice());
        skuVO.setMainImageUrl(sku.getMainImageUrl());
        return skuVO;
    }
}
