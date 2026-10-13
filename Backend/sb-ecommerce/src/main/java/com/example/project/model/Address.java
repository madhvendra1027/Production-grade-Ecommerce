package com.example.project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    @Size(min = 2, message = "State name must be least of 5 letters!")
    private String state;

    @NotBlank
    @Size(min = 2, message = "Country name should be atleast of 2 letters!")
    private String country;

    @NotBlank
    @Size(min = 2, message = "Pin code name must be least of 5 letters!")
    private String pincode;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Address(String streetName, String buildingName, String city, String state, String pincode) {
        this.streetName = streetName;
        this.buildingName = buildingName;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
    }
}
