package com.firstclub.membership.eligibility;

import java.util.Optional;

public interface MemberProfileRepository {

    MemberProfile save(MemberProfile profile);

    Optional<MemberProfile> findByUserId(String userId);
}
