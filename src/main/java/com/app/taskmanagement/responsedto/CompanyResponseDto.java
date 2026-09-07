package com.app.taskmanagement.responsedto;

import com.app.taskmanagement.entity.User;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponseDto {
    private String id;
    private String name;
    private String occupation;
    private LocalDate createdAt;
    private String address;
    private UserResponseDto admin;
}
