package com.shopping.engine.service;

import com.shopping.engine.controller.dto.ProductRequestDto;
import com.shopping.engine.domain.Product;
import com.shopping.engine.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findAll();
        }
        return productRepository.findByNameContainingIgnoreCase(keyword.trim());
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));
    }

    @Transactional
    public Product createProduct(ProductRequestDto request) {
        validate(request);
        Product product = new Product(request.name().trim(), request.price(), request.stockQuantity());
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long productId, ProductRequestDto request) {
        validate(request);
        Product product = getProduct(productId);
        product.update(request.name().trim(), request.price(), request.stockQuantity());
        return product;
    }

    @Transactional
    public void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Product not found with id: " + productId);
        }
        productRepository.deleteById(productId);
    }

    private void validate(ProductRequestDto request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (request.price() == null || request.price().signum() < 0) {
            throw new IllegalArgumentException("price must be greater than or equal to zero");
        }
        if (request.stockQuantity() < 0) {
            throw new IllegalArgumentException("stockQuantity must be greater than or equal to zero");
        }
    }
}
