
package com.recallradar.recall;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecallRepository
        extends JpaRepository<Recall, Long> {

    // ==========================================
    // CHECK DUPLICATE SOURCE RECORD
    // ==========================================

    boolean existsBySourceRecordId(String sourceRecordId);

    // ==========================================
    // COUNT RECALLS BY STATUS
    // ==========================================

    long countByStatus(String status);

    // ==========================================
    // FIND RECALL BY ID WITH PRODUCT
    // Fixes LazyInitializationException
    // ==========================================

    @Query("""
        SELECT r
        FROM Recall r
        LEFT JOIN FETCH r.product
        WHERE r.id = :id
        """)
    Optional<Recall> findByIdWithProduct(
            @Param("id") Long id
    );

    // ==========================================
    // SEARCH RECALLS
    // Searches recall and product information
    // ==========================================

    @EntityGraph(attributePaths = "product")
    @Query("""
        SELECT r
        FROM Recall r
        LEFT JOIN r.product p
        WHERE
            (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(r.title) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(r.description) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(r.reason) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(p.brand) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(p.category) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(p.modelNumber) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(p.batchNumber) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
            )
        AND (:severity IS NULL OR :severity = '' OR r.severity = :severity)
        AND (:status IS NULL OR :status = '' OR r.status = :status)
        ORDER BY r.recallDate DESC, r.id DESC
        """)
    Page<Recall> searchRecalls(
            @Param("keyword") String keyword,
            @Param("severity") String severity,
            @Param("status") String status,
            Pageable pageable
    );

    // ==========================================
    // FIND RELEVANT RECALLS
    // Loads Product together with Recall
    // ==========================================

    @EntityGraph(attributePaths = "product")
    @Query("""
        SELECT r
        FROM Recall r
        LEFT JOIN r.product p
        WHERE
            (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(r.title) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(r.description) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(r.reason) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(p.brand) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(p.category) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(p.modelNumber) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
                OR LOWER(p.batchNumber) LIKE CONCAT('%', LOWER(CAST(:keyword AS string)), '%')
            )
        AND (:status IS NULL OR :status = '' OR r.status = :status)
        ORDER BY r.recallDate DESC, r.id DESC
        """)
    Page<Recall> findRelevantRecalls(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    // ==========================================
    // FIND RECALLS BY EXACT / SUBSTRING MODEL NUMBER
    // ==========================================

    @EntityGraph(attributePaths = "product")
    @Query("""
        SELECT r
        FROM Recall r
        JOIN r.product p
        WHERE LOWER(TRIM(p.modelNumber)) = LOWER(TRIM(:modelNumber))
           OR LOWER(p.modelNumber) LIKE LOWER(CONCAT('%', :modelNumber, '%'))
        ORDER BY r.recallDate DESC, r.id DESC
        """)
    List<Recall> findByProductModelNumber(
            @Param("modelNumber") String modelNumber
    );

    // ==========================================
    // FIND RECALLS BY EXACT / SUBSTRING BATCH NUMBER
    // ==========================================

    @EntityGraph(attributePaths = "product")
    @Query("""
        SELECT r
        FROM Recall r
        JOIN r.product p
        WHERE LOWER(TRIM(p.batchNumber)) = LOWER(TRIM(:batchNumber))
           OR LOWER(p.batchNumber) LIKE LOWER(CONCAT('%', :batchNumber, '%'))
        ORDER BY r.recallDate DESC, r.id DESC
        """)
    List<Recall> findByProductBatchNumber(
            @Param("batchNumber") String batchNumber
    );

    // ==========================================
    // FIND RECALLS BY MODEL AND BATCH NUMBER
    // ==========================================

    @EntityGraph(attributePaths = "product")
    @Query("""
        SELECT r
        FROM Recall r
        JOIN r.product p
        WHERE (LOWER(TRIM(p.modelNumber)) = LOWER(TRIM(:modelNumber))
               OR LOWER(p.modelNumber) LIKE LOWER(CONCAT('%', :modelNumber, '%')))
          AND (LOWER(TRIM(p.batchNumber)) = LOWER(TRIM(:batchNumber))
               OR LOWER(p.batchNumber) LIKE LOWER(CONCAT('%', :batchNumber, '%')))
        ORDER BY r.recallDate DESC, r.id DESC
        """)
    List<Recall> findByProductModelAndBatch(
            @Param("modelNumber") String modelNumber,
            @Param("batchNumber") String batchNumber
    );

    // ==========================================
    // ANALYTICS AGGREGATION QUERIES
    // ==========================================

    @Query("""
        SELECT COALESCE(r.severity, 'UNKNOWN'), COUNT(r)
        FROM Recall r
        GROUP BY r.severity
        ORDER BY COUNT(r) DESC
        """)
    List<Object[]> countRecallsBySeverity();

    @Query("""
        SELECT COALESCE(p.category, 'Uncategorized'), COUNT(r)
        FROM Recall r
        LEFT JOIN r.product p
        GROUP BY p.category
        ORDER BY COUNT(r) DESC
        """)
    List<Object[]> countRecallsByCategory();

    @Query("""
        SELECT COALESCE(p.brand, 'Unknown Brand'), COUNT(r)
        FROM Recall r
        LEFT JOIN r.product p
        GROUP BY p.brand
        ORDER BY COUNT(r) DESC
        """)
    List<Object[]> countRecallsByBrand();

    @EntityGraph(attributePaths = "product")
    @Query("""
        SELECT r
        FROM Recall r
        JOIN r.product p
        WHERE LOWER(TRIM(p.brand)) = LOWER(TRIM(:brand))
        ORDER BY r.recallDate DESC, r.id DESC
        """)
    List<Recall> findByProductBrand(
            @Param("brand") String brand
    );

    @EntityGraph(attributePaths = "product")
    List<Recall> findByProductId(Long productId);

    @EntityGraph(attributePaths = "product")
    @Query("""
        SELECT r FROM Recall r
        LEFT JOIN r.product p
        WHERE (p IS NOT NULL AND (
                LOWER(TRIM(p.modelNumber)) = LOWER(TRIM(:modelNumber))
                OR LOWER(p.modelNumber) LIKE LOWER(CONCAT('%', :modelNumber, '%'))
                OR REPLACE(REPLACE(LOWER(p.modelNumber), '-', ''), ' ', '') = :normalizedModel
              ))
           OR (p IS NULL AND (
                LOWER(r.title) LIKE LOWER(CONCAT('%', :modelNumber, '%'))
                OR LOWER(r.description) LIKE LOWER(CONCAT('%', :modelNumber, '%'))
                OR LOWER(r.reason) LIKE LOWER(CONCAT('%', :modelNumber, '%'))
              ))
        ORDER BY r.recallDate DESC, r.id DESC
        """)
    List<Recall> findByModelAcrossLinkedAndUnlinked(
            @Param("modelNumber") String modelNumber,
            @Param("normalizedModel") String normalizedModel
    );

    @EntityGraph(attributePaths = "product")
    @Query("""
        SELECT r FROM Recall r
        LEFT JOIN r.product p
        WHERE (p IS NOT NULL AND (
                LOWER(TRIM(p.batchNumber)) = LOWER(TRIM(:batchNumber))
                OR LOWER(p.batchNumber) LIKE LOWER(CONCAT('%', :batchNumber, '%'))
                OR REPLACE(REPLACE(LOWER(p.batchNumber), '-', ''), ' ', '') = :normalizedBatch
              ))
           OR (p IS NULL AND (
                LOWER(r.title) LIKE LOWER(CONCAT('%', :batchNumber, '%'))
                OR LOWER(r.description) LIKE LOWER(CONCAT('%', :batchNumber, '%'))
              ))
        ORDER BY r.recallDate DESC, r.id DESC
        """)
    List<Recall> findByBatchAcrossLinkedAndUnlinked(
            @Param("batchNumber") String batchNumber,
            @Param("normalizedBatch") String normalizedBatch
    );

    @EntityGraph(attributePaths = "product")
    @Query("""
        SELECT r FROM Recall r
        LEFT JOIN r.product p
        WHERE (p IS NOT NULL AND LOWER(TRIM(p.brand)) = LOWER(TRIM(:brand)))
           OR (p IS NULL AND (
                LOWER(r.title) LIKE LOWER(CONCAT('%', :brand, '%'))
                OR LOWER(r.description) LIKE LOWER(CONCAT('%', :brand, '%'))
              ))
        ORDER BY r.recallDate DESC, r.id DESC
        """)
    List<Recall> findByBrandAcrossLinkedAndUnlinked(@Param("brand") String brand);

    @EntityGraph(attributePaths = "product")
    @Query("""
        SELECT r FROM Recall r
        LEFT JOIN r.product p
        WHERE UPPER(r.severity) = UPPER(:severity)
        ORDER BY r.recallDate DESC, r.id DESC
        """)
    List<Recall> findBySeverityIgnoreCase(@Param("severity") String severity);

    @EntityGraph(attributePaths = "product")
    @Query("""
        SELECT r FROM Recall r
        LEFT JOIN r.product p
        WHERE UPPER(r.status) = UPPER(:status)
        ORDER BY r.recallDate DESC, r.id DESC
        """)
    List<Recall> findByStatusIgnoreCase(@Param("status") String status);
}
