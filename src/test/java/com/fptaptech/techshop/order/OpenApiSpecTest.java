package com.fptaptech.techshop.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OrderApplication.class)
@AutoConfigureMockMvc
class OpenApiSpecTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("Root OpenAPI 3.1 Spec metadata is valid")
    void specDescribesOpenApi31AndMetadata() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", startsWith("3.1")))
                .andExpect(jsonPath("$.info.title").value("TechShop Order Service API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"));
    }

    @Test
    @DisplayName("Group 1 Contract: public-v1 exposes v1 order endpoint and marks totalAmount as deprecated")
    void testPublicV1ContractSpec() throws Exception {
        mvc.perform(get("/v3/api-docs/public-v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", startsWith("3.1")))
                .andExpect(jsonPath("$.paths['/api/v1/orders/{code}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders/{code}'].get.deprecated").value(true))
                // Must NOT leak admin or partner internal routes
                .andExpect(jsonPath("$.paths['/api/admin/reports/daily-revenue']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/webhooks/shipping']").doesNotExist());
    }

    @Test
    @DisplayName("Group 2 Contract: public-v2 exposes modern customer order endpoints with Money schema")
    void testPublicV2ContractSpec() throws Exception {
        mvc.perform(get("/v3/api-docs/public-v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", startsWith("3.1")))
                .andExpect(jsonPath("$.paths['/api/orders']").exists())
                .andExpect(jsonPath("$.paths['/api/orders/{code}']").exists())
                .andExpect(jsonPath("$.components.schemas.MoneyResponse").exists())
                // Must NOT contain deprecated v1 or internal routes
                .andExpect(jsonPath("$.paths['/api/v1/orders/{code}']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/admin/reports/daily-revenue']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/webhooks/shipping']").doesNotExist());
    }

    @Test
    @DisplayName("Group 3 Contract: admin exposes daily-revenue reports and invoice management with bearerAuth")
    void testAdminContractSpec() throws Exception {
        mvc.perform(get("/v3/api-docs/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", startsWith("3.1")))
                .andExpect(jsonPath("$.paths['/api/admin/reports/daily-revenue']").exists())
                .andExpect(jsonPath("$.paths['/api/orders/{code}/invoice']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                // Must NOT contain partner webhook
                .andExpect(jsonPath("$.paths['/api/webhooks/shipping']").doesNotExist());
    }

    @Test
    @DisplayName("Group 4 Contract: partner exposes only carrier webhooks and requires X-Partner-Key")
    void testPartnerContractSpec() throws Exception {
        mvc.perform(get("/v3/api-docs/partner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", startsWith("3.1")))
                .andExpect(jsonPath("$.paths['/api/webhooks/shipping']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.partnerKey").exists())
                // Must NOT contain customer orders or admin reports
                .andExpect(jsonPath("$.paths['/api/orders']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/orders/{code}']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/admin/reports/daily-revenue']").doesNotExist());
    }
}

