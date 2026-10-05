package com.firstclub.membership.repository;

import com.firstclub.membership.model.MemberProfile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMemberProfileRepository implements MemberProfileRepository {

    private final Map<String, MemberProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public MemberProfile save(MemberProfile profile) {
        profiles.put(profile.getUserId(), profile);
        return profile;
    }

    @Override
    public Optional<MemberProfile> findByUserId(String userId) {
        return Optional.ofNullable(profiles.get(userId));
    }
}
