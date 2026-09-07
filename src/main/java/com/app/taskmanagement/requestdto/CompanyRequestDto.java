package com.app.taskmanagement.requestdto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
public class CompanyRequestDto {
    @NotBlank
    private String companyName;
    @NotBlank
    private String occupation;
    @NotBlank
    private String address;
}
