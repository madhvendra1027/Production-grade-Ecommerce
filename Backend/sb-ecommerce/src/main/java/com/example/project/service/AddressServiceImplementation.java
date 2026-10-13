package com.example.project.service;

import com.example.project.globalExceptionHandler.ResourceNotFoundException;
import com.example.project.model.Address;
import com.example.project.model.User;
import com.example.project.payLoad.AddressDTO;
import com.example.project.repositories.AddressRepository;
import com.example.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImplementation implements AddressService {

    private final ModelMapper modelMapper;
    private final AddressRepository addressRepository;
    private final AuthUtil authUtil;

    public AddressServiceImplementation(ModelMapper modelMapper,
                                        AddressRepository addressRepository,
                                        AuthUtil authUtil) {
        this.modelMapper = modelMapper;
        this.addressRepository = addressRepository;
        this.authUtil = authUtil;
    }

    @Override
    @Transactional
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        Address address = modelMapper.map(addressDTO, Address.class);

        // Never trust an id supplied on a create. Without this, POSTing
        // {"addressId": 7, ...} turns save() into an update of address 7 - which may
        // belong to somebody else - instead of an insert.
        address.setId(null);
        address.setUser(user);

        Address savedAddress = addressRepository.save(address);
        return toDTO(savedAddress);
    }

    @Override
    public List<AddressDTO> getLoggedInUserAddresses() {
        // Was addressRepository.findAll(), which returned every address belonging to
        // every user to any authenticated caller.
        Long userId = authUtil.loggedInUser().getUserId();
        return addressRepository.findByUser_UserId(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<AddressDTO> getAllAddresses() {
        return addressRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public AddressDTO getAddressById(Long addressId) {
        return toDTO(findOwnedAddress(addressId));
    }

    @Override
    public List<AddressDTO> getUserAddressList(User user) {
        return addressRepository.findByUser_UserId(user.getUserId()).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address addressFromDatabase = findOwnedAddress(addressId);

        addressFromDatabase.setCity(addressDTO.getCity());
        addressFromDatabase.setPincode(addressDTO.getPincode());
        addressFromDatabase.setState(addressDTO.getState());
        addressFromDatabase.setCountry(addressDTO.getCountry());
        addressFromDatabase.setStreetName(addressDTO.getStreetName());
        addressFromDatabase.setBuildingName(addressDTO.getBuildingName());

        // The old version also reached through to address.getUser(), removed the
        // address from that user's collection and re-added it, then saved the user.
        // That was pointless churn on an owning side that does not need it - and
        // because the user came from the row rather than the session, it meant a
        // caller could rewrite any other user's address.
        Address updatedAddress = addressRepository.save(addressFromDatabase);
        return toDTO(updatedAddress);
    }

    @Override
    @Transactional
    public String deleteAddress(Long addressId) {
        Address addressFromDatabase = findOwnedAddress(addressId);

//        User owner = addressFromDatabase.getUser();
//        if (owner != null) {
//            // Keep the in-memory graph consistent; User.addresses has
//            // orphanRemoval = true so this alone would delete the row.
//            owner.getAddresses().removeIf(address -> addressId.equals(address.getId()));
//        }

        // Explicit delete so the row is removed regardless of cascade configuration.
        addressRepository.delete(addressFromDatabase);

        return "Address deleted successfully with addressId : " + addressId;
    }

    /**
     * Loads an address and verifies it belongs to the caller.
     *
     * Returns 404 when it does not exist and 403 when it belongs to someone else.
     * Every mutating and single-read path goes through here; previously
     * getAddressById, updateAddress and deleteAddress all took the id straight from
     * the URL and never compared it to the session, so any logged-in user could
     * read, edit or delete any address by guessing a number.
     */
    private Address findOwnedAddress(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        Long callerId = authUtil.loggedInUser().getUserId();
        User owner = address.getUser();

        if (owner == null || !owner.getUserId().equals(callerId)) {
            throw new AccessDeniedException("You do not have access to this address.");
        }

        return address;
    }

    /**
     * Address.id does not reliably map onto AddressDTO.addressId through
     * ModelMapper's name matching, and the frontend keys every address off
     * addressId, so set it explicitly rather than depending on that behaviour.
     */
    private AddressDTO toDTO(Address address) {
        AddressDTO dto = modelMapper.map(address, AddressDTO.class);
        dto.setAddressId(address.getId());
        return dto;
    }
}
