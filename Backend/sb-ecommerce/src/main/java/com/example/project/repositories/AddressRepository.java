package com.example.project.repositories;

import com.example.project.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /** Scopes address lookups to a single owner so callers cannot read other users' addresses. */
    List<Address> findByUser_UserId(Long userId);
}
