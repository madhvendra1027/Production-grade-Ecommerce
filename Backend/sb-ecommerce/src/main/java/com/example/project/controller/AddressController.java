package com.example.project.controller;


import com.example.project.model.User;
import com.example.project.payLoad.AddressDTO;
import com.example.project.service.AddressService;
import com.example.project.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AddressController {

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private AddressService addressService;

    @PostMapping("/addresses")
    public ResponseEntity<AddressDTO> createAddress(@Valid @RequestBody AddressDTO addressDTO){
        User user = authUtil.loggedInUser();
        AddressDTO savedAddress = addressService.createAddress(addressDTO, user);
        return new ResponseEntity<>(savedAddress, HttpStatus.CREATED);
    }

    /**
     * Returns only the caller's own addresses.
     *
     * This used to call addressService.getAddress(), which was a findAll() - every
     * address of every customer, handed to any logged-in user. The frontend only
     * ever wanted the caller's own list.
     */
    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDTO>> getAddress(){
        return new ResponseEntity<>(addressService.getLoggedInUserAddresses(), HttpStatus.OK);
    }

    @GetMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDTO> getAddressById(@PathVariable Long addressId){
        AddressDTO addressDTO = addressService.getAddressById(addressId);
        return new ResponseEntity<>(addressDTO, HttpStatus.OK);
    }

    @GetMapping("/users/addresses")
    public ResponseEntity<List<AddressDTO>> getUserAddressList(){
        User user = authUtil.loggedInUser();
        List<AddressDTO> addressDTOList = addressService.getUserAddressList(user);
        return new ResponseEntity<>(addressDTOList, HttpStatus.OK);
    }

    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable Long addressId,
                                                    @Valid @RequestBody AddressDTO addressDTO){
        AddressDTO updatedAddress = addressService.updateAddress(addressId, addressDTO);
        return new ResponseEntity<>(updatedAddress, HttpStatus.OK);
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long addressId){
        String status = addressService.deleteAddress(addressId);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    /** The unrestricted "all addresses" view, now behind /api/admin/** (ROLE_ADMIN). */
    @GetMapping("/admin/addresses")
    public ResponseEntity<List<AddressDTO>> getAllAddresses(){
        return new ResponseEntity<>(addressService.getAllAddresses(), HttpStatus.OK);
    }
}
