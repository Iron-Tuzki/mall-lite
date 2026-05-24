package com.tuzki.mall.user;

import com.tuzki.mall.user.entity.Address;
import com.tuzki.mall.user.mapper.AddressMapper;
import com.tuzki.mall.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AddressApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Test
    void addressCrudKeepsDefaultAddressExclusiveAndUsesLogicalDelete() throws Exception {
        Long userId = registerUser("address_user_");

        // 新增一条地址
        mockMvc.perform(post("/api/users/{userId}/addresses", userId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "receiverName": "Alice",
                                  "receiverPhone": "13900001111",
                                  "province": "Zhejiang",
                                  "city": "Hangzhou",
                                  "district": "Xihu",
                                  "detailAddress": "No. 1 Road",
                                  "postalCode": "310000",
                                  "defaultFlag": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.receiverName").value("Alice"))
                .andExpect(jsonPath("$.data.defaultFlag").value(1));

        Long firstAddressId = findAddressId(userId, "Alice");

        mockMvc.perform(post("/api/users/{userId}/addresses", userId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "receiverName": "Bob",
                                  "receiverPhone": "13900002222",
                                  "province": "Shanghai",
                                  "city": "Shanghai",
                                  "district": "Pudong",
                                  "detailAddress": "No. 2 Road",
                                  "defaultFlag": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receiverName").value("Bob"))
                .andExpect(jsonPath("$.data.defaultFlag").value(1));

        Long secondAddressId = findAddressId(userId, "Bob");
        assertAddressDefaultFlag(firstAddressId, 0);
        assertAddressDefaultFlag(secondAddressId, 1);

        mockMvc.perform(get("/api/users/{userId}/addresses", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/users/{userId}/addresses/{addressId}", userId, secondAddressId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(secondAddressId))
                .andExpect(jsonPath("$.data.receiverName").value("Bob"));

        mockMvc.perform(put("/api/users/{userId}/addresses/{addressId}", userId, secondAddressId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "receiverName": "Bob Updated",
                                  "receiverPhone": "13900003333",
                                  "province": "Jiangsu",
                                  "city": "Nanjing",
                                  "district": "Gulou",
                                  "detailAddress": "No. 3 Road",
                                  "defaultFlag": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receiverName").value("Bob Updated"))
                .andExpect(jsonPath("$.data.defaultFlag").value(0));

        mockMvc.perform(delete("/api/users/{userId}/addresses/{addressId}", userId, secondAddressId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Address deletedAddress = addressMapper.selectById(secondAddressId);
        org.junit.jupiter.api.Assertions.assertEquals(1, deletedAddress.getDeleted());

        mockMvc.perform(get("/api/users/{userId}/addresses", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(firstAddressId));
    }

    @Test
    void addressOperationsRejectMissingUser() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/addresses", 999999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("user not found"));
    }

    @Test
    void addressOperationsRejectAddressOwnedByAnotherUser() throws Exception {
        Long ownerId = registerUser("address_owner_");
        Long anotherUserId = registerUser("address_another_");

        mockMvc.perform(post("/api/users/{userId}/addresses", ownerId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "receiverName": "Owner",
                                  "receiverPhone": "13900004444",
                                  "province": "Guangdong",
                                  "city": "Shenzhen",
                                  "district": "Nanshan",
                                  "detailAddress": "No. 4 Road",
                                  "defaultFlag": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Long addressId = findAddressId(ownerId, "Owner");

        mockMvc.perform(get("/api/users/{userId}/addresses/{addressId}", anotherUserId, addressId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("address not found"));
    }

    private Long registerUser(String usernamePrefix) throws Exception {
        String username = usernamePrefix + System.nanoTime();
        mockMvc.perform(post("/api/users/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password123",
                                  "nickname": "Address Test User"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        return userMapper.selectList(null).stream()
                .filter(user -> username.equals(user.getUsername()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private Long findAddressId(Long userId, String receiverName) {
        return addressMapper.selectList(null).stream()
                .filter(address -> userId.equals(address.getUserId()))
                .filter(address -> receiverName.equals(address.getReceiverName()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private void assertAddressDefaultFlag(Long addressId, int defaultFlag) {
        Address address = addressMapper.selectById(addressId);
        org.junit.jupiter.api.Assertions.assertEquals(defaultFlag, address.getDefaultFlag());
    }
}
