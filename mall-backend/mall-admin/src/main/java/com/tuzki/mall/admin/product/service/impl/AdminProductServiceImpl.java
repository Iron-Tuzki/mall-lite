package com.tuzki.mall.admin.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.admin.product.dto.AdminProductRequest;
import com.tuzki.mall.admin.product.dto.AdminProductSkuRequest;
import com.tuzki.mall.admin.product.service.AdminProductService;
import com.tuzki.mall.admin.product.vo.AdminProductSkuVO;
import com.tuzki.mall.admin.product.vo.AdminProductVO;
import com.tuzki.mall.common.api.PageResult;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.product.entity.Category;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.CategoryMapper;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 后台商品管理默认实现，负责编排商品、SKU 和库存的事务性维护。
 */
@Service
public class AdminProductServiceImpl implements AdminProductService {

    private static final int NOT_DELETED = 0;

    private static final int DELETED = 1;

    private static final int DEFAULT_PAGE_NO = 1;

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MAX_PAGE_SIZE = 100;

    private static final int INITIAL_VERSION = 0;

    private final ProductMapper productMapper;

    private final SkuMapper skuMapper;

    private final CategoryMapper categoryMapper;

    private final InventoryMapper inventoryMapper;

    public AdminProductServiceImpl(ProductMapper productMapper,
                                   SkuMapper skuMapper,
                                   CategoryMapper categoryMapper,
                                   InventoryMapper inventoryMapper) {
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.categoryMapper = categoryMapper;
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    public PageResult<AdminProductVO> listProducts(Integer pageNo,
                                                   Integer pageSize,
                                                   Long categoryId,
                                                   String keyword,
                                                   Integer status) {
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        long offset = (long) (safePageNo - 1) * safePageSize;

        LambdaQueryWrapper<Product> queryWrapper = productQuery(categoryId, keyword, status);
        long total = productMapper.selectCount(queryWrapper);
        List<AdminProductVO> records = productMapper.selectList(productQuery(categoryId, keyword, status)
                        .orderByDesc(Product::getId)
                        .last("LIMIT " + safePageSize + " OFFSET " + offset))
                .stream()
                .map(product -> toProductVO(product, false))
                .toList();
        return new PageResult<>(safePageNo, safePageSize, total, records);
    }

    @Override
    public AdminProductVO getProduct(Long productId) {
        return toProductVO(getProductOrThrow(productId), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductVO createProduct(AdminProductRequest request) {
        ensureCategoryExists(request.getCategoryId());
        ensureProductCodeUnique(request.getProductCode(), null);
        ensureSkuCodesUnique(request.getSkus(), null);

        Product product = new Product();
        fillProduct(product, request);
        product.setDeleted(NOT_DELETED);
        productMapper.insert(product);

        for (AdminProductSkuRequest skuRequest : request.getSkus()) {
            createSkuAndInventory(product.getId(), skuRequest);
        }
        return getProduct(product.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductVO updateProduct(Long productId, AdminProductRequest request) {
        Product product = getProductOrThrow(productId);
        ensureCategoryExists(request.getCategoryId());
        ensureProductCodeUnique(request.getProductCode(), productId);
        ensureSkuCodesUnique(request.getSkus(), productId);

        fillProduct(product, request);
        productMapper.updateById(product);

        Set<Long> retainedSkuIds = new HashSet<>();
        for (AdminProductSkuRequest skuRequest : request.getSkus()) {
            if (skuRequest.getId() == null) {
                Sku sku = createSkuAndInventory(productId, skuRequest);
                retainedSkuIds.add(sku.getId());
            } else {
                updateSkuAndInventory(productId, skuRequest);
                retainedSkuIds.add(skuRequest.getId());
            }
        }
        softDeleteRemovedSkus(productId, retainedSkuIds);
        return getProduct(productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        Product product = getProductOrThrow(productId);
        product.setDeleted(DELETED);
        productMapper.updateById(product);

        List<Sku> skus = listSkus(productId);
        for (Sku sku : skus) {
            sku.setDeleted(DELETED);
            skuMapper.updateById(sku);
            softDeleteInventory(sku.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductVO updateStatus(Long productId, Integer status) {
        Product product = getProductOrThrow(productId);
        product.setStatus(status);
        productMapper.updateById(product);
        return getProduct(productId);
    }

    @Override
    public PageResult<AdminProductSkuVO> listSelectableSkus(Integer pageNo, Integer pageSize, String keyword) {
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        long offset = (long) (safePageNo - 1) * safePageSize;

        LambdaQueryWrapper<Sku> queryWrapper = skuQuery(keyword);
        long total = skuMapper.selectCount(queryWrapper);
        List<AdminProductSkuVO> records = skuMapper.selectList(skuQuery(keyword)
                        .orderByDesc(Sku::getId)
                        .last("LIMIT " + safePageSize + " OFFSET " + offset))
                .stream()
                .map(this::toSkuVO)
                .toList();
        return new PageResult<>(safePageNo, safePageSize, total, records);
    }

    private LambdaQueryWrapper<Product> productQuery(Long categoryId, String keyword, Integer status) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getDeleted, NOT_DELETED);
        if (categoryId != null) {
            queryWrapper.eq(Product::getCategoryId, categoryId);
        }
        if (status != null) {
            queryWrapper.eq(Product::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Product::getName, keyword)
                    .or()
                    .like(Product::getProductCode, keyword));
        }
        return queryWrapper;
    }

    private LambdaQueryWrapper<Sku> skuQuery(String keyword) {
        LambdaQueryWrapper<Sku> queryWrapper = new LambdaQueryWrapper<Sku>()
                .eq(Sku::getDeleted, NOT_DELETED);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Sku::getSkuName, keyword)
                    .or()
                    .like(Sku::getSkuCode, keyword));
        }
        return queryWrapper;
    }

    private void fillProduct(Product product, AdminProductRequest request) {
        product.setCategoryId(request.getCategoryId());
        product.setProductCode(request.getProductCode());
        product.setName(request.getName());
        product.setSubtitle(request.getSubtitle());
        product.setMainImageUrl(request.getMainImageUrl());
        product.setDescription(request.getDescription());
        product.setStatus(request.getStatus());
        product.setSort(request.getSort());
    }

    private Sku createSkuAndInventory(Long productId, AdminProductSkuRequest request) {
        Sku sku = new Sku();
        fillSku(sku, productId, request);
        sku.setDeleted(NOT_DELETED);
        skuMapper.insert(sku);

        Inventory inventory = new Inventory();
        inventory.setSkuId(sku.getId());
        inventory.setAvailableStock(request.getAvailableStock());
        inventory.setLockedStock(0);
        inventory.setVersion(INITIAL_VERSION);
        inventory.setDeleted(NOT_DELETED);
        inventoryMapper.insert(inventory);
        return sku;
    }

    private void updateSkuAndInventory(Long productId, AdminProductSkuRequest request) {
        Sku sku = skuMapper.selectOne(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getId, request.getId())
                .eq(Sku::getProductId, productId)
                .eq(Sku::getDeleted, NOT_DELETED));
        if (sku == null) {
            throw new BusinessException(404, "sku not found");
        }
        fillSku(sku, productId, request);
        skuMapper.updateById(sku);

        Inventory inventory = getInventoryOrNull(sku.getId());
        if (inventory == null) {
            inventory = new Inventory();
            inventory.setSkuId(sku.getId());
            inventory.setAvailableStock(request.getAvailableStock());
            inventory.setLockedStock(0);
            inventory.setVersion(INITIAL_VERSION);
            inventory.setDeleted(NOT_DELETED);
            inventoryMapper.insert(inventory);
            return;
        }
        inventory.setAvailableStock(request.getAvailableStock());
        inventoryMapper.updateById(inventory);
    }

    private void fillSku(Sku sku, Long productId, AdminProductSkuRequest request) {
        sku.setProductId(productId);
        sku.setSkuCode(request.getSkuCode());
        sku.setSkuName(request.getSkuName());
        sku.setSpecData(request.getSpecData());
        sku.setPrice(request.getPrice());
        sku.setOriginalPrice(request.getOriginalPrice());
        sku.setMainImageUrl(request.getMainImageUrl());
        sku.setStatus(request.getStatus());
    }

    private void softDeleteRemovedSkus(Long productId, Set<Long> retainedSkuIds) {
        List<Sku> skus = listSkus(productId);
        for (Sku sku : skus) {
            if (!retainedSkuIds.contains(sku.getId())) {
                sku.setDeleted(DELETED);
                skuMapper.updateById(sku);
                softDeleteInventory(sku.getId());
            }
        }
    }

    private void softDeleteInventory(Long skuId) {
        Inventory inventory = getInventoryOrNull(skuId);
        if (inventory == null) {
            return;
        }
        inventory.setDeleted(DELETED);
        inventoryMapper.updateById(inventory);
    }

    private void ensureCategoryExists(Long categoryId) {
        Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getId, categoryId)
                .eq(Category::getDeleted, NOT_DELETED));
        if (category == null) {
            throw new BusinessException(404, "category not found");
        }
    }

