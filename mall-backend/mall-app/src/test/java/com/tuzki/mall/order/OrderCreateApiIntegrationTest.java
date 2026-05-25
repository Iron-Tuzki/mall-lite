package com.tuzki.mall.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.entity.OrderItem;
import com.tuzki.mall.order.mapper.OrderItemMapper;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.product.entity.Category;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.CategoryMapper;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.user.entity.Address;
import com.tuzki.mall.user.entity.User;
import com.tuzki.mall.user.mapper.AddressMapper;
import com.tuzki.mall.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderCreateApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Test
    void createOrderLocksStockAndCreatesOrderWithItemSnapshot() throws Exception {
        Long userId = insertUser();
        Long addressId = insertAddress(userId);
        Sku sku = insertProductAndSku();
        insertInventory(sku.getId(), 10, 0);

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content("""
                                {
                                  "requestId": "REQ-create-%d",
                                  "userId": %d,
                                  "addressId": %d,
                                  "skuId": %d,
                                  "quantity": 2,
                                  "remark": "please ship soon"
                                }
                                """.formatted(System.nanoTime(), userId, addressId, sku.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").isNumber())
                .andExpect(jsonPath("$.data.orderNo").isString())
                .andExpect(jsonPath("$.data.status").value(10))
                .andExpect(jsonPath("$.data.totalAmount").value(398.00))
                .andExpect(jsonPath("$.data.payAmount").value(398.00));

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getRemark, "please ship soon"));
        assertNotNull(order);
        assertEquals(new BigDecimal("398.00"), order.getTotalAmount());
        assertEquals("测试收货人", order.getReceiverName());
        assertEquals("广东省", order.getReceiverProvince());

        mockMvc.perform(get("/api/orders/{orderId}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(order.getId()))
                .andExpect(jsonPath("$.data.orderNo").value(order.getOrderNo()))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.status").value(10))
                .andExpect(jsonPath("$.data.totalAmount").value(398.00))
                .andExpect(jsonPath("$.data.receiverName").value("测试收货人"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].skuId").value(sku.getId()))
                .andExpect(jsonPath("$.data.items[0].productName").value("Order Test Product"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        OrderItem orderItem = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        assertNotNull(orderItem);
        assertEquals(sku.getId(), orderItem.getSkuId());
        assertEquals("Order Test Product", orderItem.getProductName());
        assertEquals("Order Test SKU", orderItem.getSkuName());
        assertEquals(new BigDecimal("199.00"), orderItem.getUnitPrice());
        assertEquals(2, orderItem.getQuantity());
        assertEquals(new BigDecimal("398.00"), orderItem.getTotalAmount());

        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, sku.getId()));
        assertEquals(8, inventory.getAvailableStock());
        assertEquals(2, inventory.getLockedStock());
        assertEquals(1, inventory.getVersion());
    }

    @Test
    void createOrderReturnsExistingOrderWhenRequestIdIsRepeated() throws Exception {
        Long userId = insertUser();
        Long addressId = insertAddress(userId);
        Sku sku = insertProductAndSku();
        insertInventory(sku.getId(), 10, 0);
        String requestId = "REQ-" + System.nanoTime();

        String requestBody = """
                {
                  "requestId": "%s",
                  "userId": %d,
                  "addressId": %d,
                  "skuId": %d,
                  "quantity": 2,
                  "remark": "idempotent order test"
                }
                """.formatted(requestId, userId, addressId, sku.getId());

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").isNumber())
                .andExpect(jsonPath("$.data.orderNo").isString());

        Order firstOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getRequestId, requestId));
        assertNotNull(firstOrder);

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(firstOrder.getId()))
                .andExpect(jsonPath("$.data.orderNo").value(firstOrder.getOrderNo()))
                .andExpect(jsonPath("$.data.status").value(10))
                .andExpect(jsonPath("$.data.totalAmount").value(398.00));

        assertEquals(1L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getRequestId, requestId)));
        assertEquals(1L, orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, firstOrder.getId())));

        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, sku.getId()));
        assertEquals(8, inventory.getAvailableStock());
        assertEquals(2, inventory.getLockedStock());
        assertEquals(1, inventory.getVersion());
    }

    @Test
    void createOrderRejectsInsufficientStockAndDoesNotCreateOrder() throws Exception {
        Long userId = insertUser();
        Long addressId = insertAddress(userId);
        Sku sku = insertProductAndSku();
        insertInventory(sku.getId(), 1, 0);

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content("""
                                {
                                  "requestId": "REQ-insufficient-%d",
                                  "userId": %d,
                                  "addressId": %d,
                                  "skuId": %d,
                                  "quantity": 2
                                }
                                """.formatted(System.nanoTime(), userId, addressId, sku.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("insufficient stock"));

        assertEquals(0L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)));
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, sku.getId()));
        assertEquals(1, inventory.getAvailableStock());
        assertEquals(0, inventory.getLockedStock());
    }

    @Test
    void cancelPendingOrderReleasesLockedStock() throws Exception {
        Long userId = insertUser();
        Long addressId = insertAddress(userId);
        Sku sku = insertProductAndSku();
        insertInventory(sku.getId(), 10, 0);

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content("""
                                {
                                  "requestId": "REQ-cancel-%d",
                                  "userId": %d,
                                  "addressId": %d,
                                  "skuId": %d,
                                  "quantity": 2,
                                  "remark": "cancel order test"
                                }
                                """.formatted(System.nanoTime(), userId, addressId, sku.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getRemark, "cancel order test"));
        assertNotNull(order);

        mockMvc.perform(post("/api/orders/{orderId}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Order cancelledOrder = orderMapper.selectById(order.getId());
        assertEquals(30, cancelledOrder.getStatus());
        assertNotNull(cancelledOrder.getCancelTime());

        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, sku.getId()));
        assertEquals(10, inventory.getAvailableStock());
        assertEquals(0, inventory.getLockedStock());
        assertEquals(2, inventory.getVersion());
    }

    @Test
    void cancelOrderRejectsAlreadyCancelledOrder() throws Exception {
        Long userId = insertUser();
        Long addressId = insertAddress(userId);
        Sku sku = insertProductAndSku();
        insertInventory(sku.getId(), 10, 0);

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content("""
                                {
                                  "requestId": "REQ-double-cancel-%d",
                                  "userId": %d,
                                  "addressId": %d,
                                  "skuId": %d,
                                  "quantity": 2,
                                  "remark": "double cancel order test"
                                }
                                """.formatted(System.nanoTime(), userId, addressId, sku.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getRemark, "double cancel order test"));
        assertNotNull(order);

        mockMvc.perform(post("/api/orders/{orderId}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("cancelled order cannot be cancelled"));
    }

    @Test
    void getOrderRejectsMissingOrder() throws Exception {
        mockMvc.perform(get("/api/orders/{orderId}", 999999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("order not found"));
    }

    private Long insertUser() {
        long suffix = System.nanoTime();
        User user = new User();
        user.setUsername("order_user_" + suffix);
        user.setPassword("encoded-password");
        user.setNickname("Order Test User");
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);
        return user.getId();
    }

    private Long insertAddress(Long userId) {
        Address address = new Address();
        address.setUserId(userId);
        address.setReceiverName("测试收货人");
        address.setReceiverPhone("13800000000");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetailAddress("科技园测试地址");
        address.setDefaultFlag(1);
        address.setDeleted(0);
        addressMapper.insert(address);
        return address.getId();
    }

    private Sku insertProductAndSku() {
        long suffix = System.nanoTime();

        Category category = new Category();
        category.setParentId(0L);
        category.setName("Order Category " + suffix);
        category.setLevel(1);
        category.setSort(1);
        category.setStatus(1);
        category.setDeleted(0);
        categoryMapper.insert(category);

        Product product = new Product();
        product.setCategoryId(category.getId());
        product.setProductCode("ORDER-P" + suffix);
        product.setName("Order Test Product");
        product.setMainImageUrl("https://example.com/order-product.png");
        product.setStatus(1);
        product.setSort(1);
        product.setDeleted(0);
        productMapper.insert(product);

        Sku sku = new Sku();
        sku.setProductId(product.getId());
        sku.setSkuCode("ORDER-S" + suffix);
        sku.setSkuName("Order Test SKU");
        sku.setSpecData("{\"color\":\"black\"}");
        sku.setPrice(new BigDecimal("199.00"));
        sku.setMainImageUrl("https://example.com/order-sku.png");
        sku.setStatus(1);
        sku.setDeleted(0);
        skuMapper.insert(sku);
        return sku;
    }

    private void insertInventory(Long skuId, Integer availableStock, Integer lockedStock) {
        Inventory inventory = new Inventory();
        inventory.setSkuId(skuId);
        inventory.setAvailableStock(availableStock);
        inventory.setLockedStock(lockedStock);
        inventory.setVersion(0);
        inventory.setDeleted(0);
        inventoryMapper.insert(inventory);
    }
}
