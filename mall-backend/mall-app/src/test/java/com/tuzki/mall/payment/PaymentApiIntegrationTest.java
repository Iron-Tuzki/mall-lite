package com.tuzki.mall.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.payment.entity.Payment;
import com.tuzki.mall.payment.mapper.PaymentMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentApiIntegrationTest {

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
    private PaymentMapper paymentMapper;

    @Test
    void payPendingOrderCreatesSuccessfulPaymentAndDeductsLockedStock() throws Exception {
        Long userId = insertUser();
        Long addressId = insertAddress(userId);
        Sku sku = insertProductAndSku();
        insertInventory(sku.getId(), 10, 0);

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content("""
                                {
                                  "userId": %d,
                                  "addressId": %d,
                                  "skuId": %d,
                                  "quantity": 2,
                                  "remark": "payment order test"
                                }
                                """.formatted(userId, addressId, sku.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getRemark, "payment order test"));
        assertNotNull(order);

        mockMvc.perform(post("/api/orders/{orderId}/pay", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentNo").isString())
                .andExpect(jsonPath("$.data.orderId").value(order.getId()))
                .andExpect(jsonPath("$.data.orderStatus").value(20))
                .andExpect(jsonPath("$.data.paymentStatus").value(20))
                .andExpect(jsonPath("$.data.payAmount").value(398.00));

        Order paidOrder = orderMapper.selectById(order.getId());
        assertEquals(20, paidOrder.getStatus());
        assertNotNull(paidOrder.getPayTime());

        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, order.getId()));
        assertNotNull(payment);
        assertEquals(order.getOrderNo(), payment.getOrderNo());
        assertEquals(userId, payment.getUserId());
        assertEquals(new BigDecimal("398.00"), payment.getPayAmount());
        assertEquals(20, payment.getStatus());
        assertNotNull(payment.getPayTime());

        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, sku.getId()));
        assertEquals(8, inventory.getAvailableStock());
        assertEquals(0, inventory.getLockedStock());
        assertEquals(2, inventory.getVersion());
    }

    @Test
    void payOrderRejectsAlreadyPaidOrder() throws Exception {
        Long userId = insertUser();
        Long addressId = insertAddress(userId);
        Sku sku = insertProductAndSku();
        insertInventory(sku.getId(), 10, 0);

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content("""
                                {
                                  "userId": %d,
                                  "addressId": %d,
                                  "skuId": %d,
                                  "quantity": 1,
                                  "remark": "double payment order test"
                                }
                                """.formatted(userId, addressId, sku.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getRemark, "double payment order test"));
        assertNotNull(order);

        mockMvc.perform(post("/api/orders/{orderId}/pay", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/orders/{orderId}/pay", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("paid order cannot be paid"));
    }

    private Long insertUser() {
        long suffix = System.nanoTime();
        User user = new User();
        user.setUsername("payment_user_" + suffix);
        user.setPassword("encoded-password");
        user.setNickname("Payment Test User");
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);
        return user.getId();
    }

    private Long insertAddress(Long userId) {
        Address address = new Address();
        address.setUserId(userId);
        address.setReceiverName("Payment Receiver");
        address.setReceiverPhone("13800000000");
        address.setProvince("Guangdong");
        address.setCity("Shenzhen");
        address.setDistrict("Nanshan");
        address.setDetailAddress("Payment Test Address");
        address.setDefaultFlag(1);
        address.setDeleted(0);
        addressMapper.insert(address);
        return address.getId();
    }

    private Sku insertProductAndSku() {
        long suffix = System.nanoTime();

        Category category = new Category();
        category.setParentId(0L);
        category.setName("Payment Category " + suffix);
        category.setLevel(1);
        category.setSort(1);
        category.setStatus(1);
        category.setDeleted(0);
        categoryMapper.insert(category);

        Product product = new Product();
        product.setCategoryId(category.getId());
        product.setProductCode("PAY-P" + suffix);
        product.setName("Payment Test Product");
        product.setMainImageUrl("https://example.com/payment-product.png");
        product.setStatus(1);
        product.setSort(1);
        product.setDeleted(0);
        productMapper.insert(product);

        Sku sku = new Sku();
        sku.setProductId(product.getId());
        sku.setSkuCode("PAY-S" + suffix);
        sku.setSkuName("Payment Test SKU");
        sku.setSpecData("{\"color\":\"black\"}");
        sku.setPrice(new BigDecimal("199.00"));
        sku.setMainImageUrl("https://example.com/payment-sku.png");
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
