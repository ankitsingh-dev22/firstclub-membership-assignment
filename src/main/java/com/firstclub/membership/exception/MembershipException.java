package com.firstclub.membership.exception;

import lombok.Getter;

@Getter
public class MembershipException extends RuntimeException {

    private final ErrorCode errorCode;

    public MembershipException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
