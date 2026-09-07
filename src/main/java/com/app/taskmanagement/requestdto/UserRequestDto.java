package com.app.taskmanagement.requestdto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.stereotype.Component;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Component
public class UserRequestDto {
    @NotBlank
    private String name;
    @Email
    private String email;
    @Size(max = 14,min = 8)
    private String password;
    @Size(min = 10,max = 10)
    private String phone;
}