    private void ensureProductCodeUnique(String productCode, Long currentProductId) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getProductCode, productCode);
        if (currentProductId != null) {
            queryWrapper.ne(Product::getId, currentProductId);
        }
        if (productMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(400, "product code already exists");
        }
    }

    private void ensureSkuCodesUnique(List<AdminProductSkuRequest> skus, Long currentProductId) {
        Set<String> requestCodes = new HashSet<>();
        for (AdminProductSkuRequest skuRequest : skus) {
            if (!requestCodes.add(skuRequest.getSkuCode())) {
                throw new BusinessException(400, "sku code duplicated in request");
            }
            LambdaQueryWrapper<Sku> queryWrapper = new LambdaQueryWrapper<Sku>()
                    .eq(Sku::getSkuCode, skuRequest.getSkuCode());
            if (skuRequest.getId() != null) {
                queryWrapper.ne(Sku::getId, skuRequest.getId());
            }
            if (currentProductId != null) {
                queryWrapper.ne(Sku::getProductId, currentProductId);
            }
            if (skuMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException(400, "sku code already exists");
            }
        }
    }

    private Product getProductOrThrow(Long productId) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getDeleted, NOT_DELETED));
        if (product == null) {
            throw new BusinessException(404, "product not found");
        }
        return product;
    }

    private List<Sku> listSkus(Long productId) {
        return skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getProductId, productId)
                .eq(Sku::getDeleted, NOT_DELETED)
                .orderByDesc(Sku::getId));
    }

    private Inventory getInventoryOrNull(Long skuId) {
        return inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, skuId)
                .eq(Inventory::getDeleted, NOT_DELETED));
    }

    private AdminProductVO toProductVO(Product product, boolean includeSkus) {
        AdminProductVO productVO = new AdminProductVO();
        productVO.setId(product.getId());
        productVO.setCategoryId(product.getCategoryId());
        productVO.setProductCode(product.getProductCode());
        productVO.setName(product.getName());
        productVO.setSubtitle(product.getSubtitle());
        productVO.setMainImageUrl(product.getMainImageUrl());
        productVO.setDescription(product.getDescription());
        productVO.setStatus(product.getStatus());
        productVO.setSort(product.getSort());
        productVO.setCreateTime(product.getCreateTime());
        productVO.setUpdateTime(product.getUpdateTime());
        if (includeSkus) {
            productVO.setSkus(listSkus(product.getId()).stream()
                    .map(this::toSkuVO)
                    .toList());
        }
        return productVO;
    }

    private AdminProductSkuVO toSkuVO(Sku sku) {
        Product product = productMapper.selectById(sku.getProductId());
        Inventory inventory = getInventoryOrNull(sku.getId());
        AdminProductSkuVO skuVO = new AdminProductSkuVO();
        skuVO.setId(sku.getId());
        skuVO.setProductId(sku.getProductId());
        skuVO.setProductName(product == null ? null : product.getName());
        skuVO.setProductCode(product == null ? null : product.getProductCode());
        skuVO.setSkuCode(sku.getSkuCode());
        skuVO.setSkuName(sku.getSkuName());
        skuVO.setSpecData(sku.getSpecData());
        skuVO.setPrice(sku.getPrice());
        skuVO.setOriginalPrice(sku.getOriginalPrice());
        skuVO.setMainImageUrl(sku.getMainImageUrl());
        skuVO.setStatus(sku.getStatus());
        skuVO.setAvailableStock(inventory == null ? 0 : inventory.getAvailableStock());
        skuVO.setLockedStock(inventory == null ? 0 : inventory.getLockedStock());
        return skuVO;
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
}
