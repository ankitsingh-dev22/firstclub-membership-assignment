package com.firstclub.membership.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs against the real application.yml, so it also proves the configured catalog binds and validates.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsConfiguredPlans() throws Exception {
        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", contains("MONTHLY", "QUARTERLY", "YEARLY")))
                .andExpect(jsonPath("$[1].duration").value("P3M"))
                .andExpect(jsonPath("$[1].price.amount").value(799))
                .andExpect(jsonPath("$[1].price.currency").value("INR"));
    }

    @Test
    void listsTiersWithBenefitsAndEligibilityForUser() throws Exception {
        mockMvc.perform(put("/api/users/catalog-gold/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderCount": 6, "monthlyOrderValue": 12000}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tiers").param("userId", "catalog-gold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", contains("SILVER", "GOLD", "PLATINUM")))
                .andExpect(jsonPath("$[*].eligible", contains(true, true, false)))
                .andExpect(jsonPath("$[1].eligibility").value("(orderCount >= 5 AND monthlyOrderValue >= 10000)"))
                .andExpect(jsonPath("$[1].benefits[1].type").value("EXTRA_DISCOUNT"))
                .andExpect(jsonPath("$[1].benefits[1].value").value(5));
    }
}
