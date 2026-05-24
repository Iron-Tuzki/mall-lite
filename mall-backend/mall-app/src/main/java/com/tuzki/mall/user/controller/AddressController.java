package com.tuzki.mall.user.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.user.dto.AddressRequest;
import com.tuzki.mall.user.service.AddressService;
import com.tuzki.mall.user.vo.AddressVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing user shipping addresses.
 */
@RestController
@RequestMapping("/api/users/{userId}/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public Result<AddressVO> create(@PathVariable Long userId, @Valid @RequestBody AddressRequest request) {
        return Result.success(addressService.create(userId, request));
    }

    @GetMapping
    public Result<List<AddressVO>> list(@PathVariable Long userId) {
        return Result.success(addressService.listByUserId(userId));
    }

    @GetMapping("/{addressId}")
    public Result<AddressVO> getById(@PathVariable Long userId, @PathVariable Long addressId) {
        return Result.success(addressService.getById(userId, addressId));
    }

    @PutMapping("/{addressId}")
    public Result<AddressVO> update(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request
    ) {
        return Result.success(addressService.update(userId, addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public Result<Void> delete(@PathVariable Long userId, @PathVariable Long addressId) {
        addressService.delete(userId, addressId);
        return Result.success();
    }
}
