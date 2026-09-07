package com.app.taskmanagement.responsedto;

import com.app.taskmanagement.entity.Employee;
import com.app.taskmanagement.enums.Status;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponseDto {
    private String id;
    private String name;
    private String description;
    private LocalDate deadline;
    private String manager;
    private Status status;
    private String companyName;

}
