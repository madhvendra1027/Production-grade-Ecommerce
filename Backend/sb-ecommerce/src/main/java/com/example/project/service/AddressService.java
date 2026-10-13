package com.example.project.service;

import com.example.project.model.User;
import com.example.project.payLoad.AddressDTO;

import java.util.List;

public interface AddressService {

    AddressDTO createAddress(AddressDTO addressDTO, User user);

    /** Addresses belonging to the currently authenticated user. */
    List<AddressDTO> getLoggedInUserAddresses();

    /** Every address in the system. Admin-only - must not be exposed to normal users. */
    List<AddressDTO> getAllAddresses();

    /** Throws AccessDeniedException unless the address belongs to the current user. */
    AddressDTO getAddressById(Long addressId);

    List<AddressDTO> getUserAddressList(User user);

    AddressDTO updateAddress(Long addressId, AddressDTO addressDTO);

    String deleteAddress(Long addressId);
}
