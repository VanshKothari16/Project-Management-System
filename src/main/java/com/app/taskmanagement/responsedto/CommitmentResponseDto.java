package com.app.taskmanagement.responsedto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommitmentResponseDto {
    private String id;
    private String commitment;
    @JsonFormat(pattern = "dd/MMM/yyyy, hh:mm a")
    private LocalDateTime createdAt;
    private String employeeName;
    private String taskName;
    private String employeeEmail;
}
