package com.example.project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
@ToString
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long productId;

    // Stable external ID used only for idempotent DummyJSON imports. Null for normal products.
    @Column(name = "dummyjson_id", unique = true)
    private Long dummyJsonId;

    @NotBlank
    @Size(min = 3, message = "Product name should be least 3-characters long")
    private String productName;

    private String image;

    @NotBlank
    @Size(min = 6, message = "Product description should be least 6-characters long")
    private String description;

    @NotNull
    @Min(1)
    private Integer quantity;

    private double price;

    private double discount;

    private double specialPrice;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private User user;

    @OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    private List<CartItems> products = new ArrayList<>();
}
