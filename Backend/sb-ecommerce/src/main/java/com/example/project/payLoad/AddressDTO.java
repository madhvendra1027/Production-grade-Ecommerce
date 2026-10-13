package com.example.project.payLoad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The `users` list was removed from this DTO. It was declared as
 * List&lt;User&gt; - the JPA entity - so any address response had the potential to
 * serialise whole user records, bcrypt password hash included. Nothing read it.
 *
 * Constraints mirror the Address entity so a bad payload fails validation at the
 * controller boundary with a 400, instead of reaching the persistence layer and
 * surfacing as a ConstraintViolationException 500 at flush time.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDTO {

        private Long addressId;

        @NotBlank
        @Size(min = 5, message = "Street name must be least of 5 letters!")
        private String streetName;

        @NotBlank
        @Size(min = 5, message = "Building name must be least of 5 letters!")
        private String buildingName;

        @NotBlank
        @Size(min = 4, message = "City name must be least of 4 letters!")
        private String city;

        @NotBlank
        @Size(min = 2, message = "State name must be least of 2 letters!")
        private String state;

        @NotBlank
        @Size(min = 2, message = "Country name should be atleast of 2 letters!")
        private String country;

        @NotBlank
        @Size(min = 2, message = "Pin code must be least of 2 letters!")
        private String pincode;
}
