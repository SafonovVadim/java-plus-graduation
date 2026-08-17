package ru.practicum.categories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);

    @Modifying
    @Query(value = "DELETE FROM categories c WHERE c.id = :catId AND NOT EXISTS " +
            "(SELECT 1 FROM events e WHERE e.category_id = :catId)", nativeQuery = true)
    int deleteCategoryIfNotUsed(@Param("catId") Long catId);
}
