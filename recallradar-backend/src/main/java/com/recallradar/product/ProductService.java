package com.recallradar.product;

import com.recallradar.product.dto.ProductRequest;
import com.recallradar.product.dto.ProductResponse;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final ModelMapper modelMapper;
    private final com.recallradar.recall.RecallRepository recallRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public ProductService(
            ProductRepository repository,
            ModelMapper modelMapper,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.recallradar.recall.RecallRepository recallRepository) {
        this.repository = repository;
        this.modelMapper = modelMapper;
        this.recallRepository = recallRepository;
    }

    // Convert Product entity to ProductResponse record
    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getCategory(),
                product.getModelNumber(),
                product.getBatchNumber(),
                product.getCreatedAt()
        );
    }

    // Map request fields to the Product entity
    private void mapRequestToProduct(
            ProductRequest request,
            Product product) {

        // Explicit mapping supports Java record request DTOs
        product.setName(request.name().trim());
        product.setBrand(request.brand());
        product.setCategory(request.category());
        product.setModelNumber(request.modelNumber());
        product.setBatchNumber(request.batchNumber());
    }

    // Create product
    @Transactional
    public ProductResponse create(ProductRequest request) {

        Product product = new Product();

        mapRequestToProduct(request, product);

        Product savedProduct = repository.save(product);

        return toResponse(savedProduct);
    }

    // Get product by ID
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {

        Product product = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with ID: " + id));

        return toResponse(product);
    }

    // Get all products with pagination
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(int page, int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("id").descending());

        return repository.findAll(pageable)
                .map(this::toResponse);
    }

    // Update product
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {

        Product product = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with ID: " + id));

        mapRequestToProduct(request, product);

        Product updatedProduct = repository.save(product);

        return toResponse(updatedProduct);
    }

    // Delete product (safely unlinks any associated recalls so safety history is preserved)
    @Transactional
    public void delete(Long id) {

        Product product = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with ID: " + id));

        if (recallRepository != null) {
            java.util.List<com.recallradar.recall.Recall> linkedRecalls = recallRepository.findByProductId(id);
            for (com.recallradar.recall.Recall r : linkedRecalls) {
                r.setProduct(null);
                r.setAssociationStatus("UNLINKED");
                recallRepository.save(r);
            }
        }

        repository.delete(product);
    }

    // Count total registered products
    @Transactional(readOnly = true)
    public long countProducts() {
        return repository.count();
    }

    // Search products by keyword across name, brand, model, batch, category
    @Transactional(readOnly = true)
    public java.util.List<Product> searchProducts(String keyword) {
        if (!org.springframework.util.StringUtils.hasText(keyword)) {
            return repository.findAll();
        }
        return repository.searchProducts(keyword.trim());
    }

    // Find products by exact or partial brand
    @Transactional(readOnly = true)
    public java.util.List<Product> findByBrand(String brand) {
        if (!org.springframework.util.StringUtils.hasText(brand)) {
            return java.util.List.of();
        }
        return repository.findByBrandIgnoreCase(brand.trim());
    }

    // Find products by model number
    @Transactional(readOnly = true)
    public java.util.List<Product> findByModelNumber(String modelNumber) {
        if (!org.springframework.util.StringUtils.hasText(modelNumber)) {
            return java.util.List.of();
        }
        return repository.findByModelNumberIgnoreCase(modelNumber.trim());
    }

    // Find products by batch number
    @Transactional(readOnly = true)
    public java.util.List<Product> findByBatchNumber(String batchNumber) {
        if (!org.springframework.util.StringUtils.hasText(batchNumber)) {
            return java.util.List.of();
        }
        return repository.findByBatchNumberIgnoreCase(batchNumber.trim());
    }
}
