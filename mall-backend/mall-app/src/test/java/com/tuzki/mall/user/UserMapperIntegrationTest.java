package com.tuzki.mall.user;

import com.tuzki.mall.user.mapper.AddressMapper;
import com.tuzki.mall.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class UserMapperIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Test
    void userAndAddressMappersCanQueryTables() {
        assertNotNull(userMapper.selectCount(null));
        assertNotNull(addressMapper.selectCount(null));
    }
}
