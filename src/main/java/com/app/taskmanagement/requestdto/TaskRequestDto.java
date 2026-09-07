package com.app.taskmanagement.requestdto;

import com.app.taskmanagement.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequestDto {
    @NotBlank
    private String title;
    @NotBlank
    private String description;
    @NotNull
    private LocalDate dueDate;
    @NotNull
    private Priority priority;
}
