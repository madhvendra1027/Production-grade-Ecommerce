package com.example.project.repositories;

import com.example.project.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepositories extends JpaRepository<Category, Long> {
    Category findByCategoryName(String categoryName);

    Category findByCategoryNameIgnoreCase(String categoryName);
}
