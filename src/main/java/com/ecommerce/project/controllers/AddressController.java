package com.ecommerce.project.controllers;

import com.ecommerce.project.dto.AddressDTO;
import com.ecommerce.project.entities.User;
import com.ecommerce.project.services.AddressService;
import com.ecommerce.project.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AddressController {

    private final AddressService addressService;
    private final AuthUtil authUtil;

    @PostMapping("/addresses")
    public ResponseEntity<AddressDTO> createAddress(@Valid @RequestBody AddressDTO addressDTO) {
        log.debug("AddressController.createAddress call started with: {}", addressDTO);
        User user = authUtil.loggedInUser();
        AddressDTO savedAddress = addressService.createAddress(addressDTO, user);
        log.debug("AddressController.createAddress call completed with: {}", savedAddress);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedAddress);
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDTO>> getAddresses() {
        log.debug("AddressController.getAddresses call started with...");
        List<AddressDTO> addressList = addressService.getAddresses();
        log.debug("AddressController.getAddresses call completed with: {}", addressList);
        return ResponseEntity.status(HttpStatus.OK)
                .body(addressList);
    }

    @GetMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDTO> getAddressById(@PathVariable Long addressId) {
        log.debug("AddressController.getAddressById call started with...");
        AddressDTO address = addressService.getAddressById(addressId);
        log.debug("AddressController.getAddressById call completed with: {}", address);
        return ResponseEntity.status(HttpStatus.OK)
                .body(address);
    }

    @GetMapping("/addresses/user")
    public ResponseEntity<List<AddressDTO>> getUserAddresses() {
        log.debug("AddressController.getAddressByUser call started with...");
        User user = authUtil.loggedInUser();
        List<AddressDTO> addressList = addressService.getUserAddresses(user);
        log.debug("AddressController.getAddressByUser call completed with: {}", addressList);
        return ResponseEntity.status(HttpStatus.OK)
                .body(addressList);
    }

    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable Long addressId, @RequestBody AddressDTO addressDTO) {
        log.debug("AddressController.updateAddress call started with...");
        AddressDTO address = addressService.updateAddress(addressId, addressDTO);
        log.debug("AddressController.updateAddress call completed with: {}", address);
        return ResponseEntity.status(HttpStatus.OK)
                .body(address);
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long addressId) {
        log.debug("AddressController.deleteAddress call started with...");
        String status = addressService.deleteAddress(addressId);
        log.debug("AddressController.deleteAddress call completed with: {}", status);
        return ResponseEntity.status(HttpStatus.OK)
                .body(status);
    }

}
