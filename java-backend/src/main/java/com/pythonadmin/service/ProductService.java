package com.pythonadmin.service;

import com.pythonadmin.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepo;
    private final ProductCategoryRepository categoryRepo;

    public ProductService(ProductRepository productRepo, ProductCategoryRepository categoryRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
    }

    // --- Category Methods ---

    public List<ProductCategoryEntity> listAllCategories() {
        return categoryRepo.findAll();
    }

    @Transactional
    public ProductCategoryEntity saveCategory(ProductCategoryEntity category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "类型名称不能为空");
        }
        if (category.getCode() == null || category.getCode().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "类型编码不能为空");
        }
        
        categoryRepo.findByCode(category.getCode()).ifPresent(existing -> {
            if (category.getId() == null || !existing.getId().equals(category.getId())) {
                throw new ResponseStatusException(BAD_REQUEST, "类型编码已存在");
            }
        });

        return categoryRepo.save(category);
    }

    public ProductCategoryEntity getCategory(Integer id) {
        return categoryRepo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "产品类型不存在"));
    }

    @Transactional
    public void deleteCategory(Integer id) {
        categoryRepo.deleteById(id);
    }

    // --- Product Methods ---

    public Page<ProductEntity> listProducts(String name, Boolean status, Integer categoryId, Pageable pageable) {
        // 如果有搜索关键词，使用关键词搜索并应用状态和分类过滤
        if (name != null && !name.isBlank()) {
            return productRepo.findByNameContainingIgnoreCaseAndFilters(name, status, categoryId, pageable);
        }
        // 否则使用综合过滤
        return productRepo.findByFilters(status, categoryId, pageable);
    }

    public Page<ProductEntity> listActiveProducts(Pageable pageable) {
        return productRepo.findByStatusTrue(pageable);
    }

    @Transactional
    public ProductEntity saveProduct(ProductEntity product, Integer categoryId) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "产品名称不能为空");
        }
        
        if (categoryId != null) {
            ProductCategoryEntity category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "所选产品类型不存在"));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        // 实际价格根据官方价格和折扣计算 (实际价格 = 官方价格 - 折扣)
        if (product.getOfficialPrice() != null && product.getDiscount() != null) {
            product.setActualPrice(product.getOfficialPrice().subtract(product.getDiscount()));
        }

        return productRepo.save(product);
    }

    public ProductEntity getProduct(Integer id) {
        return productRepo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "产品不存在"));
    }

    @Transactional
    public void deleteProduct(Integer id) {
        productRepo.deleteById(id);
    }
}
