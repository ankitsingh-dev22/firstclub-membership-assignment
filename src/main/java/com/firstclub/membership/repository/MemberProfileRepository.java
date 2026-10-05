package com.firstclub.membership.repository;

import com.firstclub.membership.model.MemberProfile;

import java.util.Optional;

public interface MemberProfileRepository {

    MemberProfile save(MemberProfile profile);

    Optional<MemberProfile> findByUserId(String userId);
}
