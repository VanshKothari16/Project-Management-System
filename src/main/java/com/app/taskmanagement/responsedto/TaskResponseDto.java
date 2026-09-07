package com.app.taskmanagement.responsedto;

import com.app.taskmanagement.enums.Priority;
import com.app.taskmanagement.enums.Status;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TaskResponseDto {
    private String id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private Priority priority;
    private Status status;
    private String projectName;
    private List<String> employee;
}
