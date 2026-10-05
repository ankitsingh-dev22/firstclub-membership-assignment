package com.firstclub.membership.membership;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeTierRequest {

    @NotBlank
    private String tierCode;
}
