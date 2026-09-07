package com.app.taskmanagement.requestdto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EmployeeRequestDto extends UserRequestDto {
    @NotBlank
    private String qualification;

    @NotBlank
    private String specialization;

    @NotBlank
    private String companyCode;
}
