package com.tuzki.mall.user.service;

import com.tuzki.mall.user.dto.AddressRequest;
import com.tuzki.mall.user.vo.AddressVO;

import java.util.List;

/**
 * Address business service for managing user shipping addresses.
 */
public interface AddressService {

    /**
     * Creates a shipping address for the specified user.
     *
     * @param userId user id that owns the address
     * @param request address creation request
     * @return created address public information
     */
    AddressVO create(Long userId, AddressRequest request);

    /**
     * Lists all active shipping addresses owned by the specified user.
     *
     * @param userId user id that owns addresses
     * @return active address list
     */
    List<AddressVO> listByUserId(Long userId);

    /**
     * Gets one active shipping address by user id and address id.
     *
     * @param userId user id that owns the address
     * @param addressId address id
     * @return address public information
     */
    AddressVO getById(Long userId, Long addressId);

    /**
     * Updates one active shipping address by user id and address id.
     *
     * @param userId user id that owns the address
     * @param addressId address id
     * @param request address update request
     * @return updated address public information
     */
    AddressVO update(Long userId, Long addressId, AddressRequest request);

    /**
     * Logically deletes one active shipping address by user id and address id.
     *
     * @param userId user id that owns the address
     * @param addressId address id
     */
    void delete(Long userId, Long addressId);
}
