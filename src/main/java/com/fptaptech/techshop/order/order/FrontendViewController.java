package com.fptaptech.techshop.order.order;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendViewController {

    private final ProductService productService;

    public FrontendViewController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping({"/", "/dashboard"})
    public String index(Model model) {
        model.addAttribute("appName", "TechShop Order Service");
        model.addAttribute("products", productService.findAll());
        return "index";
    }
}
