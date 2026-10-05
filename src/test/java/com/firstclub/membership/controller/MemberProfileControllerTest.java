package com.firstclub.membership.controller;

import com.firstclub.membership.repository.InMemoryMemberProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberProfileController.class)
@Import(InMemoryMemberProfileRepository.class)
class MemberProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void storesAndReturnsProfile() throws Exception {
        mockMvc.perform(put("/api/users/u1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderCount": 6, "monthlyOrderValue": 15000, "cohorts": ["VIP"]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/u1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.orderCount").value(6))
                .andExpect(jsonPath("$.monthlyOrderValue").value(15000))
                .andExpect(jsonPath("$.cohorts", containsInAnyOrder("VIP")));
    }

    @Test
    void returnsEmptyProfileForUnknownUser() throws Exception {
        mockMvc.perform(get("/api/users/new-user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(0))
                .andExpect(jsonPath("$.monthlyOrderValue").value(0))
                .andExpect(jsonPath("$.cohorts").isEmpty());
    }

    @Test
    void rejectsNegativeValues() throws Exception {
        mockMvc.perform(put("/api/users/u1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderCount": -1, "monthlyOrderValue": -5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors.orderCount").exists())
                .andExpect(jsonPath("$.fieldErrors.monthlyOrderValue").exists());
    }
}
