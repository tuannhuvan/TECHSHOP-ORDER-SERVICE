package com.fptaptech.techshop.order.order;

import com.fptaptech.techshop.order.common.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController implements ProductApi {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok(productService.findAll());
    }

    @Override
    public ResponseEntity<Product> getProductById(Long id) {
        Product product = productService.findById(id)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sản phẩm id=" + id));
        return ResponseEntity.ok(product);
    }
}
