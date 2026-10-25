package com.fptaptech.techshop.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${app.api.version:1.0.0}") String version,
            @Value("${app.api.server-url:http://localhost:8080}") String serverUrl) {

        return new OpenAPI()
                .info(new Info()
                        .title("TechShop Order Service API")
                        .version(version)
                        .description("Microservice quản lý toàn bộ quy trình đặt hàng, thanh toán, vận chuyển và báo cáo doanh thu cho TechShop.")
                        .contact(new Contact().name("TechShop Architecture Team").email("architecture@techshop.vn"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(new Server().url(serverUrl).description("Môi trường hiện tại")))
                .externalDocs(new ExternalDocumentation()
                        .description("Tài liệu kiến trúc hệ thống TechShop")
                        .url("https://docs.techshop.vn/architecture"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập Bearer JWT token dành cho CUSTOMER hoặc ADMIN (ví dụ: dev-customer-token, dev-admin-token)"))
                        .addSecuritySchemes("partnerKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Partner-Key")
                                .description("API Key header dành cho đối tác vận chuyển (mặc định: techshop-partner-shipping-secret-key-2026)")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .addSecurityItem(new SecurityRequirement().addList("partnerKey"));
    }

    @Bean
    public GroupedOpenApi publicV1Group() {
        return GroupedOpenApi.builder()
                .group("public-v1")
                .displayName("1. Public API v1 (Deprecated totalAmount)")
                .pathsToMatch("/api/v1/orders/**", "/api/orders/**")
                .build();
    }

    @Bean
    public GroupedOpenApi publicV2Group() {
        return GroupedOpenApi.builder()
                .group("public-v2")
                .displayName("2. Public API v2 (Chuẩn Money total)")
                .pathsToMatch("/api/orders/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminGroup() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("3. Admin API (Quản trị & Báo cáo)")
                .pathsToMatch("/api/admin/**", "/api/orders/*/transitions", "/api/orders/*/invoice")
                .build();
    }

    @Bean
    public GroupedOpenApi partnerGroup() {
        return GroupedOpenApi.builder()
                .group("partner")
                .displayName("4. Partner API (Webhook Vận chuyển)")
                .pathsToMatch("/api/webhooks/**")
                .build();
    }

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }
}
