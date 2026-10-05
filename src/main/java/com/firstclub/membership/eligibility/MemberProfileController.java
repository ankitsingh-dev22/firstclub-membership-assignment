package com.firstclub.membership.eligibility;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stands in for the order and CRM systems that would normally push these facts to us.
 */
@RestController
@RequestMapping("/api/users/{userId}/profile")
@RequiredArgsConstructor
public class MemberProfileController {

    private final MemberProfileRepository profileRepository;

    @PutMapping
    public MemberProfile updateProfile(@PathVariable String userId,
                                       @Valid @RequestBody MemberProfileRequest request) {
        MemberProfile profile = new MemberProfile(
                userId, request.getOrderCount(), request.getMonthlyOrderValue(), request.getCohorts());
        return profileRepository.save(profile);
    }

    @GetMapping
    public MemberProfile getProfile(@PathVariable String userId) {
        return profileRepository.findByUserId(userId).orElseGet(() -> MemberProfile.empty(userId));
    }
}
