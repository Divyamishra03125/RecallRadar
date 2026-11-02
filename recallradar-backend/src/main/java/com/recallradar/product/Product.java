

package com.recallradar.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "product")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 150)
    private String brand;

    @Column(length = 100)
    private String category;

    @Column(name = "model_number", length = 100)
    private String modelNumber;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Product() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
