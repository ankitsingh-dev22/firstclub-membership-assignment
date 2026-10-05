package com.firstclub.membership.eligibility;

import com.firstclub.membership.model.MemberProfile;

public interface EligibilityRule {

    boolean isSatisfiedBy(MemberProfile profile);

    String describe();
}
