package com.firstclub.membership.repository;

import com.firstclub.membership.exception.ErrorCode;
import com.firstclub.membership.exception.MembershipException;
import com.firstclub.membership.model.Membership;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the latest membership per user. Both writes go through {@link ConcurrentHashMap#compute},
 * which runs the check and the write atomically for that user's key. Throwing inside compute
 * leaves the stored value untouched.
 */
@Repository
public class InMemoryMembershipRepository implements MembershipRepository {

    private final ConcurrentHashMap<String, Membership> membershipsByUser = new ConcurrentHashMap<>();

    @Override
    public Optional<Membership> findLatestByUserId(String userId) {
        return Optional.ofNullable(membershipsByUser.get(userId));
    }

    @Override
    public Membership insertIfNoActive(Membership membership, Instant now) {
        return membershipsByUser.compute(membership.getUserId(), (userId, existing) -> {
            if (existing != null && existing.isActiveAt(now)) {
                throw new MembershipException(ErrorCode.ACTIVE_MEMBERSHIP_EXISTS,
                        "User " + userId + " already has an active membership");
            }
            return membership;
        });
    }

    @Override
    public Membership update(Membership updated, long expectedVersion) {
        return membershipsByUser.compute(updated.getUserId(), (userId, stored) -> {
            if (stored == null || !stored.getId().equals(updated.getId()) || stored.getVersion() != expectedVersion) {
                throw new MembershipException(ErrorCode.CONCURRENT_MODIFICATION,
                        "Membership was modified by another request, reload and retry");
            }
            return updated;
        });
    }
}
