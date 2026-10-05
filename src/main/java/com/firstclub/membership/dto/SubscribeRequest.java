package com.firstclub.membership.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscribeRequest {

    @NotBlank
    private String planCode;

    @NotBlank
    private String tierCode;
}
