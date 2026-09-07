package com.app.taskmanagement.requestdto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommitmentRequestDto {
    private String taskId;
    private String commitment;
}
