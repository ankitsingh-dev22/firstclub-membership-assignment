package com.firstclub.membership.controller;

import com.firstclub.membership.config.Catalog;
import com.firstclub.membership.dto.TierResponse;
import com.firstclub.membership.model.MemberProfile;
import com.firstclub.membership.model.MembershipPlan;
import com.firstclub.membership.model.MembershipTier;
import com.firstclub.membership.repository.MemberProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final Catalog catalog;
    private final MemberProfileRepository profileRepository;

    @GetMapping("/plans")
    public List<MembershipPlan> getPlans() {
        return catalog.getPlans();
    }

    @GetMapping("/tiers")
    public List<TierResponse> getTiers(@RequestParam(required = false) String userId) {
        if (userId == null) {
            return catalog.getTiers().stream()
                    .map(tier -> new TierResponse(tier, null))
                    .toList();
        }
        MemberProfile profile = profileRepository.findByUserId(userId).orElseGet(() -> MemberProfile.empty(userId));
        MembershipTier ceiling = catalog.highestEligibleTier(profile);
        return catalog.getTiers().stream()
                .map(tier -> new TierResponse(tier, tier.getRank() <= ceiling.getRank()))
                .toList();
    }
}
