package com.pythonadmin.api;

import com.pythonadmin.domain.ProductCategoryEntity;
import com.pythonadmin.domain.ProductEntity;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.service.AdminService;
import com.pythonadmin.service.CurrentUserService;
import com.pythonadmin.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class ProductController {
    private final ProductService productService;
    private final CurrentUserService currentUserService;
    private final AdminService adminService;

    public ProductController(ProductService productService, CurrentUserService currentUserService, AdminService adminService) {
        this.productService = productService;
        this.currentUserService = currentUserService;
        this.adminService = adminService;
    }

    // --- Category Endpoints ---

    @GetMapping("/products/categories")
    public List<ProductCategoryEntity> listCategories() {
        return productService.listAllCategories();
    }

    @PostMapping("/products/categories")
    @Transactional
    public ProductCategoryEntity createCategory(@RequestBody ProductCategoryEntity category) {
        checkAdmin();
        return productService.saveCategory(category);
    }

    @PutMapping("/products/categories/{id}")
    @Transactional
    public ProductCategoryEntity updateCategory(@PathVariable("id") Integer id, @RequestBody ProductCategoryEntity category) {
        checkAdmin();
        category.setId(id);
        return productService.saveCategory(category);
    }

    @DeleteMapping("/products/categories/{id}")
    public void deleteCategory(@PathVariable("id") Integer id) {
        checkAdmin();
        productService.deleteCategory(id);
    }

    // --- Product Endpoints ---

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Boolean status,
            @RequestParam(name = "category_id", required = false) Integer categoryId
    ) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, pageSize));
        Page<ProductEntity> entities = productService.listProducts(keyword, status, categoryId, PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "sort", "id")));
        return entities.map(this::toResponse);
    }

    @GetMapping("/products/{id}")
    @Transactional(readOnly = true)
    public ProductResponse getProduct(@PathVariable("id") Integer id) {
        ProductEntity p = productService.getProduct(id);
        return toResponse(p);
    }

    @PostMapping("/products")
    @Transactional
    public ProductResponse createProduct(@RequestBody ProductRequest payload) {
        checkAdmin();
        ProductEntity p = new ProductEntity();
        copyProperties(payload, p);
        ProductEntity saved = productService.saveProduct(p, payload.categoryId());
        return toResponse(saved);
    }

    @PutMapping("/products/{id}")
    @Transactional
    public ProductResponse updateProduct(@PathVariable("id") Integer id, @RequestBody ProductRequest payload) {
        checkAdmin();
        ProductEntity p = productService.getProduct(id);
        copyProperties(payload, p);
        ProductEntity saved = productService.saveProduct(p, payload.categoryId());
        return toResponse(saved);
    }

    @DeleteMapping("/products/{id}")
    public void deleteProduct(@PathVariable("id") Integer id) {
        checkAdmin();
        productService.deleteProduct(id);
    }

    private ProductResponse toResponse(ProductEntity p) {
        CategoryResponse category = null;
        if (p.getCategory() != null) {
            try {
                // 触发加载以避免 LazyInitializationException，或者直接获取 ID 和 Name
                category = new CategoryResponse(p.getCategory().getId(), p.getCategory().getName());
            } catch (Exception e) {
                // 如果实在加载失败（由于 proxy 问题），则保持为 null 或只传 ID
            }
        }
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getThumbnail(),
                p.getSort(),
                p.getStatus(),
                p.getDescription(),
                p.getPoster(),
                p.getDetailImages(),
                p.getOfficialPrice(),
                p.getDiscount(),
                p.getActualPrice(),
                category,
                p.getCreatedAt() != null ? p.getCreatedAt().toString() : ""
        );
    }

    private void checkAdmin() {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
    }

    private void copyProperties(ProductRequest src, ProductEntity target) {
        target.setName(src.name());
        target.setThumbnail(src.thumbnail());
        target.setSort(src.sort() != null ? src.sort() : 0);
        target.setStatus(src.status() != null ? src.status() : true);
        target.setDescription(src.description());
        target.setPoster(src.poster());
        target.setDetailImages(src.detailImages());
        target.setOfficialPrice(src.officialPrice() != null ? src.officialPrice() : BigDecimal.ZERO);
        target.setDiscount(src.discount() != null ? src.discount() : BigDecimal.ZERO);
    }

    public record ProductRequest(
            String name,
            String thumbnail,
            Integer sort,
            Boolean status,
            String description,
            String poster,
            String detailImages,
            BigDecimal officialPrice,
            BigDecimal discount,
            Integer categoryId
    ) {}

    public record CategoryResponse(Integer id, String name) {}

    public record ProductResponse(
            Integer id,
            String name,
            String thumbnail,
            Integer sort,
            Boolean status,
            String description,
            String poster,
            String detailImages,
            BigDecimal officialPrice,
            BigDecimal discount,
            BigDecimal actualPrice,
            CategoryResponse category,
            String createdAt
    ) {}
}
