package com.example.project.payLoad;

import com.example.project.model.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
       private Long userId;
       private String username;
       private String email;

       /**
        * Defence in depth: ModelMapper happily copies the bcrypt hash from User into
        * this field, so mark it write-only to guarantee it is never serialised into a
        * response even if some future mapping path forgets to null it out.
        */
       @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
       private String password;

       private Set<Role> roles = new HashSet<>();
       private AddressDTO address;
       private CartDTO cart;
}
