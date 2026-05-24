package com.tuzki.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.user.dto.AddressRequest;
import com.tuzki.mall.user.entity.Address;
import com.tuzki.mall.user.entity.User;
import com.tuzki.mall.user.mapper.AddressMapper;
import com.tuzki.mall.user.mapper.UserMapper;
import com.tuzki.mall.user.service.AddressService;
import com.tuzki.mall.user.vo.AddressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Default implementation of user shipping address management logic.
 */
@Service
public class AddressServiceImpl implements AddressService {

    private static final int NOT_DELETED = 0;

    private static final int DELETED = 1;

    private static final int DEFAULT_ADDRESS = 1;

    private static final int NON_DEFAULT_ADDRESS = 0;

    private final UserMapper userMapper;

    private final AddressMapper addressMapper;

    public AddressServiceImpl(UserMapper userMapper, AddressMapper addressMapper) {
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO create(Long userId, AddressRequest request) {
        ensureUserExists(userId);
        resetOtherDefaultAddressesIfNeeded(userId, request.getDefaultFlag());

        Address address = new Address();
        address.setUserId(userId);
        fillAddress(address, request);
        address.setDeleted(NOT_DELETED);
        addressMapper.insert(address);
        return toAddressVO(address);
    }

    @Override
    public List<AddressVO> listByUserId(Long userId) {
        ensureUserExists(userId);
        return addressMapper.selectList(activeAddressQuery(userId)
                        .orderByDesc(Address::getDefaultFlag)
                        .orderByDesc(Address::getId))
                .stream()
                .map(this::toAddressVO)
                .toList();
    }

    @Override
    public AddressVO getById(Long userId, Long addressId) {
        ensureUserExists(userId);
        return toAddressVO(getActiveAddress(userId, addressId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO update(Long userId, Long addressId, AddressRequest request) {
        ensureUserExists(userId);
        Address address = getActiveAddress(userId, addressId);
        resetOtherDefaultAddressesIfNeeded(userId, request.getDefaultFlag());
        fillAddress(address, request);
        addressMapper.updateById(address);
        return toAddressVO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long addressId) {
        ensureUserExists(userId);
        Address address = getActiveAddress(userId, addressId);
        address.setDeleted(DELETED);
        addressMapper.updateById(address);
    }

    private void ensureUserExists(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "user not found");
        }
    }

    private Address getActiveAddress(Long userId, Long addressId) {
        Address address = addressMapper.selectOne(activeAddressQuery(userId)
                .eq(Address::getId, addressId));
        if (address == null) {
            throw new BusinessException(404, "address not found");
        }
        return address;
    }

    private LambdaQueryWrapper<Address> activeAddressQuery(Long userId) {
        return new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getDeleted, NOT_DELETED);
    }

    private void resetOtherDefaultAddressesIfNeeded(Long userId, Integer defaultFlag) {
        if (!Integer.valueOf(DEFAULT_ADDRESS).equals(defaultFlag)) {
            return;
        }

        Address address = new Address();
        address.setDefaultFlag(NON_DEFAULT_ADDRESS);
        addressMapper.update(address, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getDeleted, NOT_DELETED)
                .eq(Address::getDefaultFlag, DEFAULT_ADDRESS));
    }

    private void fillAddress(Address address, AddressRequest request) {
        address.setReceiverName(request.getReceiverName().trim());
        address.setReceiverPhone(request.getReceiverPhone().trim());
        address.setProvince(request.getProvince().trim());
        address.setCity(request.getCity().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setDetailAddress(request.getDetailAddress().trim());
        address.setPostalCode(trimToNull(request.getPostalCode()));
        address.setDefaultFlag(request.getDefaultFlag());
    }

    private AddressVO toAddressVO(Address address) {
        AddressVO addressVO = new AddressVO();
        addressVO.setId(address.getId());
        addressVO.setUserId(address.getUserId());
        addressVO.setReceiverName(address.getReceiverName());
        addressVO.setReceiverPhone(address.getReceiverPhone());
        addressVO.setProvince(address.getProvince());
        addressVO.setCity(address.getCity());
        addressVO.setDistrict(address.getDistrict());
        addressVO.setDetailAddress(address.getDetailAddress());
        addressVO.setPostalCode(address.getPostalCode());
        addressVO.setDefaultFlag(address.getDefaultFlag());
        return addressVO;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
