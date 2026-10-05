package com.firstclub.membership.controller;

import com.firstclub.membership.config.Catalog;
import com.firstclub.membership.dto.ChangeTierRequest;
import com.firstclub.membership.dto.MembershipResponse;
import com.firstclub.membership.dto.SubscribeRequest;
import com.firstclub.membership.model.Membership;
import com.firstclub.membership.service.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequestMapping("/api/users/{userId}/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;
    private final Catalog catalog;
    private final Clock clock;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse subscribe(@PathVariable String userId, @Valid @RequestBody SubscribeRequest request) {
        return toResponse(membershipService.subscribe(userId, request.getPlanCode(), request.getTierCode()));
    }

    @GetMapping
    public MembershipResponse getMembership(@PathVariable String userId) {
        return toResponse(membershipService.getLatest(userId));
    }

    @PutMapping("/tier")
    public MembershipResponse changeTier(@PathVariable String userId, @Valid @RequestBody ChangeTierRequest request) {
        return toResponse(membershipService.changeTier(userId, request.getTierCode()));
    }

    @PostMapping("/cancel")
    public MembershipResponse cancel(@PathVariable String userId) {
        return toResponse(membershipService.cancel(userId));
    }

    @PostMapping("/tier-evaluation")
    public MembershipResponse evaluateTier(@PathVariable String userId) {
        return toResponse(membershipService.evaluateTier(userId));
    }

    private MembershipResponse toResponse(Membership membership) {
        return new MembershipResponse(
                membership,
                catalog.getPlan(membership.getPlanCode()),
                catalog.getTier(membership.getTierCode()),
                membership.statusAt(clock.instant()));
    }
}
