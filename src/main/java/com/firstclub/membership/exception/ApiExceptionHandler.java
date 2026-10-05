package com.firstclub.membership.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MembershipException.class)
    public ResponseEntity<ProblemDetail> handleMembershipException(MembershipException ex) {
        log.info("Request rejected: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        return problem(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ResponseEntity<ProblemDetail> response = problem(ErrorCode.INVALID_REQUEST, "Request validation failed");
        response.getBody().setProperty("fieldErrors", fieldErrors);
        return response;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return problem(ErrorCode.INVALID_REQUEST, "Request body is missing or malformed");
    }

    private ResponseEntity<ProblemDetail> problem(ErrorCode code, String message) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(code.getStatus(), message);
        body.setProperty("code", code.name());
        return ResponseEntity.status(code.getStatus()).body(body);
    }
}
