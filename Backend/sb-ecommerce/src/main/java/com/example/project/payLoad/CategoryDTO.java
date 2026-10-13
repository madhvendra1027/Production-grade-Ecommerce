package com.example.project.payLoad;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

    @Schema(description = "Category ID")
    private Long categoryId;

    // Mirrors the Category entity so an invalid name is rejected with a 400 at the
    // controller rather than blowing up as a ConstraintViolationException 500 on flush.
    @NotBlank
    @Size(min = 5, message = "Category name should be atleast 5 letters long")
    @Schema(description = "Category name for the category you want to add!")
    private String categoryName;
}
