package com.ecommerce.project.services.impl;

import com.ecommerce.project.dto.AddressDTO;
import com.ecommerce.project.entities.Address;
import com.ecommerce.project.entities.User;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.services.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;


    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        log.debug("AddressServiceImpl.createAddress call started.");
        Address address = modelMapper.map(addressDTO, Address.class);

        List<Address> addresses = user.getAddresses();
        addresses.add(address);
        user.setAddresses(addresses);

        address.setUser(user);
        Address savedAddress = addressRepository.save(address);
        log.debug("AddressServiceImpl.createAddress call completed with: {}", savedAddress);
        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAddresses() {
        log.debug("AddressServiceImpl.getAddresses call started.");
        List<Address> addresses = addressRepository.findAll();
        List<AddressDTO> addressDTOS = addresses.stream()
                .map(
                        address ->
                                modelMapper.map(address, AddressDTO.class))
                .toList();
        log.debug("AddressServiceImpl.getAddresses call completed with: {}", addresses);
        return addressDTOS;
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        log.debug("AddressServiceImpl.getAddressById call started.");
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> {
                    log.debug("AddressService.getAddressById call failed with: {}", addressId);
                    return new ResourceNotFoundException("Address not found with id " + addressId);
                });
        AddressDTO addressDTO = modelMapper.map(address, AddressDTO.class);
        log.debug("AddressServiceImpl.getAddressById call completed with: {}", addressDTO);
        return addressDTO;
    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        log.debug("AddressServiceImpl.getUserAddresses call started.");
        List<Address> addresses = user.getAddresses();
        List<AddressDTO> addressDTOS = addresses.stream()
                .map(
                        address ->
                                modelMapper.map(address, AddressDTO.class))
                .toList();
        log.debug("AddressServiceImpl.getUserAddresses call completed with: {}", addresses);
        return addressDTOS;
    }

    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        log.debug("AddressServiceImpl.updateAddressById call started with addressId: {} and address: {}", addressId, addressDTO);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException("Address not found with id " + addressId));

        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setStreet(addressDTO.getStreet());
        address.setPincode(addressDTO.getPincode());
        address.setCountry(addressDTO.getCountry());
        address.setBuildingName(addressDTO.getBuildingName());

        Address updatedAddress = addressRepository.save(address);

        User user = address.getUser();
        user.getAddresses().removeIf(add -> add.getAddressId().equals(addressId));
        user.getAddresses().add(updatedAddress);
        userRepository.save(user);

        log.debug("AddressServiceImpl.updateAddressById call completed with: {}", updatedAddress);
        return modelMapper.map(updatedAddress, AddressDTO.class);
    }

    @Override
    public String deleteAddress(Long addressId) {
        log.debug("AddressServiceImpl.deleteAddressById call started with addressId: {}", addressId);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException("Address not found with id " + addressId));

        User user = address.getUser();
        user.getAddresses().removeIf(add -> add.getAddressId().equals(addressId));
        userRepository.save(user);

        addressRepository.delete(address);

        log.debug("AddressServiceImpl.deleteAddressById call completed with: {}", addressId);
        return "Address deleted successfully with addressId: " + addressId;
    }
}
