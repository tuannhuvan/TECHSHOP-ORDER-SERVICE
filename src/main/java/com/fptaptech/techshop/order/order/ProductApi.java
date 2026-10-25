package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Products", description = "Danh mục sản phẩm điện tử công nghệ TechShop")
public interface ProductApi {

    @Operation(summary = "Lấy danh sách tất cả sản phẩm")
    @GetMapping(value = "/api/products", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Product>> getProducts();

    @Operation(summary = "Lấy thông tin chi tiết sản phẩm theo ID")
    @GetMapping(value = "/api/products/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Product> getProductById(@PathVariable Long id);
}
