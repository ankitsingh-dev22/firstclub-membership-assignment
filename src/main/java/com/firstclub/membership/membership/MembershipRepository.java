package com.firstclub.membership.membership;

import java.time.Instant;
import java.util.Optional;

public interface MembershipRepository {

    Optional<Membership> findLatestByUserId(String userId);

    /**
     * Stores a new membership unless the user already has one that is active at {@code now}.
     * The check and the write happen atomically.
     */
    Membership insertIfNoActive(Membership membership, Instant now);

    /**
     * Replaces the stored membership only if it is the same membership and still at {@code expectedVersion}.
     * Otherwise someone else changed it first and the caller must reload.
     */
    Membership update(Membership updated, long expectedVersion);
}
