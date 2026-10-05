package com.firstclub.membership.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST),

    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND),
    TIER_NOT_FOUND(HttpStatus.NOT_FOUND),
    MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND),

    ACTIVE_MEMBERSHIP_EXISTS(HttpStatus.CONFLICT),
    MEMBERSHIP_NOT_ACTIVE(HttpStatus.CONFLICT),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT),

    TIER_NOT_ELIGIBLE(HttpStatus.UNPROCESSABLE_ENTITY);

    private final HttpStatus status;
}
