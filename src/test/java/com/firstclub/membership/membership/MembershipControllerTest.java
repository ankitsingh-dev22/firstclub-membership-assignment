package com.firstclub.membership.membership;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the membership API end to end against the catalog in application.yml.
 * Each test uses its own user id because the in-memory repositories are shared by the Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MembershipControllerTest {

    private static final Instant START = Instant.parse("2027-01-31T04:30:00Z"); // 31 Jan 10:00 IST

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Kolkata"));
        setTime(START);
    }

    @Test
    void subscribeCreatesActiveMembershipWithCalendarExpiry() throws Exception {
        subscribe("sub-ok", "MONTHLY", "SILVER")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.planCode").value("MONTHLY"))
                .andExpect(jsonPath("$.tierCode").value("SILVER"))
                .andExpect(jsonPath("$.pricePaid.amount").value(299))
                .andExpect(jsonPath("$.startedAt").value("2027-01-31T04:30:00Z"))
                .andExpect(jsonPath("$.expiresAt").value("2027-02-28T04:30:00Z"))
                .andExpect(jsonPath("$.benefits[0].type").value("FREE_DELIVERY"));
    }

    @Test
    void subscribeRejectsUnknownPlanAndTierAboveCeiling() throws Exception {
        subscribe("sub-bad", "WEEKLY", "SILVER")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));

        subscribe("sub-bad", "MONTHLY", "GOLD")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TIER_NOT_ELIGIBLE"));
    }

    @Test
    void subscribeRejectsSecondActiveMembership() throws Exception {
        subscribe("sub-twice", "MONTHLY", "SILVER").andExpect(status().isCreated());

        subscribe("sub-twice", "YEARLY", "SILVER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_MEMBERSHIP_EXISTS"));
    }

    @Test
    void tierChangeStaysWithinCeilingAndKeepsPlanPriceAndExpiry() throws Exception {
        updateProfile("tier-vip", """
                {"orderCount": 0, "monthlyOrderValue": 0, "cohorts": ["VIP"]}
                """);
        subscribe("tier-vip", "YEARLY", "SILVER").andExpect(status().isCreated());

        changeTier("tier-vip", "PLATINUM")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tierCode").value("PLATINUM"))
                .andExpect(jsonPath("$.planCode").value("YEARLY"))
                .andExpect(jsonPath("$.pricePaid.amount").value(2499))
                .andExpect(jsonPath("$.expiresAt").value("2028-01-31T04:30:00Z"))
                .andExpect(jsonPath("$.version").value(1));

        // A VIP does not meet Gold's own conditions, but Gold is below their Platinum ceiling.
        changeTier("tier-vip", "GOLD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tierCode").value("GOLD"))
                .andExpect(jsonPath("$.version").value(2));

        subscribe("tier-regular", "MONTHLY", "SILVER").andExpect(status().isCreated());
        changeTier("tier-regular", "GOLD")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TIER_NOT_ELIGIBLE"));
    }

    @Test
    void cancelEndsMembershipImmediately() throws Exception {
        subscribe("cancel", "MONTHLY", "SILVER").andExpect(status().isCreated());
        setTime(START.plusSeconds(3600));

        mockMvc.perform(post("/api/users/cancel/membership/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value("2027-01-31T05:30:00Z"))
                .andExpect(jsonPath("$.benefits", empty()));

        mockMvc.perform(post("/api/users/cancel/membership/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_NOT_ACTIVE"));
    }

    @Test
    void currentMembershipReportsExpiryAndAllowsResubscribing() throws Exception {
        mockMvc.perform(get("/api/users/current/membership"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_NOT_FOUND"));

        subscribe("current", "MONTHLY", "SILVER").andExpect(status().isCreated());
        setTime(Instant.parse("2027-02-28T04:30:00Z"));

        mockMvc.perform(get("/api/users/current/membership"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"))
                .andExpect(jsonPath("$.expiresAt").value("2027-02-28T04:30:00Z"))
                .andExpect(jsonPath("$.benefits", empty()));

        subscribe("current", "QUARTERLY", "SILVER")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.benefits", not(empty())));
    }

    @Test
    void tierEvaluationMovesUserToTheirCeiling() throws Exception {
        subscribe("eval", "MONTHLY", "SILVER").andExpect(status().isCreated());

        updateProfile("eval", """
                {"orderCount": 6, "monthlyOrderValue": 12000}
                """);
        evaluateTier("eval").andExpect(jsonPath("$.tierCode").value("GOLD"));

        updateProfile("eval", """
                {"orderCount": 1, "monthlyOrderValue": 500}
                """);
        evaluateTier("eval").andExpect(jsonPath("$.tierCode").value("SILVER"));
    }

    private void setTime(Instant instant) {
        when(clock.instant()).thenReturn(instant);
    }

    private ResultActions subscribe(String userId, String planCode, String tierCode) throws Exception {
        return mockMvc.perform(post("/api/users/{userId}/membership", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planCode\": \"%s\", \"tierCode\": \"%s\"}".formatted(planCode, tierCode)));
    }

    private ResultActions changeTier(String userId, String tierCode) throws Exception {
        return mockMvc.perform(put("/api/users/{userId}/membership/tier", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tierCode\": \"%s\"}".formatted(tierCode)));
    }

    private ResultActions evaluateTier(String userId) throws Exception {
        return mockMvc.perform(post("/api/users/{userId}/membership/tier-evaluation", userId))
                .andExpect(status().isOk());
    }

    private void updateProfile(String userId, String json) throws Exception {
        mockMvc.perform(put("/api/users/{userId}/profile", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
}
