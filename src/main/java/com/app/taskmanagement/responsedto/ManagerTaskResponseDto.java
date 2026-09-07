package com.app.taskmanagement.responsedto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ManagerTaskResponseDto extends TaskResponseDto{
    private Integer totalCommitments;
}
