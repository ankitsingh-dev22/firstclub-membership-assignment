package com.firstclub.membership.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST),

    ACTIVE_MEMBERSHIP_EXISTS(HttpStatus.CONFLICT),
    MEMBERSHIP_NOT_ACTIVE(HttpStatus.CONFLICT),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT);

    private final HttpStatus status;
}
