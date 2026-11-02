package com.recallradar.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
        SELECT p FROM Product p
        WHERE (:keyword IS NULL OR :keyword = '' OR
               LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%') OR
               LOWER(p.brand) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%') OR
               LOWER(p.category) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%') OR
               LOWER(p.modelNumber) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%') OR
               LOWER(p.batchNumber) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%'))
        ORDER BY p.id ASC
        """)
    List<Product> searchProducts(@Param("keyword") String keyword);

    List<Product> findByBrandIgnoreCase(String brand);

    List<Product> findByModelNumberIgnoreCase(String modelNumber);

    List<Product> findByBatchNumberIgnoreCase(String batchNumber);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByNameIgnoreCase(String name);

    List<Product> findByBrandIgnoreCaseAndModelNumberIgnoreCase(String brand, String modelNumber);

    @Query("""
        SELECT p FROM Product p
        WHERE REPLACE(REPLACE(LOWER(p.modelNumber), '-', ''), ' ', '') = :normalizedModel
        """)
    List<Product> findByNormalizedModelNumber(@Param("normalizedModel") String normalizedModel);

    @Query("""
        SELECT p FROM Product p
        WHERE REPLACE(REPLACE(LOWER(p.batchNumber), '-', ''), ' ', '') = :normalizedBatch
        """)
    List<Product> findByNormalizedBatchNumber(@Param("normalizedBatch") String normalizedBatch);

    @Query("""
        SELECT p FROM Product p
        WHERE LOWER(p.brand) = LOWER(:brand)
          AND (REPLACE(REPLACE(LOWER(p.modelNumber), '-', ''), ' ', '') = :normalizedModel
               OR LOWER(p.modelNumber) LIKE LOWER(CONCAT('%', :modelNumber, '%')))
        """)
    List<Product> findByBrandAndModelFuzzy(
            @Param("brand") String brand,
            @Param("modelNumber") String modelNumber,
            @Param("normalizedModel") String normalizedModel
    );
}
